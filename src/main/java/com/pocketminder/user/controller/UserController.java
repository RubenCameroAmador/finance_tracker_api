package com.pocketminder.user.controller;

import com.pocketminder.auth.entity.User;
import com.pocketminder.user.dto.UpdateUserDTO;
import com.pocketminder.user.dto.UserResponseDTO;
import com.pocketminder.user.mapper.UserMapper;
import com.pocketminder.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public UserResponseDTO me(){
        User user = userService.getCurrentUser();

        return userMapper.toResponse(user);
    }

    @PutMapping("/me")
    public UserResponseDTO updateProfile(
            @Valid @RequestBody UpdateUserDTO request
            ){
        User updatedUser = userService.updateProfile(request);
        return userMapper.toResponse(updatedUser);
    }


}
