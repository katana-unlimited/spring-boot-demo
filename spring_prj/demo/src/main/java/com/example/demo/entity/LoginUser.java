package com.example.demo.entity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.demo.model.GenderStatus;
import com.querydsl.core.annotations.QueryEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Data
@Table(schema = LoginUser.SCHEMA_NAME, name = LoginUser.TABLE_NAME)
// @EqualsAndHashCode(callSuper=true)
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@QueryEntity
public class LoginUser extends AbstractEntity {
    public static final String SCHEMA_NAME = "sample_schema";
    public static final String TABLE_NAME  = "login_user";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GenderStatus gender;

    @Column(nullable = true)
    private String genre;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roleList;

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Optional<String> getGenre() {
        return Optional.ofNullable(genre);
    }

    private boolean areRolesEqual(List<Role> list1, List<Role> list2) {
        if (list1 == null && list2 == null) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LoginUser loginUser = (LoginUser) o;
        return Objects.equals(id, loginUser.id) &&
                Objects.equals(name, loginUser.name) &&
                Objects.equals(email, loginUser.email) &&
                Objects.equals(password, loginUser.password) &&
                Objects.equals(gender, loginUser.gender) &&
                Objects.equals(genre, loginUser.genre) &&
                areRolesEqual(roleList, loginUser.roleList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name, email, password, gender, genre, roleList);
    }
}
