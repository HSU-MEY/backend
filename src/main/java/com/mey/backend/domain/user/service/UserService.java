package com.mey.backend.domain.user.service;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.dto.UserInfoUpdateRequest;
import com.mey.backend.domain.user.entity.User;
import com.mey.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.from(user);
    }

    @Transactional
    public void updateUserInfo(User user, UserInfoUpdateRequest request) {

        User optionalUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

//        if (request.getUsername() != null) {
//            optionalUser.setUsername(request.getUsername());
//        }

        if (request.getPassword() != null) {
            optionalUser.setPassword(request.getPassword());
        }

        if (request.getEmail() != null) {
            optionalUser.setEmail(request.getEmail());
        }

        userRepository.save(optionalUser);
    }
}