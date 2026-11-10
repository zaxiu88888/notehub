package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteDailyStatMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.NoteDailyStat;
import com.notehub.model.vo.note.NoteDailyStatVO;
import com.notehub.service.NoteDailyStatService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NoteDailyStatServiceImpl implements NoteDailyStatService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 30;

    private final NoteDailyStatMapper statMapper;
    private final NoteMapper noteMapper;

    public NoteDailyStatServiceImpl(NoteDailyStatMapper statMapper, NoteMapper noteMapper) {
        this.statMapper = statMapper;
        this.noteMapper = noteMapper;
    }

    @Override
    public void recordLike(Long noteId, LocalDate date, int delta) {
        if (delta != 0) {
            statMapper.adjustLike(noteId, date, delta);
        }
    }

    @Override
    public void recordComment(Long noteId, LocalDate date, int delta) {
        if (delta != 0) {
            statMapper.adjustComment(noteId, date, delta);
        }
    }

    @Override
    public void recordCollect(Long noteId, LocalDate date, int delta) {
        if (delta != 0) {
            statMapper.adjustCollect(noteId, date, delta);
        }
    }

    @Override
    public List<NoteDailyStatVO> recent(Long noteId, Integer days) {
        if (noteMapper.selectById(noteId) == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        int size = days == null ? DEFAULT_DAYS : Math.min(Math.max(days, 1), MAX_DAYS);
        LocalDate since = LocalDate.now().minusDays(size - 1L);
        return statMapper.selectSince(noteId, since).stream().map(stat -> {
            NoteDailyStatVO vo = new NoteDailyStatVO();
            vo.setDate(stat.getStatDate().toString());
            vo.setLikeCount(stat.getLikeCount() == null ? 0 : stat.getLikeCount());
            vo.setCommentCount(stat.getCommentCount() == null ? 0 : stat.getCommentCount());
            vo.setCollectCount(stat.getCollectCount() == null ? 0 : stat.getCollectCount());
            return vo;
        }).toList();
    }

    @Override
    public void deleteByNoteId(Long noteId) {
        statMapper.deleteByNoteId(noteId);
    }
}
