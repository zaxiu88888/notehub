package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.mapper.UserMapper;
import com.notehub.model.dto.user.LoginRequest;
import com.notehub.model.dto.user.RegisterRequest;
import com.notehub.model.dto.user.UpdateUserRequest;
import com.notehub.model.dto.user.UserQueryParam;
import com.notehub.model.entity.User;
import com.notehub.model.vo.user.LoginUserVO;
import com.notehub.model.vo.user.UserVO;
import com.notehub.security.JwtUtil;
import com.notehub.service.EmailService;
import com.notehub.service.ImageCleanupService;
import com.notehub.service.UploadService;
import com.notehub.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UploadService uploadService;
    private final ImageCleanupService imageCleanupService;

    public UserServiceImpl(UserMapper userMapper, EmailService emailService,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           UploadService uploadService, ImageCleanupService imageCleanupService) {
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.uploadService = uploadService;
        this.imageCleanupService = imageCleanupService;
    }

    @Override
    public LoginUserVO register(RegisterRequest request) {
        if (!emailService.verifyAndConsumeCode(request.getEmail(), request.getCode())) {
            throw BusinessException.of(ErrorCode.CODE_INVALID);
        }
        if (userMapper.selectByAccount(request.getAccount()) != null) {
            throw BusinessException.of(ErrorCode.DUPLICATED, "账号已被注册");
        }
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw BusinessException.of(ErrorCode.DUPLICATED, "邮箱已被注册");
        }

        User user = new User();
        user.setAccount(request.getAccount());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);

        log.info("[用户] 注册成功: userId={}, account={}", user.getUserId(), user.getAccount());
        return new LoginUserVO(user.getUserId(), jwtUtil.generateToken(user.getUserId()));
    }

    @Override
    public LoginUserVO login(LoginRequest request) {
        User user = userMapper.selectByAccount(request.getAccount());
        // 用户不存在与密码错误同一文案, 不泄露账号是否存在
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.of(ErrorCode.LOGIN_FAILED);
        }
        if (user.getIsBanned() != null && user.getIsBanned() == 1) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被封禁, 如有疑问请联系管理员");
        }
        userMapper.updateLastLoginAt(user.getUserId());
        return new LoginUserVO(user.getUserId(), jwtUtil.generateToken(user.getUserId()));
    }

    @Override
    public UserVO whoami(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND);
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo); // 含 account/email: 本人视角
        return vo;
    }

    @Override
    public UserVO getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND);
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setAccount(null); // 公开视图不泄露账号与邮箱
        vo.setEmail(null);
        return vo;
    }

    @Override
    public UserVO updateProfile(Long userId, UpdateUserRequest request) {
        // 全空请求直接返回当前信息, 避免 MyBatis <set> 生成空片段导致 SQL 语法错误
        boolean allEmpty = request.getUsername() == null && request.getGender() == null
                && request.getBirthday() == null && request.getSchool() == null
                && request.getSignature() == null;
        if (!allEmpty) {
            User user = new User();
            user.setUserId(userId);
            BeanUtils.copyProperties(request, user);
            userMapper.updateProfile(user);
        }
        return whoami(userId);
    }

    @Override
    public PageVO<UserVO> listUsers(UserQueryParam param) {
        int pageSize = PageUtils.clampPageSize(param.getPageSize());
        int page = PageUtils.clampPage(param.getPage());
        param.setPageSize(pageSize);
        param.setPage(page);

        long total = userMapper.countByQuery(param);
        int offset = PageUtils.offset(page, pageSize);
        List<UserVO> list = userMapper.listByQuery(param, offset, pageSize).stream()
                .map(this::toVO)
                .toList();
        return PageVO.of(list, page, pageSize, total);
    }

    @Override
    public void updateBanned(Long userId, Integer isBanned) {
        if (userMapper.selectById(userId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND);
        }
        userMapper.updateBanned(userId, isBanned);
        log.info("[管理] 用户封禁状态变更: userId={}, isBanned={}", userId, isBanned);
    }

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND);
        }
        String url = uploadService.saveImage(file);
        userMapper.updateAvatar(userId, url);
        // 旧头像无人引用才删(换新头像后, 该 URL 应从 users 中消失)
        imageCleanupService.cleanupAvatar(existing.getAvatarUrl());
        return url;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setEmail(null); // 管理端列表也不展示邮箱原文
        return vo;
    }
}
