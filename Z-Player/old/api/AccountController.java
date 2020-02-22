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

package api;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import com.tgx.chess.spring.auth.model.AccountEntity;
import com.tgx.chess.spring.auth.model.AccountStatus;
import com.tgx.chess.spring.auth.model.RoleEntity;
import com.tgx.chess.spring.auth.model.RoleEnum;
import com.tgx.chess.spring.auth.service.AccountService;

import api.dao.AuthEntry;
import api.dao.ProfileEntry;

/**
 * @author william.d.zk
 */
@RestController
@CrossOrigin(origins = "http://localhost:4444")
public class AccountController
{
    private final AccountService _AccountService;

    public AccountController(AccountService accountService)
    {
        _AccountService = accountService;
    }

    @PostMapping("/account/register")
    public @ResponseBody Object register(@RequestBody AccountDo accountDo)
    {
        Optional<AccountEntity> test = _AccountService.findByEmail(accountDo.getEmail());
        if (test.isPresent()) {
            throw new IllegalArgumentException("email exist");
        }
        else {
            test = _AccountService.findByName(accountDo.getUserName());
            if (test.isPresent()) { throw new IllegalArgumentException("user_name exist"); }
        }
        AuthEntry auth = new AuthEntry();
        auth.setStatus(AccountStatus.CREATED);
        auth.setRole(RoleEnum.USER);
        AccountEntity account = new AccountEntity();
        account.setActive(1);
        account.setName(accountDo.getUserName());
        account.setEmail(accountDo.getEmail());
        account.setPassword(accountDo.getPassword());
        _AccountService.newAccount(account);
        auth.setAuth(account.getAuth());
        auth.setSecret(account.getSecret());
        return auth;
    }

    @PostMapping("/account/login")
    public @ResponseBody AuthEntry validate(@RequestBody AccountDo accountDo)
    {
        AuthEntry authEntry = new AuthEntry();
        AccountEntity account = _AccountService.findByName(accountDo.getUserName())
                                               .orElse(_AccountService.findByEmail(accountDo.getEmail())
                                                                      .orElse(_AccountService.findByAuth(accountDo.getAuth())
                                                                                             .orElse(null)));
        if (Objects.nonNull(account)
            && account.getPassword()
                      .equals(accountDo.getPassword()))
        {
            authEntry.setStatus(AccountStatus.ONLINE);
            authEntry.setRoles(account.getRoles()
                                      .stream()
                                      .map(RoleEntity::getRole)
                                      .collect(Collectors.toList()));
        }
        else {
            authEntry.setStatus(AccountStatus.INVALID);
        }

        return authEntry;
    }

    @PostMapping(value = "/account/logout")
    public @ResponseBody AuthEntry logout()
    {
        AuthEntry auth = new AuthEntry();
        auth.setStatus(AccountStatus.OFFLINE);
        return auth;
    }

    @GetMapping(value = "/account/profile")
    public @ResponseBody ProfileEntry profile(HttpSession session)
    {
        System.out.println(session.getAttributeNames());
        ProfileEntry profile = new ProfileEntry();
        profile.setName("幂等");
        return profile;
    }

    @GetMapping("/account/init")
    public void init()
    {
        _AccountService.initializeCheck();
    }
}
