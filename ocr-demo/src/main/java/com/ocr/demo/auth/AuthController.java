package com.ocr.demo.auth;

import com.ocr.common.result.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    private static final Map<String, UserAccount> ACCOUNTS = new HashMap<>();

    static {
        ACCOUNTS.put("admin", new UserAccount("admin", "123456", "U001", "管理员", "T001", "ADMIN"));
        ACCOUNTS.put("user1", new UserAccount("user1", "123456", "U002", "普通用户", "T001", "USER"));
        ACCOUNTS.put("admin2", new UserAccount("admin2", "123456", "U003", "管理员B", "T002", "ADMIN"));
    }

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginRequest request) {
        UserAccount account = ACCOUNTS.get(request.getUsername());
        if (account == null || !account.password.equals(request.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        String token = tokenProvider.generateToken(
                account.userId, account.userName, account.tenantId, account.role);

        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("tenantId", account.tenantId);
        data.put("userId", account.userId);
        data.put("userName", account.userName);
        data.put("role", account.role);

        return R.ok(data);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    record UserAccount(String username, String password, String userId,
                       String userName, String tenantId, String role) {}
}
