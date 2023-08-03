package com.example.demo.tools;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Role;
import com.example.demo.model.GenderStatus;
import com.example.demo.model.UserDeleteForm;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserUpdateForm;

public class TestSupport {

    static public LoginUser getAdminUser() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1, "ROLE_GENERAL"));
        roles.add(new Role(2, "ROLE_ADMIN"));
        LoginUser admin = LoginUser.builder()
                .id(2)
                .name("管理太郎")
                .email("admin@example.com")
                .gender(GenderStatus.MALE)
                .genre("NEWS|SPORTS")
                .roleList(roles)
                .build();
        return admin;
    }

    static public LoginUser getGeneralUser() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1, "ROLE_GENERAL"));
        // @SuppressWarnings("unused")
        LoginUser general = LoginUser.builder()
                .id(1)
                .name("一般太郎")
                .email("general@example.com")
                .gender(GenderStatus.MALE)
                .genre("NEWS")
                .roleList(roles)
                .build();
        return general;
    }

    static public UserForm getUserForm() {
        return UserForm.builder()
            .name("name")
            .gender(GenderStatus.FEMALE)
            .email("test@email.com")
            .password("password")
            .passwordConfirmation("password")
            .genre(new String[]{ "NEWS" })
            .build();
    }

    static public UserUpdateForm getUserUpdateForm() {
        return UserUpdateForm.builder()
            .name("name")
            .gender(GenderStatus.FEMALE)
            .email("test@email.com")
            .password("password")
            .newPassword("newPassword")
            .passwordConfirmation("newPassword")
            .genre(new String[]{ "NEWS", "SPORTS" })
            .build();
    }

    static public UserDeleteForm getUserDeleteForm() {
        return UserDeleteForm.builder()
            .name("name")
            .email("test@email.com")
            .passwordConfirmation("password")
            .build();
    }
    
}
