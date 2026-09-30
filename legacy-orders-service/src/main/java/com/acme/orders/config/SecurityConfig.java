/*
 * ============================================================================
 * WARNING: INTENTIONALLY INSECURE — MIGRATION DEMO FIXTURE.
 *
 * This class deliberately uses removed/deprecated Spring Security APIs and
 * plaintext credentials, reproducing what a pre-Boot-3 service typically
 * looks like. It is not production code and must never be copied into one.
 * See docs/HAZARDS.md (section A2).
 * ============================================================================
 */
package com.acme.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/orders/**").hasRole("VIEWER")
                .antMatchers(HttpMethod.POST, "/api/orders").hasRole("OPERATOR")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .httpBasic()
            .and()
            .sessionManagement().sessionFixation().migrateSession();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    @Override
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        UserDetails viewer = User.withUsername("viewer").password("viewer").roles("VIEWER").build();
        UserDetails operator = User.withUsername("operator").password("operator").roles("VIEWER", "OPERATOR").build();
        UserDetails admin = User.withUsername("admin").password("admin").roles("VIEWER", "OPERATOR", "ADMIN").build();
        return new InMemoryUserDetailsManager(viewer, operator, admin);
    }
}
