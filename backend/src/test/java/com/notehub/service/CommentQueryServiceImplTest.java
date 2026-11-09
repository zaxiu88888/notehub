package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.mapper.CommentMapper;
import com.notehub.model.entity.Comment;
import com.notehub.model.vo.comment.CommentVO;
import com.notehub.service.impl.CommentQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentQueryServiceImplTest {

    private CommentMapper commentMapper;
    private CommentQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        service = new CommentQueryServiceImpl(commentMapper);
    }

    private Comment top(long id, long authorId) {
        Comment c = new Comment();
        c.setCommentId(id);
        c.setNoteId(10L);
        c.setAuthorId(authorId);
        c.setParentId(null);
        c.setIsDeleted(0);
        c.setContent("顶层评论" + id);
        c.setAuthorName("用户" + authorId);
        return c;
    }

    private Comment reply(long id, long parent, long authorId) {
        Comment c = top(id, authorId);
        c.setParentId(parent);
        c.setReplyToId(parent);
        c.setContent("回复" + id);
        return c;
    }

    @Test
    @DisplayName("评论列表: 顶层分页+楼中楼按 parent 归组装配")
    void listCommentsAssemblesReplies() {
        when(commentMapper.countTopLevel(10L)).thenReturn(2L);
        when(commentMapper.selectTopLevel(eq(10L), eq(0), eq(10)))
                .thenReturn(List.of(top(100L, 2L), top(200L, 3L)));
        when(commentMapper.selectRepliesByParentIds(List.of(100L, 200L)))
                .thenReturn(List.of(reply(101L, 100L, 3L), reply(102L, 100L, 4L), reply(201L, 200L, 2L)));

        PageVO<CommentVO> page = service.listComments(10L, 1, 10, null);

        assertEquals(2, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals(2, page.getList().get(0).getReplies().size()); // 100 的楼中楼
        assertEquals(1, page.getList().get(1).getReplies().size()); // 200 的楼中楼
        assertEquals(101L, page.getList().get(0).getReplies().get(0).getCommentId());
    }

    @Test
    @DisplayName("我的点赞标记: 登录时批量填充(顶层+楼中楼), 未登录零查询")
    void likedFlags() {
        when(commentMapper.countTopLevel(10L)).thenReturn(1L);
        when(commentMapper.selectTopLevel(eq(10L), eq(0), eq(10))).thenReturn(List.of(top(100L, 2L)));
        when(commentMapper.selectRepliesByParentIds(List.of(100L))).thenReturn(List.of(reply(101L, 100L, 3L)));
        when(commentMapper.selectLikedCommentIds(eq(7L), anyList())).thenReturn(List.of(101L));

        PageVO<CommentVO> page = service.listComments(10L, 1, 10, 7L);
        assertFalse(page.getList().get(0).isLiked());       // 顶层未赞
        assertTrue(page.getList().get(0).getReplies().get(0).isLiked()); // 楼中楼已赞

        // 未登录: 不查点赞
        service.listComments(10L, 1, 10, null);
        verify(commentMapper, never()).selectLikedCommentIds(eq(-1L), anyList());
    }

    @Test
    @DisplayName("软删评论: 内容替换为占位文案, 仍返回结构")
    void deletedCommentMasked() {
        Comment deleted = top(100L, 2L);
        deleted.setIsDeleted(1);
        when(commentMapper.countTopLevel(10L)).thenReturn(1L);
        when(commentMapper.selectTopLevel(eq(10L), eq(0), eq(10))).thenReturn(List.of(deleted));
        when(commentMapper.selectRepliesByParentIds(List.of(100L))).thenReturn(List.of());

        PageVO<CommentVO> page = service.listComments(10L, 1, 10, null);
        assertEquals("该评论已删除", page.getList().get(0).getContent());
        assertEquals(1, page.getList().get(0).getIsDeleted());
    }

    @Test
    @DisplayName("空评论区: 直接返回空页, 零列表查询")
    void emptyComments() {
        when(commentMapper.countTopLevel(10L)).thenReturn(0L);
        PageVO<CommentVO> page = service.listComments(10L, 1, 10, null);
        assertEquals(0, page.getTotal());
        verify(commentMapper, never()).selectTopLevel(eq(10L), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("页码越界: total>0 但本页为空 → 空页且不触发 IN () 空列表查询")
    void outOfRangePageSafe() {
        when(commentMapper.countTopLevel(10L)).thenReturn(5L);
        when(commentMapper.selectTopLevel(eq(10L), eq(100), eq(10))).thenReturn(List.of());

        PageVO<CommentVO> page = service.listComments(10L, 11, 10, 7L);

        assertEquals(5, page.getTotal());
        assertEquals(0, page.getList().size());
        verify(commentMapper, never()).selectRepliesByParentIds(anyList());
        verify(commentMapper, never()).selectLikedCommentIds(org.mockito.ArgumentMatchers.anyLong(), anyList());
    }
}
