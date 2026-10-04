package com.fcv.citas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcv.citas.adapter.in.security.AutomationKeyAuthenticationFilter;
import com.fcv.citas.adapter.out.security.JwtTokenAdapter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenAdapter jwtTokens,
                                            ObjectMapper objectMapper,
                                            CorsConfigurationSource corsConfigurationSource,
                                            @org.springframework.beans.factory.annotation.Value("${app.automation.api-key:}")
                                            String automationKey) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authorities);

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password-reset/request",
                                "/api/v1/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // La clave de automatización solo vale en /api/v1/automation/**; un JWT de usuario ahí → 403.
                        .requestMatchers("/api/v1/automation/**").hasRole("AUTOMATION")
                        .anyRequest().access((authentication, context) -> new AuthorizationDecision(
                                isUserSession(authentication.get()))))
                .addFilterBefore(new AutomationKeyAuthenticationFilter(automationKey, response ->
                                writeProblem(response, objectMapper, 401, "UNAUTHORIZED", "Authentication is required")),
                        BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.decoder(jwtTokens.accessDecoder())
                                .jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeProblem(response, objectMapper, 401, "UNAUTHORIZED", "Authentication is required")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        writeProblem(response, objectMapper, 403, "FORBIDDEN", "Access is denied")))
                .build();
    }

    /** Autenticado y no es la credencial de automatización (que no sirve fuera de /automation). */
    private static boolean isUserSession(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities().stream()
                .noneMatch(a -> AutomationKeyAuthenticationFilter.ROLE.equals(a.getAuthority()));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.allowedOrigin()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static void writeProblem(HttpServletResponse response, ObjectMapper mapper, int status,
                                     String code, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(detail);
        problem.setProperty("code", code);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
