package com.example.demo.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends Throwable{
    private String userId;
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
}
