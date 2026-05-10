package com.seap.smartfinancetracker.security.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application security configuration class.
 * <p>
 * This configuration defines security-related beans used throughout the
 * application, including the password encoder and DAO-based authentication
 * provider.
 */
@Configuration
@AllArgsConstructor
public class ApplicationConfig {

    private final UserDetailsService userDetailsService;

    /**
     * Creates and configures the {@link PasswordEncoder} bean used for hashing and
     * verifying user password throughout the application.
     * <p>
     * This implementation uses {@link BCryptPasswordEncoder}, which
     * applies the BCrypt hashing algorithm with built-in salting for
     * secure password storage
     * </p>
     *
     * @return the {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates the DAO authentication provider used for username/password
     * authentication.
     * <p>
     * The provider delegates user retrieval to the configured
     * {@link UserDetailsService} and uses the configured
     * {@link PasswordEncoder} for password validation.
     *
     * @return the configured {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
