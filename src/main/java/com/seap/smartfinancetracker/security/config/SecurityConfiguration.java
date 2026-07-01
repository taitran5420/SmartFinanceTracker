package com.seap.smartfinancetracker.security.config;

import com.seap.smartfinancetracker.security.filter.JwtAuthenticationFilter;
import com.seap.smartfinancetracker.security.provider.JwtAuthenticationProvider;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures Spring Security for the application.
 *
 * <p>This configuration:
 * <ul>
 *     <li>Sets up JWT-based authentication</li>
 *     <li>Registers authentication providers</li>
 *     <li>Defines stateless session management</li>
 *     <li>Configures secured and public endpoints</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final DaoAuthenticationProvider daoAuthenticationProvider;

    /**
     * Configures the application's security filter chain.
     *
     * @param http the {@link HttpSecurity} configuration object
     * @param authenticationManager the authentication manager used by JWT filter
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager);

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/openapi.json", "/v3/api-docs/**", "/docs", "/docs/**", "/scalar/**", "/error").permitAll()
                        // GraphiQL is the in-browser explorer UI (static assets). The /graphql
                        // query endpoint itself stays authenticated via anyRequest() below, so
                        // GraphiQL users must supply a JWT in its Headers panel.
                        .requestMatchers("/graphiql", "/graphiql/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Defines the CORS policy applied to every endpoint.
     *
     * <p>Allowed origins are driven by the {@code cors.allowed-origins} property (comma-separated),
     * so they can vary per environment without a code change. Credentials are allowed, so the wildcard
     * origin ({@code *}) must not be used — list explicit origins instead.</p>
     *
     * @param allowedOrigins the origins permitted to call the API, from {@code cors.allowed-origins}
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Creates the application's authentication manager with
     * both DAO and JWT authentication providers.
     *
     * @return the configured {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(
                daoAuthenticationProvider,
                jwtAuthenticationProvider
        ));
    }
}
