package com.example.demo.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {

    @Bean
    public AuditorAware<String> createAuditorProvider() {
        return new SecurityAuditor();
    }

    @Bean
    public AuditingEntityListener createAuditingListener() {
        return new AuditingEntityListener();
    }

    // Spring data JPAでcreatedBy updatedByカラムにログインユーザを自動設定する為の設定
    public static class SecurityAuditor implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication =
                   SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated())
            {
                return null;
            }
            return Optional.ofNullable(authentication.getName());
        }
    }
    
}
