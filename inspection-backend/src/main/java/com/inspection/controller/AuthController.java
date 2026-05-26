package com.inspection.controller;

import com.inspection.common.exception.BusinessException;
import com.inspection.common.result.ApiResponse;
import com.inspection.common.utils.JwtUtil;
import com.inspection.entity.SysUser;
import com.inspection.service.SysUserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, SysUserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginRequest req) throws BusinessException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        if (!authentication.isAuthenticated()) {
            throw new BusinessException("用户名或密码错误");
        }
        SysUser user = userService.lambdaQuery().eq(SysUser::getUsername, req.getUsername()).one();
        if (user == null) {
            throw new BusinessException("用户不存在或已禁用");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return ApiResponse.ok("登录成功", data);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ApiResponse.fail("未登录或Token已失效");
        }
        String username = (String) auth.getPrincipal();
        SysUser user = userService.lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) {
            return ApiResponse.fail("用户不存在或已禁用");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return ApiResponse.ok(data);
    }

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
