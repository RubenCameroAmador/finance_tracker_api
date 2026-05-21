package com.pocketminder.auth.controller;

import com.pocketminder.auth.dto.AuthResponseDTO;
import com.pocketminder.auth.dto.LoginRequestDTO;
import com.pocketminder.auth.dto.RegisterRequestDTO;
import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public User register(
            @Valid @RequestBody RegisterRequestDTO request
    ){
        return authService.register(request);
    }

    @GetMapping("/login")
    public AuthResponseDTO login(
            @Valid @RequestBody LoginRequestDTO request
    ){
        return authService.login(request);
    }

    @GetMapping("/me")
    public String me(){
        return "Authenticated!";
    }
}
