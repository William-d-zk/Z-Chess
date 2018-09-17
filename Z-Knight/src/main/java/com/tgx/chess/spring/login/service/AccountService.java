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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tgx.chess.spring.login.model.Account;
import com.tgx.chess.spring.login.model.Role;
import com.tgx.chess.spring.login.repository.AccountRepository;
import com.tgx.chess.spring.login.repository.RoleRepository;

@Service
public class AccountService
{

    private final AccountRepository _AccountRepository;
    private final RoleRepository    _RoleRepository;

    @Bean
    public BCryptPasswordEncoder get_BCryptPasswordEncoder() {
        return _BCryptPasswordEncoder;
    }

    private final BCryptPasswordEncoder _BCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AccountService(AccountRepository accountRepository, RoleRepository roleRepository) {
        _AccountRepository = accountRepository;
        _RoleRepository = roleRepository;
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(_AccountRepository.findByEmail(email));
    }

    public Optional<Account> findByName(String name) {
        return Optional.ofNullable(_AccountRepository.findByName(name));
    }

    public void saveAccount(Account account) {
        account.setPassword(_BCryptPasswordEncoder.encode(account.getPassword()));
        account.setActive(1);
        Role role = _RoleRepository.findByRole("ADMIN");
        if (Objects.isNull(role)) {
            role = new Role();
            role.setRole("ADMIN");
            _RoleRepository.save(role);
        }
        account.setRoles(new HashSet<>(Collections.singletonList(role)));
        _AccountRepository.save(account);
    }

}