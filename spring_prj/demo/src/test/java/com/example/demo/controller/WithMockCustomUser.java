package com.example.demo.controller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
	String username() default "Administrator";
	String name()     default "admin@example.com";
	String password() default "password";
    String role()     default "ADMIN";
}