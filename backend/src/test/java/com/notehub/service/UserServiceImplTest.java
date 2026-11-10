package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
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
import com.notehub.security.WsSessionRegistry;
import com.notehub.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final String SECRET = "unit-test-secret-0123456789-unit-test-secret-0123456789";

    private UserMapper userMapper;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private UploadService uploadService;
    private ImageCleanupService imageCleanupService;
    private WsSessionRegistry wsSessionRegistry;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        emailService = mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        uploadService = mock(UploadService.class);
        imageCleanupService = mock(ImageCleanupService.class);
        wsSessionRegistry = mock(WsSessionRegistry.class);
        userService = new UserServiceImpl(userMapper, emailService, passwordEncoder,
                new JwtUtil(SECRET, 3600), uploadService, imageCleanupService, wsSessionRegistry);
    }

    @Test
    @DisplayName("换头像: 更新新 URL 并清理旧头像文件")
    void updateAvatarCleansOldFile() {
        User existing = new User();
        existing.setUserId(7L);
        existing.setAvatarUrl("/images/old.png");
        when(userMapper.selectById(7L)).thenReturn(existing);
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", new byte[]{1, 2, 3});
        when(uploadService.saveImage(file)).thenReturn("/images/new.png");

        String url = userService.updateAvatar(7L, file);

        assertEquals("/images/new.png", url);
        verify(userMapper).updateAvatar(7L, "/images/new.png");
        verify(imageCleanupService).cleanupAvatar("/images/old.png");
    }

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@notehub.dev");
        req.setCode("123456");
        req.setAccount("newbie");
        req.setUsername("新同学");
        req.setPassword("secret123");
        return req;
    }

    @Test
    @DisplayName("注册成功: 验证码消费 + 入库加密 + 签发 token")
    void registerSuccess() {
        RegisterRequest req = registerRequest();
        when(emailService.verifyAndConsumeCode("new@notehub.dev", "123456")).thenReturn(true);
        when(userMapper.selectByAccount("newbie")).thenReturn(null);
        when(userMapper.selectByEmail("new@notehub.dev")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, User.class).setUserId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        LoginUserVO vo = userService.register(req);

        assertEquals(100L, vo.getUserId());
        assertNotNull(vo.getToken());
        assertEquals(100L, new JwtUtil(SECRET, 3600).parseUserId(vo.getToken()));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertNotEquals("secret123", captor.getValue().getPassword()); // 必须是散列
        assertEquals("newbie", captor.getValue().getAccount());
    }

    @Test
    @DisplayName("注册失败: 验证码错误 → CODE_INVALID, 不入库")
    void registerWithWrongCode() {
        RegisterRequest req = registerRequest();
        when(emailService.verifyAndConsumeCode("new@notehub.dev", "123456")).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> userService.register(req));
        assertEquals(ErrorCode.CODE_INVALID, e.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败: 账号重复 → DUPLICATED")
    void registerWithDuplicatedAccount() {
        RegisterRequest req = registerRequest();
        when(emailService.verifyAndConsumeCode("new@notehub.dev", "123456")).thenReturn(true);
        when(userMapper.selectByAccount("newbie")).thenReturn(new User());

        BusinessException e = assertThrows(BusinessException.class, () -> userService.register(req));
        assertEquals(ErrorCode.DUPLICATED, e.getErrorCode());
    }

    @Test
    @DisplayName("注册失败: 邮箱重复 → DUPLICATED")
    void registerWithDuplicatedEmail() {
        RegisterRequest req = registerRequest();
        when(emailService.verifyAndConsumeCode("new@notehub.dev", "123456")).thenReturn(true);
        when(userMapper.selectByAccount("newbie")).thenReturn(null);
        when(userMapper.selectByEmail("new@notehub.dev")).thenReturn(new User());

        BusinessException e = assertThrows(BusinessException.class, () -> userService.register(req));
        assertEquals(ErrorCode.DUPLICATED, e.getErrorCode());
    }

    private User persistedUser() {
        User user = new User();
        user.setUserId(7L);
        user.setAccount("demo");
        user.setUsername("刷题小白");
        user.setPassword(passwordEncoder.encode("user123456"));
        user.setEmail("demo@notehub.dev");
        user.setIsBanned(0);
        user.setIsAdmin(0);
        return user;
    }

    @Test
    @DisplayName("登录成功: 密码匹配 → token, 并刷新 last_login_at")
    void loginSuccess() {
        when(userMapper.selectByAccount("demo")).thenReturn(persistedUser());

        LoginRequest req = new LoginRequest();
        req.setAccount("demo");
        req.setPassword("user123456");
        LoginUserVO vo = userService.login(req);

        assertEquals(7L, vo.getUserId());
        verify(userMapper).updateLastLoginAt(7L);
    }

    @Test
    @DisplayName("登录失败: 密码错误 → LOGIN_FAILED(不泄露账号是否存在)")
    void loginWrongPassword() {
        when(userMapper.selectByAccount("demo")).thenReturn(persistedUser());
        when(userMapper.selectByAccount("ghost")).thenReturn(null);

        LoginRequest wrongPassword = new LoginRequest();
        wrongPassword.setAccount("demo");
        wrongPassword.setPassword("bad-password");
        BusinessException e1 = assertThrows(BusinessException.class, () -> userService.login(wrongPassword));
        assertEquals(ErrorCode.LOGIN_FAILED, e1.getErrorCode());

        LoginRequest noSuchUser = new LoginRequest();
        noSuchUser.setAccount("ghost");
        noSuchUser.setPassword("whatever");
        BusinessException e2 = assertThrows(BusinessException.class, () -> userService.login(noSuchUser));
        assertEquals(ErrorCode.LOGIN_FAILED, e2.getErrorCode());
        assertEquals(e1.getMessage(), e2.getMessage());
    }

    @Test
    @DisplayName("登录失败: 封禁账号 → FORBIDDEN")
    void loginBanned() {
        User banned = persistedUser();
        banned.setIsBanned(1);
        when(userMapper.selectByAccount("demo")).thenReturn(banned);

        LoginRequest req = new LoginRequest();
        req.setAccount("demo");
        req.setPassword("user123456");
        BusinessException e = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(ErrorCode.FORBIDDEN, e.getErrorCode());
    }

    @Test
    @DisplayName("whoami 返回全量(含 email); 公开主页不泄露 account/email")
    void profileVisibility() {
        when(userMapper.selectById(7L)).thenReturn(persistedUser());

        UserVO mine = userService.whoami(7L);
        assertEquals("demo@notehub.dev", mine.getEmail());

        UserVO pub = userService.getPublicProfile(7L);
        assertNull(pub.getAccount());
        assertNull(pub.getEmail());
        assertEquals("刷题小白", pub.getUsername()); // 公开字段正常返回
    }

    @Test
    @DisplayName("管理端列表: 分页参数收敛(1~50)且返回 PageVO")
    void listUsersPageBounds() {
        when(userMapper.countByQuery(any())).thenReturn(3L);
        when(userMapper.listByQuery(any(), anyInt(), anyInt())).thenReturn(List.of(new User()));

        UserQueryParam param = new UserQueryParam();
        param.setPage(0);      // 非法页码 → 收敛为 1
        param.setPageSize(500); // 超大页大小 → 收敛为 50
        PageVO<UserVO> page = userService.listUsers(param);

        assertEquals(1, page.getPage());
        assertEquals(50, page.getPageSize());
        assertEquals(3, page.getTotal());
    }

    @Test
    @DisplayName("管理端封禁: 更新库并断开已有 WS; 解封不断开")
    void updateBanned() {
        when(userMapper.selectById(7L)).thenReturn(persistedUser());
        userService.updateBanned(7L, 1);
        verify(userMapper).updateBanned(eq(7L), eq(1));
        verify(wsSessionRegistry).disconnectUser(7L);

        userService.updateBanned(7L, 0);
        verify(wsSessionRegistry, times(1)).disconnectUser(7L);

        when(userMapper.selectById(999L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.updateBanned(999L, 1));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
        verify(wsSessionRegistry, never()).disconnectUser(999L);
    }

    @Test
    @DisplayName("资料更新: 仅复制可变字段入库, 不触碰密码; 返回重读后的信息")
    void updateProfile() {
        when(userMapper.selectById(7L)).thenReturn(persistedUser());

        UpdateUserRequest req = new UpdateUserRequest();
        req.setUsername("新昵称");
        req.setSignature("坚持刷题");
        UserVO vo = userService.updateProfile(7L, req);

        // mock 环境下 whoami 重读的是原数据 → 断言"入库对象"与"返回主体"即可
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateProfile(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("新昵称", captor.getValue().getUsername());
        assertEquals("坚持刷题", captor.getValue().getSignature());
        assertNull(captor.getValue().getPassword()); // 资料更新绝不动密码
        assertEquals(7L, vo.getUserId());
    }
}
