package com.example.demo.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AppExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFoundException(UserNotFoundException ex, Model model) {
        model.addAttribute("userId", ex.getUserId());
        return "user/not_found";
    }
}
