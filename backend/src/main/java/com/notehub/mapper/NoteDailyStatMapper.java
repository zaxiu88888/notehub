package com.notehub.mapper;

import com.notehub.model.entity.NoteDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NoteDailyStatMapper {

    int adjustLike(@Param("noteId") Long noteId, @Param("statDate") LocalDate statDate,
                   @Param("delta") int delta);

    int adjustComment(@Param("noteId") Long noteId, @Param("statDate") LocalDate statDate,
                      @Param("delta") int delta);

    int adjustCollect(@Param("noteId") Long noteId, @Param("statDate") LocalDate statDate,
                      @Param("delta") int delta);

    List<NoteDailyStat> selectSince(@Param("noteId") Long noteId, @Param("since") LocalDate since);

    int deleteByNoteId(@Param("noteId") Long noteId);
}
