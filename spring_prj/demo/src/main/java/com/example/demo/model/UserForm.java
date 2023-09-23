package com.example.demo.model;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor( staticName = "of" )
@NoArgsConstructor( staticName = "empty" )
@Data
public class UserForm implements Serializable {
    @NotEmpty
    @Size(max = 128)
    private String name;
    @NotNull
    private GenderStatus gender;
    @NotEmpty
    @Size(max = 256)
    @Email
    private String email;
    @NotEmpty
    @Size(max = 16)
    private String password;
    @NotEmpty
    @Size(max = 16)
    private String passwordConfirmation;
    private String[] genre;

    @AssertTrue(message = "{invalid.userForm.password}")
    public boolean isPasswordValid() {
        if (!StringUtils.hasText(password) && !StringUtils.hasText(passwordConfirmation)) {
            return true;
        }
        return password.equals(passwordConfirmation);
    }

    @JsonIgnore
    public GenderStatus[] getGenderStatusList() {
        return GenderStatus.values();
    }

    @JsonIgnore
    public String getGenreString() {
        return String.join("|", genre);
    }

    public void setSplitGenre(Optional<String> genreString) {
        genre = genreString.map(v -> v.split("\\|")).orElse(new String[0]);
    }
}
