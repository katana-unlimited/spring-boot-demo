package com.example.demo.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.example.demo.service.LoginUserDetails;

@Component
public class UserUpdateValidator implements Validator {

    @Autowired
	private PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(Class<?> clazz) {
        return UserUpdateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "email.required");
        if (errors.hasErrors()) {
            return;
        }
        UserUpdateForm form = (UserUpdateForm) target;
        LoginUserDetails userDetails = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        if (!userDetails.isAdmin()) {
            // 入力した現在のパスワードが正しいかチェック（管理者はOK）
            if (StringUtils.hasText(form.getPassword()) && 
                !passwordEncoder.matches(form.getPassword(), userDetails.getPassword())) {
                errors.rejectValue("password", "invalid");
            }
            // 新しいパスワードを入力した時は現在のパスワードも入力しているかチェック（管理者はOK）
            if (StringUtils.hasText(form.getNewPassword()) && !StringUtils.hasText(form.getPassword())) {
                errors.rejectValue("password", "empty");
            }
        }
    }
    
}
