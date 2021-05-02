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

package com.isahl.chess.referee.start;

import com.isahl.chess.referee.security.jpa.repository.IPermissionRepository;
import com.isahl.chess.referee.security.jpa.repository.IRoleRepository;
import com.isahl.chess.referee.security.service.InvocationSecurityMetadataSourceService;
import com.isahl.chess.referee.security.service.UserDetailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@SpringBootTest
class ApplicationRefereeTest
{
    @Autowired
    IRoleRepository                         roleRepository;
    @Autowired
    IPermissionRepository                   permissionRepository;
    @Autowired
    InvocationSecurityMetadataSourceService invocationSecurityMetadataSourceService;

    @Test
    void getRole()
    {
        Collection<ConfigAttribute> collection = invocationSecurityMetadataSourceService.getAllConfigAttributes();
        System.out.println(collection);
    }

    @Autowired
    UserDetailServiceImpl userDetailService;

    @Test
    void getUser()
    {
        UserDetails user = userDetailService.loadUserByUsername("root");
        System.out.println(user);
    }
}