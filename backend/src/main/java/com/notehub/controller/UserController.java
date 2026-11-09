package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.user.LoginRequest;
import com.notehub.model.dto.user.RegisterRequest;
import com.notehub.model.dto.user.UpdateUserRequest;
import com.notehub.model.dto.user.UserQueryParam;
import com.notehub.model.vo.user.LoginUserVO;
import com.notehub.model.vo.user.UserVO;
import com.notehub.security.NeedAdmin;
import com.notehub.security.NeedLogin;
import com.notehub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户 /api/users, 管理端 /api/admin/users
 */
@RestController
@RequestMapping("/api")
@Validated
public class UserController {

    private final UserService userService;
    private final RequestContext requestContext;

    public UserController(UserService userService, RequestContext requestContext) {
        this.userService = userService;
        this.requestContext = requestContext;
    }

    @PostMapping("/users")
    public ApiResponse<LoginUserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", userService.register(request));
    }

    @PostMapping("/users/login")
    public ApiResponse<LoginUserVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", userService.login(request));
    }

    @PostMapping("/users/whoami")
    @NeedLogin
    public ApiResponse<UserVO> whoami() {
        return ApiResponse.ok(userService.whoami(requestContext.requireUserId()));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserVO> profile(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getPublicProfile(userId));
    }

    @PatchMapping("/users/me")
    @NeedLogin
    public ApiResponse<UserVO> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("资料已更新", userService.updateProfile(requestContext.requireUserId(), request));
    }

    @GetMapping("/admin/users")
    @NeedAdmin
    public ApiResponse<PageVO<UserVO>> listUsers(UserQueryParam param) {
        return ApiResponse.ok(userService.listUsers(param));
    }

    @PatchMapping("/admin/users/{userId}/ban")
    @NeedAdmin
    public ApiResponse<Void> ban(@PathVariable Long userId, @Valid @RequestBody BanRequest body) {
        userService.updateBanned(userId, body.getIsBanned());
        return ApiResponse.ok();
    }

    /** 上传头像: multipart file, 返回新头像地址 */
    @PostMapping("/users/avatar")
    @NeedLogin
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String url = userService.updateAvatar(requestContext.requireUserId(), file);
        return ApiResponse.ok("头像已更新", Map.of("avatarUrl", url));
    }

    /** 封禁请求体 */
    @Data
    public static class BanRequest {
        @NotNull(message = "isBanned 不能为空")
        @Min(value = 0, message = "isBanned 取值 0/1")
        @Max(value = 1, message = "isBanned 取值 0/1")
        private Integer isBanned;
    }
}
