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
import com.isahl.chess.king.base.util.CryptoUtil;
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

    //TODO 修改为DDL模版存储在rook/referee里
    @PostConstruct
    private void initJpaData()
    {
        List<RoleEntity> roles = new ArrayList<>(4);
        List<PermissionEntity> permissions = new ArrayList<>(4);
        PermissionEntity common_p, admin_p, root_p;
        RoleEntity admin_r, root_r, user_r;
        common_p = _PermissionRepository.findByName("common");
        admin_p = _PermissionRepository.findByName("admin");
        root_p = _PermissionRepository.findByName("root");

        if(common_p == null) {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("common");
            permission.setUrl("/user/common");
            permission.setDescription("common user permission");
            common_p = _PermissionRepository.save(permission);
        }
        if(admin_p == null) {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("admin");
            permission.setUrl("/user/admin");
            permission.setDescription("administrator  permission");
            admin_p = _PermissionRepository.save(permission);
        }

        if(root_p == null) {
            PermissionEntity permission = new PermissionEntity();
            permission.setName("root");
            permission.setUrl("/user/root");
            permission.setDescription("root all permission");
            root_p = _PermissionRepository.save(permission);
        }
        root_r = _RoleRepository.findByName("root");
        user_r = _RoleRepository.findByName("user");
        admin_r = _RoleRepository.findByName("admin");

        if(root_r == null) {
            RoleEntity role = new RoleEntity();
            role.setName("root");
            role.setPermissions(permissions);
            permissions.add(common_p);
            permissions.add(root_p);
            permissions.add(admin_p);
            root_r = _RoleRepository.save(role);
            permissions.clear();
        }
        if(admin_r == null) {
            RoleEntity role = new RoleEntity();
            role.setName("admin");
            role.setPermissions(permissions);
            permissions.add(admin_p);
            permissions.add(common_p);
            admin_r = _RoleRepository.save(role);
            permissions.clear();
        }
        if(user_r == null) {
            RoleEntity role = new RoleEntity();
            role.setName("user");
            role.setPermissions(permissions);
            permissions.add(common_p);
            user_r = _RoleRepository.save(role);
            permissions.clear();
        }

        List<UserEntity> users = _UserRepository.findAll();
        if(users.stream()
                .noneMatch(user->user.getUsername()
                                     .equals("root")))
        {
            UserEntity user = new UserEntity();
            user.setUsername("root");
            List<RoleEntity> authorities = new ArrayList<>(2);
            authorities.add(root_r);
            authorities.add(admin_r);
            authorities.add(user_r);
            user.setAuthorities(authorities);
            user.setInvalidAt(LocalDateTime.now()
                                           .plusYears(5));
            String password = CryptoUtil.Password(17, 32);
            _Logger.info("user:%s,pwd-plain:%s", user.getUsername(), password);
            user.setPassword(_PasswordEncoder.encode(password));
            _UserRepository.save(user);
        }

        if(users.stream()
                .noneMatch(user->user.getUsername()
                                     .equals("user")))
        {
            UserEntity user = new UserEntity();
            user.setUsername("user");
            user.setAuthorities(Collections.singletonList(user_r));
            user.setInvalidAt(LocalDateTime.now()
                                           .plusYears(1));
            String password = CryptoUtil.Password(9, 12);
            _Logger.info("user:%s,pwd-plain:%s", user.getUsername(), password);
            user.setPassword(_PasswordEncoder.encode(password));
            _UserRepository.save(user);
        }

        if(users.stream()
                .noneMatch(user->user.getUsername()
                                     .equals("admin")))
        {
            UserEntity user = new UserEntity();
            user.setUsername("admin");
            List<RoleEntity> authorities = new ArrayList<>(2);
            authorities.add(admin_r);
            authorities.add(user_r);
            user.setAuthorities(authorities);
            user.setInvalidAt(LocalDateTime.now()
                                           .plusYears(1));
            String password = CryptoUtil.Password(9, 12);
            _Logger.info("user:%s,pwd-plain:%s", user.getUsername(), password);
            user.setPassword(_PasswordEncoder.encode(password));
            _UserRepository.save(user);
        }
    }
}
