package com.notehub.mapper;

import com.notehub.model.entity.Statistic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StatisticMapper {

    /** 最近 N 条(趋势曲线), 按日期升序 */
    List<Statistic> listRecent(@Param("limit") int limit);

    Statistic selectByDate(@Param("date") LocalDate date);

    /** 按日期 upsert(uk_date 幂等, 同日重跑覆盖) */
    int upsert(Statistic statistic);
}
