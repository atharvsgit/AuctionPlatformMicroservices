package com.example.user_service.service;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.model.User;
import com.example.user_service.repo.UserRepo;
import com.example.user_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public UserResponse registerUser(RegisterRequest req) {
        if(req.getPassword()==null || req.getPassword().length()<8){
            throw new IllegalArgumentException("Password length must be greater than 8 chars");
        }

        if(userRepo.findByEmail(req.getEmail()).isPresent()){
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole().toUpperCase());

        User saved =  userRepo.save(user);

        //publish registered event to kafka
        
        return new UserResponse(saved.getId(), saved.getEmail(),saved.getRole());
    }

    public LoginResponse loginUser(LoginRequest req){
        User u = userRepo.findByEmail(req.getEmail()).orElseThrow(()->new IllegalArgumentException("Invalid " +
                "email/pass"));
        if(!passwordEncoder.matches(req.getPassword(), u.getPassword())){
            throw new IllegalArgumentException("Invalid email/pass");
        }

        String t = jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole());
        return new LoginResponse(t);
    }
}
