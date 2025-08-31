package com.mey.backend.domain.user.service;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.dto.UserInfoUpdateRequest;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.from(user);
    }

    @Transactional
    public void updateUserInfo(User currentUser, UserInfoUpdateRequest request) {

        User u = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

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

        // @Transactional 더티체킹으로 flush 되므로 save() 생략 가능
        // userRepository.save(u);
    }
}
