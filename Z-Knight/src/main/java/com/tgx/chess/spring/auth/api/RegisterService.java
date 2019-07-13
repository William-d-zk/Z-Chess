/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.spring.auth.api;

import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.spring.auth.api.dao.AuthEntry;
import com.tgx.chess.spring.auth.model.AccountEntity;
import com.tgx.chess.spring.auth.service.AccountService;

/**
 * @author william.d.zk
 */
@RestController
@CrossOrigin(origins = "http://localhost:4444")
public class RegisterService
{
    private final AccountService _AccountService;

    public RegisterService(AccountService accountService)
    {
        _AccountService = accountService;
    }

    @PostMapping(value = "/api/register")
    public @ResponseBody AuthEntry register(@RequestBody Map<String,
                                                             String> param)
    {
        System.out.println(param);
        String username = param.get("name");
        String email = param.get("email");
        String password = param.get("passwd");
        Optional<AccountEntity> test = _AccountService.findByEmail(email);
        if (test.isPresent()) {
            throw new IllegalArgumentException("email exist");
        }
        else {
            test = _AccountService.findByName(username);
            if (test.isPresent()) { throw new IllegalArgumentException("username exist"); }
        }
        AuthEntry auth = new AuthEntry();
        auth.setStatus(true);
        auth.setRole("USER");
        AccountEntity account = new AccountEntity();
        account.setActive(1);
        account.setName(username);
        account.setEmail(email);
        account.setPassword(password);
        _AccountService.saveAccount(account);
        return auth;
    }
}
