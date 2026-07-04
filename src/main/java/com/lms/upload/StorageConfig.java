package com.lms.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드된 동영상의 로컬 저장 위치 + 정적 제공 설정.
 *
 * <p>{@code /media/**} 는 Spring의 ResourceHttpRequestHandler가 처리하며, HTTP Range 요청을
 * 기본 지원한다 → 학습창에서 영상 탐색(seek)·이어듣기가 가능하다. 파일명이 UUID라
 * 추측이 어렵고, 업로드(쓰기)는 인증·권한으로 보호한다(읽기 URL만 공개).
 */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

    private final Path uploadDir;

    public StorageConfig(@Value("${app.upload.dir:uploads}") String dir) {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    @Bean
    public Path uploadDir() {
        try {
            Files.createDirectories(uploadDir);
        } catch (Exception e) {
            throw new IllegalStateException("업로드 디렉터리를 만들 수 없습니다: " + uploadDir, e);
        }
        return uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";   // addResourceLocations는 디렉터리 위치에 끝 슬래시가 필요
        }
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
