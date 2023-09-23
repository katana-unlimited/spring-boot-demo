package com.example.demo.model;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor( staticName = "of" )
@NoArgsConstructor( staticName = "empty" )
@Data
public class UserDeleteForm implements Serializable {
    private String name;
    @NotEmpty
    @Size(max = 256)
    @Email
    private String email;
    @Size(max = 16)
    private String passwordConfirmation;
}
