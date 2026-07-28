package ec.edu.ups.icc.academicevents.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import ec.edu.ups.icc.academicevents.ratelimit.RateLimitFilter;
import ec.edu.ups.icc.academicevents.security.filters.JwtAuthenticationFilter;
import ec.edu.ups.icc.academicevents.security.handlers.CustomAccessDeniedHandler;
import ec.edu.ups.icc.academicevents.security.handlers.CustomAuthenticationEntryPoint;
import ec.edu.ups.icc.academicevents.security.services.CustomUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService
            customUserDetailsService;

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    private final CustomAuthenticationEntryPoint
            authenticationEntryPoint;

    private final CustomAccessDeniedHandler
            accessDeniedHandler;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) {
        this.customUserDetailsService =
                customUserDetailsService;

        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.authenticationEntryPoint =
                authenticationEntryPoint;

        this.accessDeniedHandler =
                accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider
    authenticationProvider(
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        customUserDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return provider;
    }

    @Bean
    public AuthenticationManager
    authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider
                    authenticationProvider,
            CorsConfigurationSource
                    corsConfigurationSource,
            RateLimitFilter rateLimitFilter
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authenticationProvider(
                        authenticationProvider
                )

                .authorizeHttpRequests(authorize ->
                        authorize

                                // Las peticiones preflight OPTIONS del
                                // navegador nunca llevan el header
                                // Authorization. Deben permitirse
                                // explicitamente o el CORS real nunca
                                // funciona desde un frontend en el navegador.
                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/auth/register",
                                        "/auth/login",
                                        "/auth/refresh",
                                        "/auth/logout",
                                        "/actuator/health",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/error"
                                )
                                .permitAll()

                                // IMPORTANTE: esta regla de registrations
                                // debe ir ANTES que el permitAll() de
                                // "/events/**" de mas abajo, porque
                                // Spring Security usa la primera regla
                                // que haga match con la ruta.
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/events/*/registrations"
                                )
                                .hasAnyAuthority(
                                        "ADMIN",
                                        "ORGANIZER",
                                        "ROLE_ADMIN",
                                        "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/events/*/registrations"
                                )
                                .hasAnyAuthority(
                                        "PARTICIPANT",
                                        "ROLE_PARTICIPANT"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/categories",
                                        "/events",
                                        "/events/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/events"
                                )
                                .hasAnyAuthority(
                                        "ADMIN",
                                        "ORGANIZER",
                                        "ROLE_ADMIN",
                                        "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/events/**"
                                )
                                .hasAnyAuthority(
                                        "ADMIN",
                                        "ORGANIZER",
                                        "ROLE_ADMIN",
                                        "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/events/**"
                                )
                                .hasAnyAuthority(
                                        "ADMIN",
                                        "ORGANIZER",
                                        "ROLE_ADMIN",
                                        "ROLE_ORGANIZER"
                                )
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/sessions/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/events/*/sessions"
                                )
                                .hasAnyAuthority(
                                        "ADMIN", "ORGANIZER", "ROLE_ADMIN", "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/sessions/**"
                                )
                                .hasAnyAuthority(
                                        "ADMIN", "ORGANIZER", "ROLE_ADMIN", "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/sessions/**"
                                )
                                .hasAnyAuthority(
                                        "ADMIN", "ORGANIZER", "ROLE_ADMIN", "ROLE_ORGANIZER"
                                )

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/registrations/*/status"
                                )
                                .hasAnyAuthority(
                                        "ADMIN", "ORGANIZER", "ROLE_ADMIN", "ROLE_ORGANIZER"
                                )

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exception ->
                        exception

                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )

                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterAfter(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }
}