package com.notehub.service;

import com.notehub.model.vo.user.CheckinStatusVO;

/**
 * 每日打卡: 主键去重, 同一天重复调用幂等
 */
public interface CheckinService {

    CheckinStatusVO checkin(Long userId);

    CheckinStatusVO status(Long userId);
}
