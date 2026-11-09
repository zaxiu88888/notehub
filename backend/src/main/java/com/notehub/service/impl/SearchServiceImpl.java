package com.notehub.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.User;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.user.UserVO;
import com.notehub.service.NoteService;
import com.notehub.service.RedisService;
import com.notehub.service.SearchService;
import com.notehub.utils.SearchUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 搜索实现
 * - 笔记: Jieba 预处理 → MATCH AGAINST(ngram) → 缓存"笔记ID列表+总数"(与用户无关), VO 每次按当前用户组装
 * - 用户: 昵称/账号 LIKE(规模小, 不缓存)
 */
@Log4j2
@Service
public class SearchServiceImpl implements SearchService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String NOTE_CACHE_KEY = "search:notes:%d:%d:%d";

    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final NoteService noteService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public SearchServiceImpl(NoteMapper noteMapper, UserMapper userMapper, NoteService noteService,
                             RedisService redisService, ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
        this.noteService = noteService;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageVO<NoteVO> searchNotes(String keyword, int page, int pageSize, Long currentUserId) {
        int size = PageUtils.clampPageSize(pageSize);
        int current = PageUtils.clampPage(page);
        String processed = SearchUtils.preprocessKeyword(keyword);
        if (processed.isEmpty()) {
            return PageVO.of(List.of(), current, size, 0);
        }

        CachePayload payload = readCache(processed, current, size);
        if (payload == null) {
            long total = noteMapper.countByKeyword(processed);
            List<Long> ids = total == 0 ? List.of()
                    : noteMapper.searchByKeyword(processed, PageUtils.offset(current, size), size)
                            .stream().map(Note::getNoteId).toList();
            payload = new CachePayload(ids, total);
            writeCache(processed, current, size, payload);
        }

        if (payload.ids.isEmpty()) {
            return PageVO.of(List.of(), current, size, payload.total);
        }
        List<Note> notes = noteMapper.selectByIds(payload.ids);
        List<NoteVO> vos = noteService.toVOList(notes, currentUserId);
        return PageVO.of(vos, current, size, payload.total);
    }

    @Override
    public PageVO<UserVO> searchUsers(String keyword, int page, int pageSize) {
        int size = PageUtils.clampPageSize(pageSize);
        int current = PageUtils.clampPage(page);
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return PageVO.of(List.of(), current, size, 0);
        }
        long total = userMapper.countByKeyword(trimmed);
        List<UserVO> list = userMapper.searchByKeyword(
                        trimmed, PageUtils.offset(current, size), size)
                .stream().map(this::toVO).toList();
        return PageVO.of(list, current, size, total);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setEmail(null); // 搜索结果不暴露邮箱
        return vo;
    }

    private CachePayload readCache(String keyword, int page, int size) {
        try {
            String raw = redisService.get(cacheKey(keyword, page, size));
            if (raw == null) {
                return null;
            }
            return objectMapper.readValue(raw, new TypeReference<CachePayload>() {
            });
        } catch (Exception e) {
            log.warn("[搜索] 读取缓存失败(降级查库): {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String keyword, int page, int size, CachePayload payload) {
        try {
            redisService.set(cacheKey(keyword, page, size),
                    objectMapper.writeValueAsString(payload), CACHE_TTL);
        } catch (Exception e) {
            log.warn("[搜索] 写缓存失败(不影响结果): {}", e.getMessage());
        }
    }

    private String cacheKey(String keyword, int page, int size) {
        return String.format(NOTE_CACHE_KEY, keyword.hashCode(), page, size);
    }

    /** 缓存内容: 只存与用户无关的 ID 与总数 */
    public static class CachePayload {
        public List<Long> ids = List.of();
        public long total;

        public CachePayload() {
        }

        public CachePayload(List<Long> ids, long total) {
            this.ids = ids;
            this.total = total;
        }
    }
}
