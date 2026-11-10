package com.notehub.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CheckinMapper {

    /** 主键(user_id, checkin_date) 去重; 返回 0=今天已打卡 */
    int insertIgnore(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 打卡日期倒序 */
    List<LocalDate> selectDatesDesc(@Param("userId") Long userId);
}
