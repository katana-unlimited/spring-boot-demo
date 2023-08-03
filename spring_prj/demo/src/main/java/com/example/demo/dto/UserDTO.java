package com.example.demo.dto;

import java.util.List;

import com.example.demo.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Integer id;
    private String  name;
    private List<Role> roleList;
}
