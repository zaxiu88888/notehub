package com.notehub.service;

import com.notehub.mapper.NoteTagMapper;
import com.notehub.mapper.QuestionTagMapper;
import com.notehub.mapper.TagMapper;
import com.notehub.model.dto.tag.TagNameRow;
import com.notehub.model.entity.Tag;
import com.notehub.model.vo.tag.TagVO;
import com.notehub.service.impl.TagServiceImpl;
import com.notehub.TestCaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagServiceImplTest {

    private TagMapper tagMapper;
    private QuestionTagMapper questionTagMapper;
    private NoteTagMapper noteTagMapper;
    private TagServiceImpl service;

    @BeforeEach
    void setUp() {
        tagMapper = mock(TagMapper.class);
        questionTagMapper = mock(QuestionTagMapper.class);
        noteTagMapper = mock(NoteTagMapper.class);
        service = new TagServiceImpl(tagMapper, questionTagMapper, noteTagMapper,
                TestCaches.passThrough());

        // upsert 模拟 MySQL: 名称首次出现分配递增 ID, 重复返回同一 ID
        Map<String, Long> ids = new HashMap<>();
        AtomicLong seq = new AtomicLong(1);
        doAnswer(inv -> {
            Tag tag = inv.getArgument(0);
            tag.setTagId(ids.computeIfAbsent(tag.getName(), key -> seq.getAndIncrement()));
            return 1;
        }).when(tagMapper).upsert(any(Tag.class));
    }

    @Test
    @DisplayName("热门标签: 默认 20, limit 钳制到 50")
    void popularTagsClampsLimit() {
        when(tagMapper.selectPopular(anyInt())).thenReturn(List.of(new TagVO()));
        service.popularTags(null);
        verify(tagMapper).selectPopular(20);
        service.popularTags(999);
        verify(tagMapper).selectPopular(50);
    }

    @Test
    @DisplayName("批量标签: 按 ownerId 分组")
    void tagsGroupByOwner() {
        TagNameRow a = new TagNameRow();
        a.setOwnerId(1L);
        a.setName("数组");
        TagNameRow b = new TagNameRow();
        b.setOwnerId(1L);
        b.setName("哈希表");
        TagNameRow c = new TagNameRow();
        c.setOwnerId(2L);
        c.setName("链表");
        when(questionTagMapper.selectNamesByQuestionIds(List.of(1L, 2L)))
                .thenReturn(List.of(a, b, c));

        Map<Long, List<String>> map = service.tagsByQuestionIds(List.of(1L, 2L));

        assertEquals(List.of("数组", "哈希表"), map.get(1L));
        assertEquals(List.of("链表"), map.get(2L));
        assertEquals(Map.of(), service.tagsByNoteIds(List.of())); // 空列表不查库
    }

    @Test
    @DisplayName("题目标签同步: exam_point 拆分去重后重建关联")
    void syncQuestionTagsRebuilds() {
        service.syncQuestionTags(9L, "数组；哈希表;数组");

        verify(questionTagMapper).deleteByQuestionId(9L);
        verify(questionTagMapper).insertIgnore(9L, 1L);
        verify(questionTagMapper).insertIgnore(9L, 2L);
    }

    @Test
    @DisplayName("笔记标签: null 继承题目; 非 null 以作者选择为准")
    void syncNoteTagsInheritOrReplace() {
        when(questionTagMapper.selectNamesByQuestionId(2L)).thenReturn(List.of("数组", "哈希表"));
        service.syncNoteTags(10L, 2L, null);
        verify(noteTagMapper).deleteByNoteId(10L);
        verify(noteTagMapper).insertIgnore(10L, 1L);
        verify(noteTagMapper).insertIgnore(10L, 2L);

        // 大小写不敏感去重: DP/dp 只落一个标签
        service.syncNoteTags(11L, 2L, List.of(" DP ", "dp"));
        verify(noteTagMapper).insertIgnore(11L, 3L);
    }

    @Test
    @DisplayName("删除关联")
    void deletesRelations() {
        service.deleteQuestionTags(9L);
        service.deleteNoteTags(10L);
        verify(questionTagMapper).deleteByQuestionId(9L);
        verify(noteTagMapper).deleteByNoteId(10L);
    }
}
