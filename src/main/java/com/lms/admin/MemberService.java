package com.lms.admin;

import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.guardian.GuardianLinkRepository;
import com.lms.security.Roles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 학원 관리자(ADMIN)의 회원/강사 관리. app_user를 현재 테넌트 범위에서 CRUD한다(RLS 격리).
 * 컨트롤러에서 ADMIN 권한으로 제한한다.
 */
@Service
@Transactional
public class MemberService {

    private static final Set<String> ASSIGNABLE = Set.of(Roles.STUDENT, Roles.INSTRUCTOR, Roles.ADMIN, Roles.PARENT);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuardianLinkRepository guardianLinkRepository;

    public MemberService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                         GuardianLinkRepository guardianLinkRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.guardianLinkRepository = guardianLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<AppUser> list() {
        return userRepository.findAll();
    }

    public AppUser create(String rawEmail, String password, String displayName, List<String> roles) {
        String email = rawEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("이미 가입된 이메일입니다");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(password),
                blankToNull(displayName), normalizeRoles(roles));
        return userRepository.save(user);
    }

    public AppUser updateRoles(UUID userId, List<String> roles) {
        AppUser user = require(userId);
        user.changeRoles(normalizeRoles(roles));
        return user;
    }

    public void resetPassword(UUID userId, String newPassword) {
        require(userId).changePassword(passwordEncoder.encode(newPassword));
    }

    public void delete(UUID userId) {
        AppUser user = require(userId);
        // 이 사람이 부모든 자녀든 걸려 있는 학부모-자녀 연결을 함께 제거(고아 연결 방지).
        guardianLinkRepository.deleteByParentSubjectOrStudentSubject(user.getEmail(), user.getEmail());
        userRepository.delete(user);
    }

    private AppUser require(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다: " + userId));
    }

    private String normalizeRoles(List<String> roles) {
        List<String> upper = roles.stream().map(r -> r.trim().toUpperCase()).distinct().toList();
        for (String r : upper) {
            if (!ASSIGNABLE.contains(r)) {
                throw new BadRequestException("허용되지 않은 역할입니다: " + r);
            }
        }
        return String.join(",", upper);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
