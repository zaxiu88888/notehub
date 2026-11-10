package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageVO;
import com.notehub.event.NoteHeatmapEvent;
import com.notehub.mapper.CollectionNoteMapper;
import com.notehub.mapper.CommentMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.entity.Note;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.service.impl.NoteServiceImpl;
import com.notehub.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteServiceImplTest {

    private NoteMapper noteMapper;
    private QuestionMapper questionMapper;
    private TagService tagService;
    private NoteDailyStatService noteDailyStatService;
    private ImageCleanupService imageCleanupService;
    private ApplicationEventPublisher eventPublisher;
    private NoteServiceImpl noteService;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        questionMapper = mock(QuestionMapper.class);
        tagService = mock(TagService.class);
        when(tagService.tagsByNoteIds(any())).thenReturn(Map.of());
        noteDailyStatService = mock(NoteDailyStatService.class);
        imageCleanupService = mock(ImageCleanupService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        noteService = new NoteServiceImpl(noteMapper, mock(CommentMapper.class),
                mock(CollectionNoteMapper.class), questionMapper, tagService,
                noteDailyStatService, imageCleanupService, eventPublisher);
    }

    private Note note(long id, long authorId) {
        Note n = new Note();
        n.setNoteId(id);
        n.setAuthorId(authorId);
        n.setQuestionId(2L);
        n.setContent("# 两数之和题解\n哈希表一次遍历。");
        n.setCreatedAt(java.time.LocalDateTime.now());
        n.setLikeCount(3);
        n.setCommentCount(1);
        n.setCollectCount(2);
        n.setAuthorName("刷题小白");
        n.setQuestionTitle("两数之和");
        n.setQuestionDifficulty(1);
        return n;
    }

    @Test
    @DisplayName("聚合列表-登录: 批量互动状态填充(myActions 精确)")
    void listNotesLoggedInAggregatesMyActions() {
        when(noteMapper.countByQuery(any())).thenReturn(1L);
        when(noteMapper.listByQuery(any(), anyInt(), anyInt())).thenReturn(List.of(note(10L, 2L)));
        when(noteMapper.selectLikedNoteIds(eq(7L), anyList())).thenReturn(List.of(10L));
        when(noteMapper.selectCollectedNoteIds(eq(7L), anyList())).thenReturn(List.of());

        NoteQueryParams params = new NoteQueryParams();
        PageVO<NoteVO> page = noteService.listNotes(params, 7L);

        NoteVO vo = page.getList().get(0);
        assertTrue(vo.getMyActions().isLiked());
        assertFalse(vo.getMyActions().isCollected());
        assertEquals("刷题小白", vo.getAuthor().getUsername());
        assertEquals("两数之和", vo.getQuestion().getTitle());
        // 防 N+1: 每种批量查询只调一次
        verify(noteMapper).selectLikedNoteIds(eq(7L), anyList());
        verify(noteMapper).selectCollectedNoteIds(eq(7L), anyList());
    }

    @Test
    @DisplayName("聚合列表-未登录: myActions 恒 false 且零互动查询")
    void listNotesAnonymousSkipsInteractionQueries() {
        when(noteMapper.countByQuery(any())).thenReturn(1L);
        when(noteMapper.listByQuery(any(), anyInt(), anyInt())).thenReturn(List.of(note(10L, 2L)));

        PageVO<NoteVO> page = noteService.listNotes(new NoteQueryParams(), null);

        assertFalse(page.getList().get(0).getMyActions().isLiked());
        verify(noteMapper, never()).selectLikedNoteIds(any(), anyList());
        verify(noteMapper, never()).selectCollectedNoteIds(any(), anyList());
    }

    @Test
    @DisplayName("发布: 一题一笔记, 重复写返回 DUPLICATED; 题目不存在 NOT_FOUND")
    void createNoteOncePerQuestion() {
        when(questionMapper.selectById(2L)).thenReturn(new com.notehub.model.entity.Question());
        when(noteMapper.selectByAuthorAndQuestion(7L, 2L)).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, Note.class).setNoteId(100L);
            return 1;
        }).when(noteMapper).insert(any(Note.class));

        Long noteId = noteService.createNote(7L, 2L, "内容");
        assertEquals(100L, noteId);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NoteHeatmapEvent heatmapEvent = (NoteHeatmapEvent) eventCaptor.getValue();
        assertEquals(7L, heatmapEvent.authorId());
        assertEquals(LocalDate.now(), heatmapEvent.day());
        assertEquals(1, heatmapEvent.delta());
        // 未传标签 → 继承题目标签(syncNoteTags 收到 null)
        verify(tagService).syncNoteTags(100L, 2L, null);

        when(noteMapper.selectByAuthorAndQuestion(7L, 2L)).thenReturn(note(10L, 7L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> noteService.createNote(7L, 2L, "再写一篇"));
        assertEquals(ErrorCode.DUPLICATED, e.getErrorCode());

        // 题目不存在: 不进入重复校验, 直接 404
        when(questionMapper.selectById(999L)).thenReturn(null);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> noteService.createNote(7L, 999L, "幽灵题"));
        assertEquals(ErrorCode.NOT_FOUND, e2.getErrorCode());
        verify(noteMapper, times(1)).insert(any(Note.class)); // 只有首次成功那次
    }

    @Test
    @DisplayName("编辑/删除: 非作者 → FORBIDDEN")
    void updateDeleteOnlyByAuthor() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));

        BusinessException e1 = assertThrows(BusinessException.class,
                () -> noteService.updateNote(10L, 99L, "x"));
        assertEquals(ErrorCode.FORBIDDEN, e1.getErrorCode());

        BusinessException e2 = assertThrows(BusinessException.class,
                () -> noteService.deleteNote(10L, 99L));
        assertEquals(ErrorCode.FORBIDDEN, e2.getErrorCode());
        verify(noteMapper, never()).deleteById(10L);
    }

    @Test
    @DisplayName("编辑: 清理正文中被移除的图片")
    void updateCleansRemovedImages() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));

        noteService.updateNote(10L, 2L, "新内容");

        verify(imageCleanupService).cleanupRemovedImages(
                "# 两数之和题解\n哈希表一次遍历。", "新内容");
    }

    @Test
    @DisplayName("删除(作者): 连带软删评论 + 清空收藏夹条目")
    void deleteNoteCascades() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));

        noteService.deleteNote(10L, 2L);

        ArgumentCaptor<Long> noteIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(noteMapper).deleteById(noteIdCaptor.capture());
        assertEquals(10L, noteIdCaptor.getValue());
        verify(tagService).deleteNoteTags(10L);
        verify(noteDailyStatService).deleteByNoteId(10L);
        verify(imageCleanupService).cleanupNoteImages("# 两数之和题解\n哈希表一次遍历。");
    }

    @Test
    @DisplayName("按 ID 列表分页(收藏夹内容复用): 批量查(防N+1) + 保持收藏顺序 + 越界页空")
    void listNotesByIdsPagination() {
        PageVO<NoteVO> empty = noteService.listNotesByIds(List.of(), 1, 10, null);
        assertEquals(0, empty.getTotal());

        when(noteMapper.selectByIds(List.of(10L, 11L))).thenReturn(List.of(note(11L, 2L), note(10L, 2L)));
        PageVO<NoteVO> page = noteService.listNotesByIds(List.of(10L, 11L, 12L), 1, 2, null);
        assertEquals(3, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals(10L, page.getList().get(0).getNoteId()); // 按传入顺序还原
        assertEquals(11L, page.getList().get(1).getNoteId());
        verify(noteMapper, times(1)).selectByIds(anyList()); // 一条批量, 无逐条查询
        verify(noteMapper, never()).selectById(org.mockito.ArgumentMatchers.anyLong());

        // 越界页: 不查库
        PageVO<NoteVO> beyond = noteService.listNotesByIds(List.of(10L), 5, 10, null);
        assertEquals(0, beyond.getList().size());
    }

    @Test
    @DisplayName("myNote: 无笔记返回 null")
    void findMyNoteId() {
        when(noteMapper.selectByAuthorAndQuestion(7L, 2L)).thenReturn(null);
        assertNull(noteService.findMyNoteId(7L, 2L));
    }
}
