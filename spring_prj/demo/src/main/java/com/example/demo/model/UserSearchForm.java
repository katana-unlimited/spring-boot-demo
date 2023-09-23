package com.example.demo.model;

import java.io.Serializable;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSearchForm implements Serializable {
    @Size(max = 10)
    @Pattern(regexp = "\\d*")
    private String id;
    @Size(max = 128)
    private String name;
    @Size(max = 256)
    private String email;
    private String sortBy = "+id";
    private String[] roles;
}
