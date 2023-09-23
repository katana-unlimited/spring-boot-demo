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
public class UserDeleteValidator implements Validator {

    @Autowired
	private PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(Class<?> clazz) {
        return UserDeleteForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "email.required");
        if (errors.hasErrors()) {
            return;
        }
        UserDeleteForm form = (UserDeleteForm) target;
        LoginUserDetails userDetails = (LoginUserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        if (!userDetails.isAdmin()) {
            // 入力した現在のパスワードが正しいかチェック（管理者はOK）
            if (StringUtils.hasText(form.getPasswordConfirmation())) {
                if (!passwordEncoder.matches(form.getPasswordConfirmation(), userDetails.getPassword()))
                    errors.rejectValue("passwordConfirmation", "invalid");
            } else
                errors.rejectValue("passwordConfirmation", "empty");
        }
    }
    
}
