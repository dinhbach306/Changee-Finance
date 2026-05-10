package org.dinhb.microservice.core.auth.config;

import org.dinhb.microservice.core.common.security.JwtUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(JwtConfig.JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtUtils jwtUtils(JwtProperties props) {
        return new JwtUtils(props.secret(), props.issuer(), props.ttl());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @ConfigurationProperties(prefix = "auth.jwt")
    public record JwtProperties(String secret, String issuer, Duration ttl) {}
}
