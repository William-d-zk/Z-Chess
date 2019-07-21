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

package com.tgx.chess.spring.auth.api.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.tgx.chess.spring.auth.model.RoleEnum;

/**
 * @author william.d.zk
 */
public class AuthEntry
        implements
        Serializable
{
    private static final long    serialVersionUID = -9052656763177899443L;
    private boolean              status;
    private Collection<RoleEnum> roles;

    public boolean isStatus()
    {
        return status;
    }

    public void setStatus(boolean status)
    {
        this.status = status;
    }

    public Collection<RoleEnum> getRoles()
    {
        return roles;
    }

    public void setRoles(Collection<RoleEnum> roles)
    {
        this.roles = roles;
    }

    public void setRole(RoleEnum role)
    {
        ArrayList<RoleEnum> list = new ArrayList<>(1);
        list.add(role);
        setRoles(list);
    }
}
