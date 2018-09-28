/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.spring.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.tgx.chess.spring.login.service.UserDetailServiceImpl;
import com.tgx.chess.spring.web.error.TgxAccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig
        extends
        WebSecurityConfigurerAdapter
{

    private final TgxAccessDeniedHandler  _AccessDeniedHandler;
    private final BCryptPasswordEncoder   _BCryptPasswordEncoder;
    private final UserDetailServiceImpl   _UserDetailServiceImpl;
    private final DelegatingMessageSource _DelegatingMessageSource;

    @Autowired
    public SecurityConfig(TgxAccessDeniedHandler accessDeniedHandler,
                          BCryptPasswordEncoder bCryptPasswordEncoder,
                          UserDetailServiceImpl userDetailService,
                          DelegatingMessageSource delegatingMessageSource) {
        _AccessDeniedHandler = accessDeniedHandler;
        _BCryptPasswordEncoder = bCryptPasswordEncoder;
        _UserDetailServiceImpl = userDetailService;
        _DelegatingMessageSource = delegatingMessageSource;
    }

    @Bean
    public MessageSource validationMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:validation");
        messageSource.setDefaultEncoding("UTF-8");
        _DelegatingMessageSource.setParentMessageSource(messageSource);
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(validationMessageSource());
        return bean;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
            .disable()
            .authorizeRequests()
            .antMatchers("/", "/login", "/registration")
            .permitAll()
            .antMatchers("/admin/**")
            .hasAuthority("ADMIN")
            .anyRequest()
            .authenticated()
            .and()
            .formLogin()
            .loginPage("/login")
            .failureUrl("/login?error=true")
            .defaultSuccessUrl("/admin/home")
            .usernameParameter("email")
            .passwordParameter("password")
            .permitAll()
            .and()
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login")
            .and()
            .exceptionHandling()
            .accessDeniedHandler(_AccessDeniedHandler);
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(_UserDetailServiceImpl)
            .passwordEncoder(_BCryptPasswordEncoder);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
           .antMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**")
           .antMatchers("/client/**");
    }
}
