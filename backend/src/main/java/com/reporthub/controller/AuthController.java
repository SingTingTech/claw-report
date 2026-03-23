package com.reporthub.controller;

import com.reporthub.common.R;
import com.reporthub.entity.User;
import com.reporthub.security.LoginUser;
import com.reporthub.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        try {
            Map<String, Object> result = authService.login(username, password);
            return R.success(result);
        } catch (RuntimeException e) {
            return R.fail(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public R<Void> register(@RequestBody User user) {
        try {
            authService.register(user);
            return R.success(null);
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/me")
    public R<LoginUser> me(@AuthenticationPrincipal LoginUser user) {
        return R.success(user);
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.success(null);
    }
}
