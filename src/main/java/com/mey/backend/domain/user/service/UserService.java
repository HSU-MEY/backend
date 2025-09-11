package com.mey.backend.domain.user.service;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.dto.UserInfoUpdateRequest;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.repository.UserRepository;
import com.mey.backend.global.util.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Manager s3Manager;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.from(user);
    }

    @Transactional
    public void updateUserInfo(User currentUser, UserInfoUpdateRequest request, MultipartFile profileImage) {

        User u = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // 이름 변경
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            u.setNickname(request.getNickname());
        }

        // 이메일 변경
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(u.getEmail())) {

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalStateException("이미 사용 중인 이메일입니다.");
            }
            u.setEmail(request.getEmail());
        }

        // 비밀번호 변경, 인코딩 필수!!
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // 프로필 이미지 변경
        if (profileImage != null && !profileImage.isEmpty()) {
            changeUserProfileImage(u, profileImage);
        }
    }

    private void changeUserProfileImage(User user, MultipartFile profileImage) {
        if (profileImage.getOriginalFilename() == null || profileImage.getOriginalFilename().isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 유효하지 않습니다.");
        }
        
        String keyName = s3Manager.generateProfileKeyName(profileImage.getOriginalFilename());
        String profileImageUrl = s3Manager.uploadFile(keyName, profileImage);
        
        user.setProfileImageUrl(profileImageUrl);
    }
}
