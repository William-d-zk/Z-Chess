package com.tgx.chess.pawn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.tgx.chess.pawn.error.PawnAccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig
        extends
        WebSecurityConfigurerAdapter
{

    private final PawnAccessDeniedHandler _AccessDeniedHandler;

    @Autowired
    public SecurityConfig(PawnAccessDeniedHandler accessDeniedHandler) {
        _AccessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
            .disable()
            .authorizeRequests()
            .antMatchers("/", "/home", "/about")
            .permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .formLogin()
            .loginPage("/login")
            .permitAll()
            .and()
            .logout()
            .permitAll()
            .and()
            .exceptionHandling()
            .accessDeniedHandler(_AccessDeniedHandler);
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication()
            .withUser("user")
            .password("password")
            .roles("USER")
            .and()
            .withUser("admin")
            .password("password")
            .roles("ADMIN");
    }
}
