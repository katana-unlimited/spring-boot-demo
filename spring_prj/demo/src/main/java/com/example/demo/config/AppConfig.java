package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.Optional;

import org.modelmapper.Condition;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.example.demo.dto.LoginUserDTO;
import com.example.demo.dto.RoleDTO;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Role;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserUpdateForm;
import com.example.demo.utils.DateTimeUtils;

@Configuration
public class AppConfig {
	private PasswordEncoder passwordEncoder;

    public AppConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        //マッチング戦略を厳しいものに設定
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        //型の完全マッチなど
		modelMapper.getConfiguration().setFullTypeMatchingRequired(true);
        //nullをスキップする設定
		modelMapper.getConfiguration().setSkipNullEnabled(false);
        // コンバーター
        Converter<Optional<String>, String> optToString = ctx -> ctx.getSource() == null ? null
                : ctx.getSource().orElse(null);
        Converter<LocalDateTime, String> toDateTimeString = ctx -> ctx.getSource() == null ? null
                : DateTimeUtils.formatDateTime(ctx.getSource());
        Converter<String, String> encodePassword = ctx -> ctx.getSource() == null ? null
                : passwordEncoder.encode(ctx.getSource());
        // コンディション
        Condition<String, String> hasText = ctx -> StringUtils.hasText(ctx.getSource());
        // マッピング
        modelMapper.createTypeMap(LoginUser.class, LoginUserDTO.class)
                .addMappings(mapper -> {
                    mapper.using(optToString).map(LoginUser::getGenre, LoginUserDTO::setGenre);
                    mapper.using(toDateTimeString).map(LoginUser::getCreatedAt, LoginUserDTO::setCreatedAt);
                    mapper.using(toDateTimeString).map(LoginUser::getUpdatedAt, LoginUserDTO::setUpdatedAt);
                });
        modelMapper.createTypeMap(Role.class, RoleDTO.class)
                .addMappings(mapper -> {
                    mapper.using(toDateTimeString).map(Role::getCreatedAt, RoleDTO::setCreatedAt);
                    mapper.using(toDateTimeString).map(Role::getUpdatedAt, RoleDTO::setUpdatedAt);
                });
        modelMapper.createTypeMap(LoginUser.class, UserForm.class)
                .addMappings(mapper -> {
                    mapper.skip(UserForm::setPassword);
                    mapper.skip(UserForm::setGenre);
                    mapper.map(LoginUser::getGenre, UserForm::setSplitGenre);
                });
        modelMapper.createTypeMap(LoginUser.class, UserUpdateForm.class)
                .addMappings(mapper -> {
                    mapper.skip(UserUpdateForm::setPassword);
                    mapper.skip(UserUpdateForm::setGenre);
                    mapper.map(LoginUser::getGenre, UserUpdateForm::setSplitGenre);
                });
        modelMapper.createTypeMap(UserForm.class, LoginUser.class)
                .addMappings(mapper -> {
                    mapper.using(encodePassword).map(UserForm::getPassword, LoginUser::setPassword);
                    mapper.map(UserForm::getGenreString, LoginUser::setGenre);
                });
        modelMapper.createTypeMap(UserUpdateForm.class, LoginUser.class)
                .addMappings(mapper -> {
                    mapper.when(hasText).using(encodePassword).map(UserUpdateForm::getNewPassword, LoginUser::setPassword);
                    mapper.map(UserUpdateForm::getGenreString, LoginUser::setGenre);
                });
        return modelMapper;
    }
}