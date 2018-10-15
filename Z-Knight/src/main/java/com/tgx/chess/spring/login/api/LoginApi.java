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

package com.tgx.chess.spring.login.api;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.tgx.chess.spring.login.service.AccountService;

@RestController
@CrossOrigin(origins = "http://localhost:4444")
public class LoginApi
{
    private final AccountService accountService;

    @Autowired
    public LoginApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(value = "/api/login")
    public @ResponseBody Object validate(@RequestBody Object param, HttpSession httpSession) {
        System.out.println(param);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("currentAuthority", "admin");
        return response;
    }

    @PostMapping(value = "/api/register")
    public @ResponseBody Object register(@RequestBody Object param, HttpSession httpSession) {
        System.out.println(param);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("currentAuthority", "user");
        return response;
    }

    @PostMapping(value = "/logout")
    public String logout(HttpSession httpSession) {
        return "Admin";
    }

    @GetMapping(value = "/profile")
    public String profile(HttpSession httpSession) {
        return "Admin";
    }
}
