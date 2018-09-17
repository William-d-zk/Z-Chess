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

package com.tgx.chess.spring.login.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.tgx.chess.spring.login.model.Account;

@Component
public class UserDetailServiceImpl
        implements
        UserDetailsService
{
    private final RoleService    _RoleService;
    private final AccountService _AccountService;

    @Autowired
    public UserDetailServiceImpl(AccountService accountService, RoleService roleService) {
        _RoleService = roleService;
        _AccountService = accountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (StringUtils.isBlank(username)) { throw new UsernameNotFoundException("用户名为空"); }

        Account login = _AccountService.findByName(username)
                                       .orElse(_AccountService.findByEmail(username)
                                                              .orElseThrow(() -> new UsernameNotFoundException("用户不存在")));

        Set<GrantedAuthority> authorities = new HashSet<>();
        login.getRoles()
             .forEach(r -> authorities.add(new SimpleGrantedAuthority(r.getRole())));

        return new User(username,
                        login.getPassword(),
                        true,//是否可用
                        true,//是否过期
                        true,//证书不过期为true
                        true,//账户未锁定为true
                        authorities);
    }
}
