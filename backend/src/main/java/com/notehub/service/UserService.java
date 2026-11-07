package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.dto.user.LoginRequest;
import com.notehub.model.dto.user.RegisterRequest;
import com.notehub.model.dto.user.UpdateUserRequest;
import com.notehub.model.dto.user.UserQueryParam;
import com.notehub.model.vo.user.LoginUserVO;
import com.notehub.model.vo.user.UserVO;

/**
 * 用户服务
 */
public interface UserService {

    /** 注册: 验证码校验(一次性) + 唯一性校验 + BCrypt 入库 + 签发 token */
    LoginUserVO register(RegisterRequest request);

    /** 登录: BCrypt 校验 + 封禁拦截 + 刷新 last_login_at + 签发 token */
    LoginUserVO login(LoginRequest request);

    /** 当前登录用户全量信息 */
    UserVO whoami(Long userId);

    /** 他人主页: 仅公开字段(email/account 不回填) */
    UserVO getPublicProfile(Long userId);

    /** 修改个人资料 */
    UserVO updateProfile(Long userId, UpdateUserRequest request);

    /** 管理端: 用户分页列表 */
    PageVO<UserVO> listUsers(UserQueryParam param);

    /** 管理端: 封禁/解封 */
    void updateBanned(Long userId, Integer isBanned);
}
