package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.CollectionMapper;
import com.notehub.mapper.CollectionNoteMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Collection;
import com.notehub.model.entity.Note;
import com.notehub.service.impl.CollectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionServiceImplTest {

    private CollectionMapper collectionMapper;
    private CollectionNoteMapper collectionNoteMapper;
    private NoteMapper noteMapper;
    private NoteService noteService;
    private NoteDailyStatService noteDailyStatService;
    private CollectionServiceImpl service;

    @BeforeEach
    void setUp() {
        collectionMapper = mock(CollectionMapper.class);
        collectionNoteMapper = mock(CollectionNoteMapper.class);
        noteMapper = mock(NoteMapper.class);
        noteService = mock(NoteService.class);
        noteDailyStatService = mock(NoteDailyStatService.class);
        service = new CollectionServiceImpl(collectionMapper, collectionNoteMapper, noteMapper,
                noteService, noteDailyStatService);
    }

    private Collection collection(long id, long creatorId) {
        Collection c = new Collection();
        c.setCollectionId(id);
        c.setCreatorId(creatorId);
        c.setName("我的收藏");
        return c;
    }

    @Test
    @DisplayName("收藏夹归属: 他人收藏夹 → FORBIDDEN")
    void foreignCollectionForbidden() {
        when(collectionMapper.selectById(5L)).thenReturn(collection(5L, 2L)); // demo 的

        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.collectNotes(7L, 5L, List.of(1L))).getErrorCode());
        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.deleteCollection(5L, 7L)).getErrorCode());
        assertEquals(ErrorCode.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.removeNote(7L, 5L, 1L)).getErrorCode());
    }

    @Test
    @DisplayName("批量收藏: 新增的计数+1, 重复的幂等跳过, 不存在的笔记报错")
    void collectNotesIdempotent() {
        when(collectionMapper.selectById(5L)).thenReturn(collection(5L, 7L));
        when(noteMapper.selectById(any(Long.class))).thenReturn(new Note());
        // 第一次收藏 10 成功, 11 重复
        when(collectionNoteMapper.insert(any())).thenReturn(1).thenReturn(0);

        int added = service.collectNotes(7L, 5L, List.of(10L, 11L));

        assertEquals(1, added);
        verify(noteMapper, times(1)).adjustCollectCount(10L, 1);
        verify(noteDailyStatService, times(1)).recordCollect(10L, 1);
        verify(noteMapper, times(0)).adjustCollectCount(11L, 1);

        when(noteMapper.selectById(404L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.collectNotes(7L, 5L, List.of(404L))).getErrorCode());
    }

    @Test
    @DisplayName("移除收藏: 有则计数-1, 无则幂等")
    void removeNoteIdempotent() {
        when(collectionMapper.selectById(5L)).thenReturn(collection(5L, 7L));
        when(collectionNoteMapper.delete(5L, 10L)).thenReturn(1).thenReturn(0);

        service.removeNote(7L, 5L, 10L);
        verify(noteMapper).adjustCollectCount(10L, -1);
        verify(noteDailyStatService).recordCollect(10L, -1);

        service.removeNote(7L, 5L, 10L);
        verify(noteMapper, times(1)).adjustCollectCount(10L, -1);
    }

    @Test
    @DisplayName("删除收藏夹(创建者): 先取条目再删, 无其他夹则递减")
    void deleteCollectionCascades() {
        when(collectionMapper.selectById(5L)).thenReturn(collection(5L, 7L));
        when(collectionNoteMapper.selectNoteIdsByCollection(5L)).thenReturn(List.of(10L, 11L));
        when(collectionNoteMapper.countByCreatorAndNote(eq(7L), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(0L);

        service.deleteCollection(5L, 7L);

        verify(collectionNoteMapper).deleteByCollectionId(5L);
        verify(noteMapper).adjustCollectCount(10L, -1);
        verify(noteMapper).adjustCollectCount(11L, -1);
        verify(collectionMapper).deleteById(5L);
    }

    @Test
    @DisplayName("移除收藏: 用户其他收藏夹仍收着 → 不递减")
    void removeNoteKeepsCountWhenInOtherCollection() {
        when(collectionMapper.selectById(5L)).thenReturn(collection(5L, 7L));
        when(collectionNoteMapper.delete(5L, 10L)).thenReturn(1);
        when(collectionNoteMapper.countByCreatorAndNote(7L, 10L)).thenReturn(1L); // 还有别的夹

        service.removeNote(7L, 5L, 10L);

        verify(noteMapper, org.mockito.Mockito.never()).adjustCollectCount(10L, -1);
    }
}
