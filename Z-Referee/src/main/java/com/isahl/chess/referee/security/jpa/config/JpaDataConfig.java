/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.referee.security.jpa.config;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.referee.security.jpa.model.PermissionEntity;
import com.isahl.chess.referee.security.jpa.model.RoleEntity;
import com.isahl.chess.referee.security.jpa.model.UserEntity;
import com.isahl.chess.referee.security.jpa.repository.IPermissionRepository;
import com.isahl.chess.referee.security.jpa.repository.IRoleRepository;
import com.isahl.chess.referee.security.jpa.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class JpaDataConfig
{
    private final IRoleRepository       _RoleRepository;
    private final IUserRepository       _UserRepository;
    private final IPermissionRepository _PermissionRepository;
    private final BCryptPasswordEncoder _PasswordEncoder;

    private final Logger _Logger = Logger.getLogger("security.referee." + getClass().getSimpleName());

    @Autowired
    public JpaDataConfig(IRoleRepository roleRepository,
                         IUserRepository userRepository,
                         IPermissionRepository permissionRepository,
                         BCryptPasswordEncoder passwordEncoder)
    {
        _RoleRepository = roleRepository;
        _UserRepository = userRepository;
        _PermissionRepository = permissionRepository;
        _PasswordEncoder = passwordEncoder;
    }

    @PostConstruct
    private void initJpaData()
    {
        List<RoleEntity> roles = _RoleRepository.findAll();
        List<UserEntity> users = _UserRepository.findAll();
        List<PermissionEntity> permissions = _PermissionRepository.findAll();
        if (roles.stream()
                 .noneMatch(role -> role.getName()
                                        .equals("root")))
        {
            RoleEntity role = new RoleEntity();
            role.setName("root");
            _RoleRepository.save(role);
        }
        if (roles.stream()
                 .noneMatch(role -> role.getName()
                                        .equals("admin")))
        {
            RoleEntity role = new RoleEntity();
            role.setName("admin");
            _RoleRepository.save(role);
        }
        if (roles.stream()
                 .noneMatch(role -> role.getName()
                                        .equals("user")))
        {
            RoleEntity role = new RoleEntity();
            role.setName("user");
            _RoleRepository.save(role);
        }

        if (users.stream()
                 .noneMatch(user -> user.getUsername()
                                        .equals("root")))
        {
            UserEntity user = new UserEntity();
            user.setUsername("root");
            List<RoleEntity> authorities = new ArrayList<>(2);
            authorities.add(_RoleRepository.findByName("root"));
            authorities.add(_RoleRepository.findByName("admin"));
            user.setAuthorities(authorities);
            user.setInvalidAt(LocalDateTime.now()
                                           .plusYears(5));
            String password = CryptUtil.Password(17, 32);
            _Logger.info("user:%s,pwd-plain:%s", user.getUsername(), password);
            user.setPassword(_PasswordEncoder.encode(password));
            _UserRepository.save(user);
        }
        if (users.stream()
                 .noneMatch(user -> user.getUsername()
                                        .equals("user")))
        {
            UserEntity user = new UserEntity();
            user.setUsername("user");
            user.setAuthorities(Collections.singletonList(_RoleRepository.findByName("user")));
            user.setInvalidAt(LocalDateTime.now()
                                           .plusYears(1));
            String password = CryptUtil.Password(9, 12);
            _Logger.info("user:%s,pwd-plain:%s", user.getUsername(), password);
            user.setPassword(_PasswordEncoder.encode(password));
            _UserRepository.save(user);
        }
        if (permissions.stream()
                       .noneMatch(permission -> permission.getName()
                                                          .equals("common")))
        {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("common");
            permission.setUrl("/user/common");
            permission.setDescription("common user permission");
            _PermissionRepository.save(permission);
            RoleEntity role = _RoleRepository.findByName("user");
            role.getPermissions()
                .add(permission);
            _RoleRepository.save(role);
            role = _RoleRepository.findByName("root");
            role.getPermissions()
                .add(permission);
            _RoleRepository.save(role);
            role = _RoleRepository.findByName("admin");
            role.getPermissions()
                .add(permission);
            _RoleRepository.save(role);
        }
        if (permissions.stream()
                       .noneMatch(permission -> permission.getName()
                                                          .equals("admin")))
        {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("admin");
            permission.setUrl("/user/admin");
            permission.setDescription("administrator  permission");
            RoleEntity role = _RoleRepository.findByName("admin");
            role.getPermissions()
                .add(permission);
            _PermissionRepository.save(permission);
            _RoleRepository.save(role);
        }

        if (permissions.stream()
                       .noneMatch(permission -> permission.getName()
                                                          .equals("root")))
        {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("root");
            permission.setUrl("/user/root");
            permission.setDescription("root all permission");
            RoleEntity role = _RoleRepository.findByName("root");
            role.getPermissions()
                .add(permission);
            _PermissionRepository.save(permission);
            _RoleRepository.save(role);
        }
    }
}
