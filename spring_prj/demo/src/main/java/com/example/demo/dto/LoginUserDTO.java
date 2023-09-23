package com.example.demo.dto;

import java.util.List;

import com.example.demo.model.GenderStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginUserDTO {
    private Integer id;
    private String name;
    private String email;
    private String password;
    private GenderStatus gender;
    private String genre;
    private List<RoleDTO> roleList;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}