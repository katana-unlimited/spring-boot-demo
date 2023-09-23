package com.example.demo.model;

import java.io.Serializable;
import java.util.Optional;

import org.springframework.util.StringUtils;

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
public class UserUpdateForm implements Serializable {
    @NotEmpty
    @Size(max = 128)
    private String name;
    @NotNull
    private GenderStatus gender;
    @NotEmpty
    @Size(max = 256)
    @Email
    private String email;
    @Size(max = 16)
    private String password;
    @Size(max = 16)
    private String newPassword;
    @Size(max = 16)
    private String passwordConfirmation;
    private String[] genre;

    @AssertTrue(message = "{invalid.userUpdateForm.newPassword}")
    public boolean isNewPasswordValid() {
        if (!StringUtils.hasText(newPassword) && !StringUtils.hasText(passwordConfirmation)) {
            return true;
        }
        return newPassword.equals(passwordConfirmation);
    }

    public GenderStatus[] getGenderStatusList() {
        return GenderStatus.values();
    }

    public String getGenreString() {
        return String.join("|", genre);
    }

    public void setSplitGenre(Optional<String> genreString) {
        genre = genreString.map(v -> v.split("\\|")).orElse(new String[0]);
    }

}
