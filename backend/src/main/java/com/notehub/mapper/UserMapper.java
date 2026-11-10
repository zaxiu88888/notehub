package com.notehub.mapper;

import com.notehub.model.dto.user.UserQueryParam;
import com.notehub.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    /** 按 ID 查(鉴权/详情) */
    User selectById(@Param("userId") Long userId);

    User selectByAccount(@Param("account") String account);

    User selectByEmail(@Param("email") String email);

    int insert(User user);

    /** 修改个人资料(username/gender/birthday/school/signature 可变, account/email/password 不在此列) */
    int updateProfile(User user);

    int updateLastLoginAt(@Param("userId") Long userId);

    int updateBanned(@Param("userId") Long userId, @Param("isBanned") Integer isBanned);

    /** 管理端: 总数 + 分页列表 */
    long countByQuery(@Param("param") UserQueryParam param);

    List<User> listByQuery(@Param("param") UserQueryParam param, @Param("offset") int offset,
                           @Param("limit") int limit);

    // ---- 统计看板聚合(每日快照) ----
    long countTodayLogin();

    long countTodayRegister();

    long countTotal();

    int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

    /** I5 上传清理: 头像 URL 引用数 */
    long countByAvatarUrl(@Param("url") String url);

    // ---- I1 搜索 ----
    List<User> searchByKeyword(@Param("keyword") String keyword,
                               @Param("offset") int offset, @Param("limit") int limit);

    long countByKeyword(@Param("keyword") String keyword);
}
