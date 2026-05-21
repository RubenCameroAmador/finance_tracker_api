package com.pocketminder.auth.service;

import com.pocketminder.auth.dto.AuthResponseDTO;
import com.pocketminder.auth.dto.LoginRequestDTO;
import com.pocketminder.auth.dto.RegisterRequestDTO;
import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import com.pocketminder.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register (RegisterRequestDTO request){
        boolean exists = userRepository.findByEmail( request.getEmail() ).isPresent();
        if ( exists ){
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
         User user = userRepository
                 .findByEmail(request.getEmail())
                 .orElseThrow(()-> new RuntimeException("Invalid credentials"));

         boolean matches = passwordEncoder.matches(
                 request.getPassword(),
                 user.getPassword()
         );

         if( !matches ){
             throw new RuntimeException("Invalid credentials");
         }

         String token = jwtService.generateToken(user.getEmail());

         return AuthResponseDTO.builder()
                 .token(token)
                 .build();

    }
}
