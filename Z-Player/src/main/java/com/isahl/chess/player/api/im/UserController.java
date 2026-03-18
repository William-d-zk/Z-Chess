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

package com.isahl.chess.player.api.im;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.player.domain.User;
import com.isahl.chess.player.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/im/users")
public class UserController
{
    private final UserRepository _UserRepository;

    @Autowired
    public UserController(UserRepository userRepository)
    {
        _UserRepository = userRepository;
    }

    @GetMapping("{userId}")
    public ZResponse<?> getUser(@PathVariable Long userId)
    {
        Optional<User> userOpt = _UserRepository.findById(userId);
        if(userOpt.isEmpty()) {
            return ZResponse.error("User not found");
        }
        User user = userOpt.get();
        return ZResponse.success(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "online", user.getOnline()
        ));
    }

    @GetMapping("search")
    public ZResponse<?> searchUsers(@RequestParam String username)
    {
        Optional<User> userOpt = _UserRepository.findByUsername(username);
        if(userOpt.isEmpty()) {
            return ZResponse.error("User not found");
        }
        User user = userOpt.get();
        return ZResponse.success(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName()
        ));
    }
}
