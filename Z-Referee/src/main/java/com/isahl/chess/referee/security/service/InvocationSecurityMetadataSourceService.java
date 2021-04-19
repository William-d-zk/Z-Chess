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

package com.isahl.chess.referee.security.service;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.referee.security.jpa.model.PermissionEntity;
import com.isahl.chess.referee.security.jpa.model.RoleEntity;
import com.isahl.chess.referee.security.jpa.repository.IPermissionRepository;
import com.isahl.chess.referee.security.jpa.repository.IRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2021/3/1
 */
@Service
public class InvocationSecurityMetadataSourceService
        implements
        FilterInvocationSecurityMetadataSource
{
    private final Logger _Logger = Logger.getLogger("security.referee." + getClass().getSimpleName());

    private final IPermissionRepository             _PermissionRepository;
    private final IRoleRepository                   _RoleRepository;
    private final Map<String,
                      List<SecurityConfig>>         _AttributeMap;

    @Autowired
    public InvocationSecurityMetadataSourceService(IPermissionRepository permissionRepository,
                                                   IRoleRepository roleRepository)
    {
        _PermissionRepository = permissionRepository;
        _RoleRepository = roleRepository;
        List<PermissionEntity> permissions = _PermissionRepository.findAll();
        List<RoleEntity> roles = _RoleRepository.findAll();
        _AttributeMap = permissions.stream()
                                   .map(p -> new Pair<>(p.getUrl(),
                                                        roles.stream()
                                                             .filter(r -> r.getPermissions()
                                                                           .stream()
                                                                           .anyMatch(rp -> rp.getUrl()
                                                                                             .equals(p.getUrl())))
                                                             .map(r -> new SecurityConfig(r.getAuthority()))
                                                             .collect(Collectors.toList())))
                                   .collect(Collectors.toConcurrentMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(@NonNull Object param) throws IllegalArgumentException
    {
        HttpServletRequest request = ((FilterInvocation) param).getHttpRequest();
        return _RoleRepository.findAll()
                              .stream()
                              .filter(role -> role.getPermissions()
                                                  .stream()
                                                  .anyMatch(p -> new AntPathRequestMatcher(p.getUrl()).matches(request)))
                              .map(role -> new SecurityConfig(role.getAuthority()))
                              .collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes()
    {
        return _AttributeMap.values()
                            .stream()
                            .flatMap(Collection::stream)
                            .distinct()
                            .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        _Logger.info("supports:%s", clazz);
        return true;
    }
}
