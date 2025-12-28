package com.moviesp.builder.config;

import com.moviesp.builder.entities.UserEntity;
import com.moviesp.builder.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getMethod().equals("OPTIONS"))
            return true;
        if (request.getRequestURI().startsWith("/api/auth"))
            return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Optional<UserEntity> user = userService.findByToken(token);
            if (user.isPresent()) {
                request.setAttribute("user", user.get());
                return true;
            }
        }

        response.setStatus(401);
        return false;
    }
}
