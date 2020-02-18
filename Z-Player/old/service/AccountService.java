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

package service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;

import model.AccountEntity;
import model.ProfileEntity;
import model.RoleEntity;
import model.RoleEnum;
import repository.AccountRepository;
import repository.ProfileRepository;
import repository.RoleRepository;

/**
 * @author william.d.zk
 */
@Service
public class AccountService
{

    private final Logger            _Logger    = Logger.getLogger(this.getClass()
                                                                      .getSimpleName());
    private final AccountRepository _AccountRepository;
    private final RoleRepository _RoleRepository;
    private final ProfileRepository _ProfileRepository;
    private final CryptUtil         _CryptUtil = new CryptUtil();

    @Autowired
    public AccountService(AccountRepository accountRepository,
                          RoleRepository roleRepository,
                          ProfileRepository profileRepository)
    {
        _AccountRepository = accountRepository;
        _RoleRepository = roleRepository;
        _ProfileRepository = profileRepository;
    }

    public void initializeCheck()
    {
        RoleEntity admin = _RoleRepository.findByRole(RoleEnum.ADMIN);
        RoleEntity user = _RoleRepository.findByRole(RoleEnum.USER);
        if (Objects.isNull(admin)) {
            admin = new RoleEntity();
            admin.setRole(RoleEnum.ADMIN);
            _RoleRepository.save(admin);
        }
        if (Objects.isNull(user)) {
            user = new RoleEntity();
            user.setRole(RoleEnum.USER);
            _RoleRepository.save(user);
        }
        AccountEntity test = _AccountRepository.findByName("root");
        if (Objects.isNull(test)) {
            ProfileEntity profile = new ProfileEntity();
            AccountEntity root = new AccountEntity();
            root.setActive(1);
            root.setName("root");
            root.setPassword("root12345");
            root.setRoles(new HashSet<>(Collections.singletonList(admin)));
            root.setEmail("z-chess@tgxstudio.com");
            root.setProfile(profile);
            profile.setAccount(root);
            newAccount(root);
        }
    }

    public Optional<AccountEntity> findByEmail(String email)
    {
        return Optional.ofNullable(_AccountRepository.findByEmail(email));
    }

    public Optional<AccountEntity> findByName(String name)
    {
        return Optional.ofNullable(_AccountRepository.findByName(name));
    }

    public Optional<AccountEntity> findByAuth(String auth)
    {
        return Optional.ofNullable(_AccountRepository.findByAuth(auth));
    }

    public void newAccount(AccountEntity account)
    {
        account.setRoles(new HashSet<>(Collections.singletonList(_RoleRepository.findByRole(RoleEnum.USER))));
        String source = String.format("email:%s|user_name:%s", account.getEmail(), account.getName());
        _Logger.info("save account %s", source);
        String auth = _CryptUtil.sha256(source);
        account.setAuth(auth);
        String salt = _CryptUtil.randomPassword(8, 16);
        account.setSalt(salt);
        account.setSecret(_CryptUtil.md5((account.getPassword() + salt)));
        _AccountRepository.save(account);
    }

    public void updateAccount(AccountEntity account)
    {
        _AccountRepository.save(account);
    }

    public AccountEntity authAccount(String auth, String cipher, String plain)
    {
        AccountEntity accountEntity = _AccountRepository.findByAuth(auth);
        if (accountEntity != null) {
            String secret = accountEntity.getSecret();
            String result = _CryptUtil.sha256(secret + plain);
            if (result.equalsIgnoreCase(cipher)) { return accountEntity; }
        }
        return null;
    }

}