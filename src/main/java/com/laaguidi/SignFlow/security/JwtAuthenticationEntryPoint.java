package com.laaguidi.SignFlow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 🔍 التحقق واش المستخدِم دوز الـ Token بنجاح وراه Authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("path", request.getRequestURI());

        // 1️⃣ حالة: السيد Authenticated ولكن وصل لهنا حيت المسار/Endpoint ما كاينش كاع
        if (isAuthenticated) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404

            body.put("status", HttpServletResponse.SC_NOT_FOUND);
            body.put("error", "Not Found");
            body.put("code", "ENDPOINT_NOT_FOUND");
            body.put("message", "The requested endpoint " + request.getRequestURI() + " does not exist.");
        }
        // 2️⃣ حالة: السيد ما عندهومش Token صحيحة ولا ما ملوكيش أصلاً
        else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

            String jwtError = (String) request.getAttribute("jwt_error");
            if (jwtError == null) {
                jwtError = "UNAUTHORIZED_ACCESS";
            }

            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("error", "Unauthorized");
            body.put("code", jwtError);
            body.put("message", getCustomMessage(jwtError));
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }

    private String getCustomMessage(String code) {
        return switch (code) {
            case "TOKEN_EXPIRED" -> "Access token has expired. Please refresh your token.";
            case "TOKEN_INVALID" -> "Invalid or tampered JWT token.";
            case "USER_NOT_FOUND" -> "User associated with this token no longer exists.";
            default -> "Authentication is required to access this resource.";
        };
    }
}