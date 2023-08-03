package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Role;
import com.example.demo.model.GenderStatus;



@SpringBootTest
@Transactional
@WithMockUser("mock_user")
public class JdbcLoginUserRepositoryTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private JdbcLoginUserRepository jdbcLoginUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    public void testCRUD() {
        Optional<Role> optRole = roleRepository.findOptionalByName("ROLE_GENERAL");
        List<Role> roleList = new ArrayList<>();
        roleList.add(optRole.get());

        // INSERT
        LoginUser loginUser = LoginUser.builder()
                .name("name")
                .email("test@email.com")
                .password(passwordEncoder.encode("password"))
                .gender(GenderStatus.OTHER)
                .genre("NEWS|SPORTS")
                .roleList(roleList)
                .build();
        LoginUser loginUser2 = jdbcLoginUserRepository.save(loginUser);
        assertEquals(loginUser.getName(), loginUser2.getName());
        assertEquals(loginUser.getEmail(), loginUser2.getEmail());
        assertEquals(loginUser.getPassword(), loginUser2.getPassword());
        assertEquals(loginUser.getGender(), loginUser2.getGender());
        assertEquals(loginUser.getGenre(), loginUser2.getGenre());
        assertEquals(loginUser.getRoleList(), loginUser2.getRoleList());

        // UPDATE
        loginUser2.setName("updateName");
        loginUser2.setPassword(passwordEncoder.encode("updatePassword"));
        loginUser2.setGender(GenderStatus.FEMALE);
        loginUser2.setGenre("FINANCE");
        LoginUser loginUser3 = jdbcLoginUserRepository.save(loginUser2);
        assertEquals(loginUser2.getName(), loginUser3.getName());
        assertEquals(loginUser2.getEmail(), loginUser3.getEmail());
        assertEquals(loginUser2.getPassword(), loginUser3.getPassword());
        assertEquals(loginUser2.getGender(), loginUser3.getGender());
        assertEquals(loginUser2.getGenre(), loginUser3.getGenre());
        assertEquals(loginUser2.getRoleList(), loginUser3.getRoleList());

        // SELECT
        Optional<LoginUser> optLoginUser = jdbcLoginUserRepository.findByEmail(loginUser.getEmail());
        assertThat(optLoginUser).isPresent();
        LoginUser loginUser4 = optLoginUser.get();
        logger.debug("jdbcLoginUserRepository.findByEmail="+loginUser4);

        // DELETE
        jdbcLoginUserRepository.delete(loginUser.getEmail());
        optLoginUser = jdbcLoginUserRepository.findByEmail(loginUser.getEmail());
        assertThat(optLoginUser).isNotPresent();
    }
}
