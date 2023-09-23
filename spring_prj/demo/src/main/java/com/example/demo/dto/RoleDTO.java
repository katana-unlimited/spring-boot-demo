package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoleDTO {
    private Integer id;
    private String name;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
