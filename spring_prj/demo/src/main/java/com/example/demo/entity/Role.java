package com.example.demo.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = Role.SCHEMA_NAME, name = Role.TABLE_NAME)
// @EqualsAndHashCode(callSuper=true)
@ToString(callSuper = true)
public class Role extends AbstractEntity {
    public static final String SCHEMA_NAME = "sample_schema";
    public static final String TABLE_NAME  = "roles";
    @Id
    @Column(nullable = false, unique = true)
    private Integer id;

    @Column(nullable = false)
    private String name;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) &&
                Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name);
    }
}
