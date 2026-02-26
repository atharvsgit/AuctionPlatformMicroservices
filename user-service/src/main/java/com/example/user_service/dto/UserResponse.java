package com.example.user_service.dto;

public class UserResponse {
    private Long id;
    private String email;
    private String role;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }
}
