package com.notehub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.PageVO;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.User;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.user.UserVO;
import com.notehub.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceImplTest {

    private NoteMapper noteMapper;
    private UserMapper userMapper;
    private NoteService noteService;
    private RedisService redisService;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        userMapper = mock(UserMapper.class);
        noteService = mock(NoteService.class);
        redisService = mock(RedisService.class);
        service = new SearchServiceImpl(noteMapper, userMapper, noteService, redisService,
                new ObjectMapper());
    }

    private Note note(long id) {
        Note n = new Note();
        n.setNoteId(id);
        n.setAuthorId(2L);
        n.setQuestionId(2L);
        n.setContent("哈希表题解");
        return n;
    }

    private NoteVO vo(long id) {
        NoteVO v = new NoteVO();
        v.setNoteId(id);
        return v;
    }

    @Test
    @DisplayName("空关键词: 直接空页, 不查库不写缓存")
    void blankKeywordShortCircuits() {
        PageVO<NoteVO> page = service.searchNotes("   ", 1, 10, 2L);

        assertEquals(0, page.getTotal());
        verify(noteMapper, never()).searchByKeyword(anyString(), anyInt(), anyInt());
        verify(redisService, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("缓存未命中: 查 count+search, 写缓存(ID 与总数), 组装 VO")
    void cacheMissQueriesDb() {
        when(redisService.get(anyString())).thenReturn(null);
        when(noteMapper.countByKeyword(anyString())).thenReturn(1L);
        when(noteMapper.searchByKeyword(anyString(), eq(0), eq(10))).thenReturn(List.of(note(5L)));
        when(noteMapper.selectByIds(List.of(5L))).thenReturn(List.of(note(5L)));
        when(noteService.toVOList(anyList(), eq(2L))).thenReturn(List.of(vo(5L)));

        PageVO<NoteVO> page = service.searchNotes("哈希表", 1, 10, 2L);

        assertEquals(1, page.getTotal());
        assertEquals(5L, page.getList().get(0).getNoteId());
        verify(redisService).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("缓存命中: 不重复查 count/search, 仍按当前用户组装互动状态")
    void cacheHitSkipsDbSearch() {
        String json = "{\"ids\":[5],\"total\":1}";
        when(redisService.get(anyString())).thenReturn(json);
        when(noteMapper.selectByIds(List.of(5L))).thenReturn(List.of(note(5L)));
        when(noteService.toVOList(anyList(), eq(9L))).thenReturn(List.of(vo(5L)));

        PageVO<NoteVO> page = service.searchNotes("哈希表", 1, 10, 9L);

        assertEquals(1, page.getTotal());
        verify(noteMapper, never()).searchByKeyword(anyString(), anyInt(), anyInt());
        verify(noteMapper, never()).countByKeyword(anyString());
        verify(noteService).toVOList(anyList(), eq(9L));
    }

    @Test
    @DisplayName("用户搜索: 邮箱脱敏 + 分页钳制")
    void searchUsersMasksEmail() {
        User u = new User();
        u.setUserId(2L);
        u.setUsername("刷题小白");
        u.setAccount("demo");
        u.setEmail("demo@notehub.dev");
        when(userMapper.countByKeyword("demo")).thenReturn(1L);
        when(userMapper.searchByKeyword(eq("demo"), eq(0), eq(50))).thenReturn(List.of(u));

        PageVO<UserVO> page = service.searchUsers("demo", 0, 999);

        assertEquals(1, page.getPage());
        assertEquals(50, page.getPageSize());
        assertEquals(null, page.getList().get(0).getEmail());
        assertTrue(page.getList().get(0).getUsername().contains("刷题"));
    }
}
