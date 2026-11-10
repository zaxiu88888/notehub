package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.RetryOnDeadlock;
import com.notehub.event.InteractionEvent;
import com.notehub.mapper.CommentLikeMapper;
import com.notehub.mapper.CommentMapper;
import com.notehub.mapper.NoteLikeMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Comment;
import com.notehub.model.entity.CommentLike;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.NoteLike;
import com.notehub.service.NoteDailyStatService;
import com.notehub.service.NoteInteractionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 笔记互动: 原子计数 + 唯一约束防重 + 事件驱动消息(自赞自评不通知)
 */
@Log4j2
@Service
public class NoteInteractionServiceImpl implements NoteInteractionService {

    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final NoteDailyStatService noteDailyStatService;
    private final ApplicationEventPublisher eventPublisher;

    public NoteInteractionServiceImpl(NoteMapper noteMapper, NoteLikeMapper noteLikeMapper,
                                      CommentMapper commentMapper, CommentLikeMapper commentLikeMapper,
                                      NoteDailyStatService noteDailyStatService,
                                      ApplicationEventPublisher eventPublisher) {
        this.noteMapper = noteMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.noteDailyStatService = noteDailyStatService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @RetryOnDeadlock
    @Transactional
    public int likeNote(Long noteId, Long userId) {
        Note note = requireNote(noteId);
        NoteLike like = new NoteLike();
        like.setNoteId(noteId);
        like.setUserId(userId);
        int inserted = noteLikeMapper.insert(like); // INSERT IGNORE, uk 防重
        if (inserted > 0) {
            noteMapper.adjustLikeCount(noteId, 1); // 原子自增
            noteDailyStatService.recordLike(noteId, 1);
            if (!note.getAuthorId().equals(userId)) { // 自赞不通知
                eventPublisher.publishEvent(InteractionEvent.likeNote(this, note.getAuthorId(), userId, noteId));
            }
        }
        return currentLikeCount(noteId);
    }

    @Override
    @RetryOnDeadlock
    @Transactional
    public void unlikeNote(Long noteId, Long userId) {
        requireNote(noteId);
        NoteLike existing = noteLikeMapper.select(noteId, userId);
        int deleted = noteLikeMapper.delete(noteId, userId); // 幂等: 没赞过则无操作
        if (deleted > 0) {
            noteMapper.adjustLikeCount(noteId, -1);
            noteDailyStatService.recordLike(noteId, dateOf(existing), -1);
        }
    }

    @Override
    @Transactional
    public Long addComment(Long noteId, Long userId, Long parentId, String content) {
        Note note = requireNote(noteId);

        Long replyToId = null;
        Long rootParentId = null;
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || parent.getNoteId() == null || !parent.getNoteId().equals(noteId)) {
                throw BusinessException.of(ErrorCode.BAD_PARAM, "回复的评论不存在");
            }
            if (parent.getIsDeleted() != null && parent.getIsDeleted() == 1) {
                throw BusinessException.of(ErrorCode.BAD_PARAM, "该评论已被删除");
            }
            // 两级结构: 楼中楼挂到顶层根评论下
            rootParentId = parent.getParentId() == null ? parent.getCommentId() : parent.getParentId();
            replyToId = parent.getCommentId();
        }

        Comment comment = new Comment();
        comment.setNoteId(noteId);
        comment.setAuthorId(userId);
        comment.setParentId(rootParentId);
        comment.setReplyToId(replyToId);
        comment.setContent(content);
        commentMapper.insert(comment);

        noteMapper.adjustCommentCount(noteId, 1);
        noteDailyStatService.recordComment(noteId, 1);
        if (rootParentId != null) {
            commentMapper.adjustReplyCount(rootParentId, 1);
        }

        // 事件: 评论文给笔记作者, 回复给被回复者(自评不通知; 回复自己也不通知)
        String excerpt = excerpt(content);
        if (!note.getAuthorId().equals(userId)) {
            eventPublisher.publishEvent(
                    InteractionEvent.commentNote(this, note.getAuthorId(), userId, noteId, excerpt));
        }
        if (replyToId != null) {
            Comment target = commentMapper.selectById(replyToId);
            if (target != null && !target.getAuthorId().equals(userId)
                    && !target.getAuthorId().equals(note.getAuthorId())) { // 避免同一条双发
                eventPublisher.publishEvent(
                        InteractionEvent.replyComment(this, target.getAuthorId(), userId, noteId, excerpt));
            }
        }
        return comment.getCommentId();
    }

    @Override
    public void updateComment(Long commentId, Long operatorId, String content) {
        requireOwnComment(commentId, operatorId);
        int updated = commentMapper.updateContent(commentId, operatorId, content);
        if (updated == 0) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "评论不可编辑(可能已删除)");
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long operatorId) {
        Comment comment = requireOwnComment(commentId, operatorId);
        int deleted = commentMapper.softDelete(commentId, operatorId);
        if (deleted == 0) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "评论已删除");
        }
        // 级联: 顶层评论删除时, 其楼中楼一并软删(契约 §8), 计数按实际删除条数递减
        long activeReplies = commentMapper.countActiveReplies(commentId);
        if (activeReplies > 0) {
            commentMapper.softDeleteReplies(commentId);
        }
        noteMapper.adjustCommentCount(comment.getNoteId(), (int) -(1 + activeReplies));
        // 按评论发生日回退, 避免删旧评论时把今天的统计减成负
        noteDailyStatService.recordComment(comment.getNoteId(),
                comment.getCreatedAt() == null ? LocalDate.now() : comment.getCreatedAt().toLocalDate(),
                (int) -(1 + activeReplies));
        if (comment.getParentId() != null) {
            commentMapper.adjustReplyCount(comment.getParentId(), -1);
        }
    }

    @Override
    @RetryOnDeadlock
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = requireComment(commentId);
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        int inserted = commentLikeMapper.insert(like);
        if (inserted > 0) {
            commentMapper.adjustLikeCount(commentId, 1);
            if (!comment.getAuthorId().equals(userId)) {
                eventPublisher.publishEvent(
                        InteractionEvent.likeComment(this, comment.getAuthorId(), userId, commentId));
            }
        }
    }

    @Override
    @RetryOnDeadlock
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        requireComment(commentId);
        int deleted = commentLikeMapper.delete(commentId, userId);
        if (deleted > 0) {
            commentMapper.adjustLikeCount(commentId, -1);
        }
    }

    private Note requireNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        return note;
    }

    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "评论不存在");
        }
        return comment;
    }

    private Comment requireOwnComment(Long commentId, Long operatorId) {
        Comment comment = requireComment(commentId);
        if (!comment.getAuthorId().equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只能操作自己的评论");
        }
        return comment;
    }

    private LocalDate dateOf(NoteLike like) {
        return like == null || like.getCreatedAt() == null
                ? LocalDate.now() : like.getCreatedAt().toLocalDate();
    }

    private int currentLikeCount(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        return note == null ? 0 : note.getLikeCount();
    }

    private String excerpt(String content) {
        return content.length() <= 50 ? content : content.substring(0, 50) + "…";
    }
}
