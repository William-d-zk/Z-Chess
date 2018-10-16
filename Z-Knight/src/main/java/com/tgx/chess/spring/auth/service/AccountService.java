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

package com.tgx.chess.spring.auth.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tgx.chess.spring.auth.model.Account;
import com.tgx.chess.spring.auth.model.Role;
import com.tgx.chess.spring.auth.repository.AccountRepository;
import com.tgx.chess.spring.auth.repository.RoleRepository;

@Service
public class AccountService
{

    private final AccountRepository _AccountRepository;
    private final RoleRepository    _RoleRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, RoleRepository roleRepository) {
        _AccountRepository = accountRepository;
        _RoleRepository = roleRepository;
    }

    public void initializeCheck() {
        Role admin = _RoleRepository.findByRole("ADMIN");
        Role user = _RoleRepository.findByRole("USER");
        if (Objects.isNull(admin)) {
            admin = new Role();
            admin.setRole("ADMIN");
            _RoleRepository.save(admin);
        }
        if (Objects.isNull(user)) {
            user = new Role();
            user.setRole("USER");
            _RoleRepository.save(user);
        }
        Account test = _AccountRepository.findByName("root");
        if (Objects.isNull(test)) {
            Account root = new Account();
            root.setActive(1);
            root.setName("root");
            root.setPassword("root");
            root.setRoles(new HashSet<>(Collections.singletonList(admin)));
            root.setEmail("z-chess@tgxstudio.com");
            _AccountRepository.save(root);
        }
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(_AccountRepository.findByEmail(email));
    }

    public Optional<Account> findByName(String name) {
        return Optional.ofNullable(_AccountRepository.findByName(name));
    }

    public void saveAccount(Account account) {
        account.setPassword(account.getPassword());
        account.setActive(1);
        Role role = _RoleRepository.findByRole("USER");
        account.setRoles(new HashSet<>(Collections.singletonList(role)));
        _AccountRepository.save(account);
    }

}