package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.event.InteractionEvent;
import com.notehub.mapper.CommentLikeMapper;
import com.notehub.mapper.CommentMapper;
import com.notehub.mapper.NoteLikeMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Comment;
import com.notehub.model.entity.Note;
import com.notehub.service.impl.NoteInteractionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteInteractionServiceImplTest {

    private NoteMapper noteMapper;
    private NoteLikeMapper noteLikeMapper;
    private CommentMapper commentMapper;
    private CommentLikeMapper commentLikeMapper;
    private ApplicationEventPublisher publisher;
    private NoteInteractionServiceImpl service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        noteLikeMapper = mock(NoteLikeMapper.class);
        commentMapper = mock(CommentMapper.class);
        commentLikeMapper = mock(CommentLikeMapper.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new NoteInteractionServiceImpl(noteMapper, noteLikeMapper,
                commentMapper, commentLikeMapper, publisher);
    }

    private Note note(long id, long authorId) {
        Note n = new Note();
        n.setNoteId(id);
        n.setAuthorId(authorId);
        n.setQuestionId(2L);
        n.setLikeCount(5);
        return n;
    }

    private Comment comment(long id, long noteId, long authorId, Long parentId) {
        Comment c = new Comment();
        c.setCommentId(id);
        c.setNoteId(noteId);
        c.setAuthorId(authorId);
        c.setParentId(parentId);
        c.setIsDeleted(0);
        return c;
    }

    @Test
    @DisplayName("点赞: 首次=insert+计数+1+事件; 重复=幂等无副作用; 自赞=无事件")
    void likeNoteIdempotentAndEventRules() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        when(noteLikeMapper.insert(any())).thenReturn(1);
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));

        service.likeNote(10L, 7L); // 7 赞 2 的笔记
        verify(noteMapper).adjustLikeCount(10L, 1);
        verify(publisher, times(1)).publishEvent(any(InteractionEvent.class));

        // 重复点赞: insert 返回 0 → 不加计数不发事件
        when(noteLikeMapper.insert(any())).thenReturn(0);
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        service.likeNote(10L, 7L);
        verify(noteMapper, times(1)).adjustLikeCount(10L, 1); // 仍只调了一次

        // 自赞: 不发事件
        when(noteLikeMapper.insert(any())).thenReturn(1);
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        service.likeNote(10L, 2L);
        verify(publisher, times(1)).publishEvent(any(InteractionEvent.class)); // 总数不变
    }

    @Test
    @DisplayName("取消点赞: 赞过则计数-1; 没赞过幂等跳过")
    void unlikeNoteIdempotent() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        when(noteLikeMapper.delete(10L, 7L)).thenReturn(1);
        service.unlikeNote(10L, 7L);
        verify(noteMapper).adjustLikeCount(10L, -1);

        when(noteLikeMapper.delete(10L, 7L)).thenReturn(0);
        service.unlikeNote(10L, 7L);
        verify(noteMapper, times(1)).adjustLikeCount(10L, -1);
    }

    @Test
    @DisplayName("顶层评论: 计数+1 且通知笔记作者(非自评)")
    void addTopLevelCommentNotifiesAuthor() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            inv.getArgument(0, Comment.class).setCommentId(500L);
            return 1;
        });

        Long commentId = service.addComment(10L, 7L, null, "写得好!");

        assertEquals(500L, commentId);
        verify(noteMapper).adjustCommentCount(10L, 1);
        verify(publisher).publishEvent(any(InteractionEvent.class));
    }

    @Test
    @DisplayName("楼中楼: 挂到顶层根评论, 回复计数+1, 通知被回复者")
    void addReplyCommentNotifiesRepliedUser() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        // 顶层评论 300(作者2), 目标是它的楼中楼 400(作者9) → 回复 400 应挂到 300 下
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 10L, 9L, 300L));
        when(commentMapper.selectById(300L)).thenReturn(comment(300L, 10L, 2L, null));
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            inv.getArgument(0, Comment.class).setCommentId(501L);
            return 1;
        });

        service.addComment(10L, 7L, 400L, "回复楼中楼");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals(300L, captor.getValue().getParentId()); // 归到顶层
        assertEquals(400L, captor.getValue().getReplyToId());
        verify(commentMapper).adjustReplyCount(300L, 1);
        verify(publisher, times(2)).publishEvent(any(InteractionEvent.class)); // 笔记作者+被回复者
    }

    @Test
    @DisplayName("评论校验: 跨笔记回复/回复已删评论 → BAD_PARAM")
    void addCommentValidations() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 999L, 9L, null)); // noteId 不匹配

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.addComment(10L, 7L, 400L, "x"));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());

        Comment deleted = comment(401L, 10L, 9L, null);
        deleted.setIsDeleted(1);
        when(commentMapper.selectById(401L)).thenReturn(deleted);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.addComment(10L, 7L, 401L, "x"));
        assertEquals(ErrorCode.BAD_PARAM, e2.getErrorCode());
    }

    @Test
    @DisplayName("删评论(作者): 软删+笔记计数-1+父回复计数-1")
    void deleteCommentAdjustsCounters() {
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 10L, 7L, 300L));
        when(commentMapper.softDelete(400L, 7L)).thenReturn(1);

        service.deleteComment(400L, 7L);

        verify(noteMapper).adjustCommentCount(10L, -1);
        verify(commentMapper).adjustReplyCount(300L, -1);
    }

    @Test
    @DisplayName("删父评论级联: 回复一并软删, commentCount 按 1+回复数 递减")
    void deleteParentCascadesReplies() {
        when(commentMapper.selectById(300L)).thenReturn(comment(300L, 10L, 7L, null));
        when(commentMapper.softDelete(300L, 7L)).thenReturn(1);
        when(commentMapper.countActiveReplies(300L)).thenReturn(2L);

        service.deleteComment(300L, 7L);

        verify(commentMapper).softDeleteReplies(300L);
        verify(noteMapper).adjustCommentCount(10L, -3); // 1 条父 + 2 条回复
    }

    @Test
    @DisplayName("回复消息目标: targetType=NOTE + noteId(前端可跳转)")
    void replyEventTargetsNote() {
        when(noteMapper.selectById(10L)).thenReturn(note(10L, 2L));
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 10L, 9L, 300L));
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            inv.getArgument(0, Comment.class).setCommentId(502L);
            return 1;
        });

        service.addComment(10L, 7L, 400L, "回复");

        org.mockito.ArgumentCaptor<org.springframework.context.ApplicationEvent> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(publisher, times(2)).publishEvent(captor.capture());
        java.util.List<InteractionEvent> events = captor.getAllValues().stream()
                .map(o -> (InteractionEvent) o).toList();
        events.forEach(e -> {
            assertEquals(com.notehub.model.enums.MessageTargetType.NOTE, e.getTargetType());
            assertEquals(10L, e.getTargetId());
        });
    }

    @Test
    @DisplayName("评论点赞: 原子计数+通知评论作者(自赞除外)")
    void likeComment() {
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 10L, 9L, null));
        when(commentLikeMapper.insert(any())).thenReturn(1);

        service.likeComment(400L, 7L);
        verify(commentMapper).adjustLikeCount(400L, 1);
        verify(publisher).publishEvent(any(InteractionEvent.class));

        when(commentLikeMapper.insert(any())).thenReturn(0);
        service.likeComment(400L, 7L);
        verify(commentMapper, times(1)).adjustLikeCount(400L, 1);
    }

    @Test
    @DisplayName("编辑评论: 仅作者本人")
    void updateCommentOnlyAuthor() {
        when(commentMapper.selectById(400L)).thenReturn(comment(400L, 10L, 7L, null));
        when(commentMapper.updateContent(400L, 7L, "new")).thenReturn(1);
        service.updateComment(400L, 7L, "new"); // OK

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.updateComment(400L, 99L, "new"));
        assertEquals(ErrorCode.FORBIDDEN, e.getErrorCode());
    }

    @Test
    @DisplayName("不存在的笔记/评论 → NOT_FOUND")
    void notFoundGuards() {
        when(noteMapper.selectById(404L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.likeNote(404L, 7L)).getErrorCode());

        when(commentMapper.selectById(404L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.likeComment(404L, 7L)).getErrorCode());
    }
}
