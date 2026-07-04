package com.lms.upload;

import com.lms.error.BadRequestException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * 동영상 업로드 (강사/관리자). 파일을 로컬 저장소에 저장하고 재생용 URL(/media/{파일명})을 돌려준다.
 * 반환된 URL을 레슨의 videoUrl로 저장하면 학습창에서 그대로 재생된다.
 */
@RestController
public class UploadController {

    private final Path uploadDir;

    public UploadController(Path uploadDir) {
        this.uploadDir = uploadDir;
    }

    private static final String VIDEO_EXT = "\\.(mp4|webm|ogg|mov|m4v)";
    // 과제/자료용 문서·이미지 화이트리스트
    private static final String FILE_EXT = "\\.(pdf|png|jpg|jpeg|gif|webp|hwp|doc|docx|ppt|pptx|xls|xlsx|txt|zip)";

    @PostMapping("/api/uploads/video")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Map<String, String> uploadVideo(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("동영상 파일만 업로드할 수 있습니다");
        }
        return store(file, VIDEO_EXT);
    }

    /** 범용 파일 업로드 (과제 첨부·자료실). 안전한 확장자만 허용. 조회는 /media/**로 제공. */
    @PostMapping("/api/uploads/file")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) {
        return store(file, FILE_EXT);
    }

    private Map<String, String> store(MultipartFile file, String allowedExtRegex) {
        if (file.isEmpty()) {
            throw new BadRequestException("빈 파일입니다");
        }
        String ext = extensionOf(file.getOriginalFilename(), allowedExtRegex);
        if (ext.isEmpty()) {
            throw new BadRequestException("허용되지 않는 파일 형식입니다");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {   // 경로 탈출 방지
            throw new BadRequestException("잘못된 파일명입니다");
        }
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("업로드에 실패했습니다: " + e.getMessage());
        }
        return Map.of("url", "/media/" + filename, "filename", filename);
    }

    private String extensionOf(String original, String allowedExtRegex) {
        if (original == null) {
            return "";
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        String ext = original.substring(dot).toLowerCase();
        return ext.matches(allowedExtRegex) ? ext : "";
    }
}
