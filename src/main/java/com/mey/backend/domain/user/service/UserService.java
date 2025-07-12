package com.mey.backend.domain.user.service;

import com.mey.backend.domain.user.dto.UserInfoResponse;
import com.mey.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.from(user);
    }
}