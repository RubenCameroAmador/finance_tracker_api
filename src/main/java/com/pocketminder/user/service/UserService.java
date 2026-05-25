package com.pocketminder.user.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import com.pocketminder.user.dto.UpdateUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getCurrentUser(){
        String email = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow();
    }

    public User updateProfile(
            UpdateUserDTO request
    ){
        User user = getCurrentUser();
        user.setName(request.getName());
        return userRepository.save(user);
    }
}
