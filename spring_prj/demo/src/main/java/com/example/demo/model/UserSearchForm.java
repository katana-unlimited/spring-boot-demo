package com.example.demo.model;

import java.io.Serializable;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSearchForm implements Serializable {
    @Size(max = 10, message = "IDは{max}桁以内で入力してください")
    @Pattern(regexp = "\\d*", message = "数値を入力してください")
    private String id;
    @Size(max = 128, message = "名前は{max}桁以内で入力してください")
    private String name;
    @Size(max = 256, message = "メールアドレスは{max}桁以内で入力してください")
    private String email;
    private String sortBy = "+id";
    private String[] roles;
}
