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

package com.tgx.chess.spring.auth.api;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.spring.auth.api.dto.AuthDTO;
import com.tgx.chess.spring.auth.model.Account;
import com.tgx.chess.spring.auth.model.Role;
import com.tgx.chess.spring.auth.service.AccountService;

@RestController
@CrossOrigin(origins = "http://localhost:4444")
public class LoginService
{
    private final AccountService _AccountService;

    @Autowired
    public LoginService(AccountService accountService) {
        _AccountService = accountService;
    }

    @PostMapping(value = "/api/auth")
    public @ResponseBody AuthDTO validate(@RequestBody Map<String, String> param, HttpSession httpSession) {
        System.out.println(param);
        AuthDTO auth = new AuthDTO();
        String username = param.get("username");
        String password = param.get("password");
        Account account = _AccountService.findByName(username)
                                         .orElse(_AccountService.findByEmail(username)
                                                                .orElse(null));
        if (Objects.nonNull(password) && Objects.nonNull(account) && password.equals(account.getPassword())) {
            auth.setStatus(true);
            auth.setRoles(account.getRoles()
                                 .stream()
                                 .map(Role::getRole)
                                 .collect(Collectors.toList()));
        }
        else {
            auth.setStatus(false);
        }

        return auth;
    }

    @PostMapping(value = "/api/logout")
    public @ResponseBody Object logout(HttpSession httpSession) {
        return "Admin";
    }

    @GetMapping(value = "/api/profile")
    public @ResponseBody Object profile(HttpSession httpSession) {
        return "Admin";
    }
}
