package com.tgx.chess.spring.web.error;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.tgx.z.king.base.log.Logger;

// handle 403 page
@Component
public class TgxAccessDeniedHandler
        implements
        AccessDeniedHandler
{

    private final Logger _Log = Logger.getLogger(getClass().getName());

    @Override
    public void handle(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse,
                       AccessDeniedException e) throws IOException {

        Authentication auth = SecurityContextHolder.getContext()
                                                   .getAuthentication();

        if (Objects.nonNull(auth)) {
            _Log.info("User '%s' attempted to access the protected URL: %s", auth.getName(), httpServletRequest.getRequestURI());
        }
        httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/403");
    }
}
