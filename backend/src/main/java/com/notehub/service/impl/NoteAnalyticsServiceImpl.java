package com.notehub.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.dto.note.DailyCountRow;
import com.notehub.model.dto.note.NoteTotalsRow;
import com.notehub.model.entity.Note;
import com.notehub.model.enums.RedisKey;
import com.notehub.model.vo.note.HeatmapVO;
import com.notehub.model.vo.note.NoteStatsVO;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.note.RankNoteVO;
import com.notehub.service.NoteAnalyticsService;
import com.notehub.service.RedisService;
import com.notehub.utils.MarkdownUtils;
import com.notehub.utils.StreakUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * I2 数据可视化实现
 * - 热力图: Redis 位图(每天 1 字节)缓存, 命中直接读; 未命中从库聚合回填; 增删笔记走事件增量维护
 * - 热度榜: 窗口内互动数聚合, 结果与用户无关, Redis 缓存 5 分钟
 * - 数据卡片: 计数列聚合 + 连续提交计算
 */
@Log4j2
@Service
public class NoteAnalyticsServiceImpl implements NoteAnalyticsService {

    private static final Duration HEATMAP_TTL = Duration.ofHours(1);
    private static final Duration RANK_TTL = Duration.ofMinutes(5);
    private static final Set<String> PERIODS = Set.of("week", "month", "all");
    private static final int DEFAULT_RANK_LIMIT = 10;
    private static final int MAX_RANK_LIMIT = 50;
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private static final int BITS_PER_DAY = 8;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NoteMapper noteMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public NoteAnalyticsServiceImpl(NoteMapper noteMapper, RedisService redisService,
                                    ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public HeatmapVO getHeatmap(Long userId, Integer year) {
        int targetYear = (year == null || year <= 0) ? LocalDate.now().getYear() : year;
        if (targetYear < MIN_YEAR || targetYear > MAX_YEAR) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "年份超出范围");
        }
        LocalDate start = LocalDate.of(targetYear, 1, 1);
        int daysInYear = start.lengthOfYear();
        String key = RedisKey.heatmap(userId, targetYear);

        byte[] bitmap = readBitmap(key);
        if (bitmap == null) {
            List<DailyCountRow> rows = noteMapper.countDailyByAuthor(
                    userId, start, start.plusYears(1).minusDays(1));
            bitmap = packBitmap(rows, daysInYear);
            writeBitmap(key, bitmap);
        }
        return buildHeatmap(targetYear, bitmap, daysInYear);
    }

    @Override
    public List<RankNoteVO> getRanklist(String period, Integer limit) {
        String normalized = period == null ? "week" : period.trim().toLowerCase();
        if (!PERIODS.contains(normalized)) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "period 仅支持 week/month/all");
        }
        int size = limit == null ? DEFAULT_RANK_LIMIT : Math.min(Math.max(limit, 1), MAX_RANK_LIMIT);
        String key = RedisKey.noteRanklist(normalized, size);

        List<RankNoteVO> cached = readRankCache(key);
        if (cached != null) {
            return cached;
        }

        List<Note> notes = "all".equals(normalized)
                ? noteMapper.selectRanklistAll(size)
                : noteMapper.selectRanklistSince(sinceOf(normalized), size);
        List<RankNoteVO> result = new ArrayList<>(notes.size());
        for (int i = 0; i < notes.size(); i++) {
            result.add(toRankVO(notes.get(i), i + 1));
        }
        writeRankCache(key, result);
        return result;
    }

    @Override
    public NoteStatsVO getTopStats(Long userId) {
        NoteTotalsRow totals = noteMapper.selectNoteTotals(userId);
        if (totals == null) {
            totals = new NoteTotalsRow();
        }
        List<LocalDate> activeDates = noteMapper.selectActiveDates(userId);

        NoteStatsVO vo = new NoteStatsVO();
        vo.setNoteCount(totals.getNoteCount());
        vo.setLikeCount(totals.getLikeCount());
        vo.setCommentCount(totals.getCommentCount());
        vo.setCollectCount(totals.getCollectCount());
        vo.setWordCount(totals.getWordCount());
        vo.setActiveDays(activeDates.size());
        vo.setCurrentStreak(StreakUtils.currentStreak(activeDates));
        vo.setLongestStreak(StreakUtils.longestStreak(activeDates));
        vo.setMonthNoteCount(noteMapper.countNotesSince(userId,
                LocalDate.now().withDayOfMonth(1).atStartOfDay()));
        return vo;
    }

    @Override
    public byte[] exportNotes(Long userId, Long noteId) {
        StringBuilder sb = new StringBuilder();
        if (noteId != null) {
            Note note = noteMapper.selectById(noteId);
            if (note == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
            }
            if (!note.getAuthorId().equals(userId)) {
                throw BusinessException.of(ErrorCode.FORBIDDEN, "只能导出自己的笔记");
            }
            appendNote(sb, note, null);
        } else {
            List<Note> notes = noteMapper.selectAllByAuthor(userId);
            sb.append("# notehub 笔记导出\n\n");
            sb.append("> 共 ").append(notes.size()).append(" 篇 · 导出时间 ")
                    .append(LocalDateTime.now().format(DATE_TIME)).append("\n");
            for (int i = 0; i < notes.size(); i++) {
                appendNote(sb, notes.get(i), i + 1);
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void recordHeatmap(Long userId, LocalDate day, int delta) {
        if (userId == null || day == null || delta == 0) {
            return;
        }
        try {
            // BITFIELD 的 offset 单位是 bit: 每天占 8 bit, 所以是日序号 * 8
            boolean applied = redisService.bitFieldIncrIfPresent(
                    RedisKey.heatmap(userId, day.getYear()),
                    (long) (day.getDayOfYear() - 1) * BITS_PER_DAY, delta);
            if (!applied) {
                log.debug("[热力图] 位图未建立, 跳过增量(下次读取时回填): user={}", userId);
            }
        } catch (Exception e) {
            log.warn("[热力图] 位图增量维护失败(不影响笔记操作): user={}, err={}", userId, e.getMessage());
        }
    }

    // ---------- 热力图缓存 ----------

    private byte[] readBitmap(String key) {
        try {
            return redisService.getBytes(key);
        } catch (Exception e) {
            log.warn("[热力图] 读取位图失败(降级查库): {}", e.getMessage());
            return null;
        }
    }

    private void writeBitmap(String key, byte[] bitmap) {
        try {
            redisService.setBytes(key, bitmap, HEATMAP_TTL);
        } catch (Exception e) {
            log.warn("[热力图] 写位图失败(不影响结果): {}", e.getMessage());
        }
    }

    /** 第 i 天 → byte[i], 直接按日序号铺字节; 超出 255 封顶 */
    private byte[] packBitmap(List<DailyCountRow> rows, int daysInYear) {
        byte[] bitmap = new byte[daysInYear];
        for (DailyCountRow row : rows) {
            int index = row.getDay().getDayOfYear() - 1;
            if (index >= 0 && index < daysInYear) {
                bitmap[index] = (byte) Math.min(row.getDayCount(), 255);
            }
        }
        return bitmap;
    }

    private HeatmapVO buildHeatmap(int year, byte[] bitmap, int daysInYear) {
        List<HeatmapVO.Day> days = new ArrayList<>(daysInYear);
        LocalDate start = LocalDate.of(year, 1, 1);
        long total = 0;
        int activeDays = 0;
        int max = 0;
        for (int i = 0; i < daysInYear; i++) {
            int count = i < bitmap.length ? bitmap[i] & 0xFF : 0;
            days.add(new HeatmapVO.Day(start.plusDays(i).toString(), count));
            total += count;
            if (count > 0) {
                activeDays++;
                max = Math.max(max, count);
            }
        }
        HeatmapVO vo = new HeatmapVO();
        vo.setYear(year);
        vo.setTotal(total);
        vo.setActiveDays(activeDays);
        vo.setMax(max);
        vo.setDays(days);
        return vo;
    }

    // ---------- 热度榜缓存 ----------

    /** 含今天在内的滚动窗口(近 7 天 = 今天及前 6 天) */
    private LocalDate sinceOf(String period) {
        return switch (period) {
            case "week" -> LocalDate.now().minusDays(6);
            case "month" -> LocalDate.now().minusDays(29);
            default -> throw BusinessException.of(ErrorCode.BAD_PARAM, "period 不合法");
        };
    }

    private RankNoteVO toRankVO(Note note, int rank) {
        RankNoteVO vo = new RankNoteVO();
        vo.setRank(rank);
        vo.setNoteId(note.getNoteId());
        vo.setQuestionId(note.getQuestionId());
        vo.setQuestionTitle(note.getQuestionTitle());
        vo.setDifficulty(note.getQuestionDifficulty());
        vo.setExcerpt(MarkdownUtils.excerpt(note.getContent()));

        NoteVO.AuthorVO author = new NoteVO.AuthorVO();
        author.setUserId(note.getAuthorId());
        author.setUsername(note.getAuthorName());
        author.setAvatarUrl(note.getAuthorAvatarUrl());
        vo.setAuthor(author);

        int likes = note.getLikeCount() == null ? 0 : note.getLikeCount();
        int comments = note.getCommentCount() == null ? 0 : note.getCommentCount();
        int collects = note.getCollectCount() == null ? 0 : note.getCollectCount();
        vo.setLikeCount(likes);
        vo.setCommentCount(comments);
        vo.setCollectCount(collects);
        vo.setHotScore((long) likes * 2 + (long) comments * 3 + (long) collects * 2);
        return vo;
    }

    private List<RankNoteVO> readRankCache(String key) {
        try {
            String raw = redisService.get(key);
            if (raw == null) {
                return null;
            }
            return objectMapper.readValue(raw, new TypeReference<List<RankNoteVO>>() {
            });
        } catch (Exception e) {
            log.warn("[热度榜] 读取缓存失败(降级查库): {}", e.getMessage());
            return null;
        }
    }

    private void writeRankCache(String key, List<RankNoteVO> ranklist) {
        try {
            redisService.set(key, objectMapper.writeValueAsString(ranklist), RANK_TTL);
        } catch (Exception e) {
            log.warn("[热度榜] 写缓存失败(不影响结果): {}", e.getMessage());
        }
    }

    // ---------- 导出 ----------

    private void appendNote(StringBuilder sb, Note note, Integer index) {
        sb.append("\n---\n\n");
        String title = note.getQuestionTitle() == null ? "未命名题目" : note.getQuestionTitle();
        if (index == null) {
            sb.append("# ").append(title).append("\n\n");
        } else {
            sb.append("## ").append(index).append(". ").append(title).append("\n\n");
        }
        sb.append("- 难度: ").append(difficultyLabel(note.getQuestionDifficulty())).append('\n');
        sb.append("- 写于: ")
                .append(note.getCreatedAt() == null ? "-" : note.getCreatedAt().format(DATE_TIME))
                .append("\n\n");
        sb.append(note.getContent() == null ? "" : note.getContent().strip()).append('\n');
    }

    private String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return "未知";
        }
        return switch (difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "未知";
        };
    }
}
