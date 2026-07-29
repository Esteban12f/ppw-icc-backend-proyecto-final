package ec.edu.ups.icc.academicevents.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
public class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity http,
            @Value("${swagger.username}") String username,
            @Value("${swagger.password}") String rawPassword
    ) throws Exception {

        PasswordEncoder passwordEncoder =
                PasswordEncoderFactories
                        .createDelegatingPasswordEncoder();

        UserDetails swaggerUser =
                User.withUsername(username)
                        .password(
                                passwordEncoder.encode(rawPassword)
                        )
                        .roles("SWAGGER")
                        .build();

        UserDetailsService swaggerUserDetailsService =
                new InMemoryUserDetailsManager(
                        swaggerUser
                );

        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(
                        swaggerUserDetailsService
                );

        authenticationProvider.setPasswordEncoder(
                passwordEncoder
        );

        http
                .securityMatcher(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                )
                .authenticationProvider(
                        authenticationProvider
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .anyRequest()
                                .hasRole("SWAGGER")
                )
                .httpBasic(
                        Customizer.withDefaults()
                )
                .csrf(
                        AbstractHttpConfigurer::disable
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                );

        return http.build();
    }
}