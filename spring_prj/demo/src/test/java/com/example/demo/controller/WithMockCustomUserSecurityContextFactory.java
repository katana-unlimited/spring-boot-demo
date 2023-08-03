package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Role;
import com.example.demo.model.GenderStatus;
import com.example.demo.service.LoginUserDetails;

public class WithMockCustomUserSecurityContextFactory
	implements WithSecurityContextFactory<WithMockCustomUser> {

    private final PasswordEncoder passwordEncoder;

    public WithMockCustomUserSecurityContextFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
	public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1, "ROLE_"+customUser.role()));
        LoginUser loginUser = LoginUser.builder()
            .email(customUser.name())
            .password(passwordEncoder.encode(customUser.password()))
            .name(customUser.username())
            .gender(GenderStatus.OTHER)
            .roleList(roles)
            .build();
		LoginUserDetails principal =
			new LoginUserDetails(loginUser);
		Authentication auth =
			UsernamePasswordAuthenticationToken.authenticated(principal, principal.getPassword(), principal.getAuthorities());
		context.setAuthentication(auth);
		return context;
	}
}
