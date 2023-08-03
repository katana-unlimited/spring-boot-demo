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
    @NotEmpty(message = "名前を入力してください")
    @Size(max = 128, message = "名前は{max}桁以内で入力してください")
    private String name;
    @NotNull(message = "性別を選択してください")
    private GenderStatus gender;
    @NotEmpty(message = "メールアドレスを入力してください")
    @Size(max = 256, message = "メールアドレスは{max}桁以内で入力してください")
    @Email(message = "メールアドレスの形式で入力してください")
    private String email;
    @Size(max = 16, message = "現在のパスワードは{max}桁以内で入力してください")
    private String password;
    @Size(max = 16, message = "新パスワードは{max}桁以内で入力してください")
    private String newPassword;
    @Size(max = 16, message = "新パスワード（確認用）は{max}桁以内で入力してください")
    private String passwordConfirmation;
    private String[] genre;

    @AssertTrue(message = "新パスワードと新パスワード（確認用）は同一にしてください")
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
