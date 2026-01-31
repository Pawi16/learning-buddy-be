package dev.pawin.backend_learning_buddy.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean; // ✅ Import this
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered; // ✅ Import this
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter; // ✅ Import this

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // We handle CORS with the specialized filter below, but we keep this as a fallback
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(req -> req
                // 1. Allow OPTIONS requests explicitly
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 2. Allow Auth endpoints
                .requestMatchers("/api/v1/auth/**", "/error").permitAll()
                // 3. Lock down everything else
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ✅ THE FIX: This filter runs BEFORE Spring Security.
     * It guarantees the CORS headers are added to every response, 
     * preventing the 403 error on Preflight.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> CorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Allow your frontend
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        
        // Allow ALL methods (GET, POST, OPTIONS, PUT, DELETE)
        config.setAllowedMethods(List.of("*"));
        
        // Allow ALL headers
        config.setAllowedHeaders(List.of("*"));
        
        // Allow credentials (cookies/tokens)
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        
        CorsFilter corsFilter = new CorsFilter(source);
        
        // Register the filter with HIGHEST precedence
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}