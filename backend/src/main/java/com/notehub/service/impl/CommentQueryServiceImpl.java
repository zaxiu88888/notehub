package com.notehub.service.impl;

import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.mapper.CommentMapper;
import com.notehub.model.entity.Comment;
import com.notehub.model.vo.comment.CommentVO;
import com.notehub.service.CommentQueryService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论查询: 顶层分页(1) + count(1) + 楼中楼批量 IN(1) + 我的点赞 IN(1) = 与页大小无关
 */
@Service
public class CommentQueryServiceImpl implements CommentQueryService {

    private final CommentMapper commentMapper;

    public CommentQueryServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public PageVO<CommentVO> listComments(Long noteId, int page, int pageSize, Long currentUserId) {
        pageSize = PageUtils.clampPageSize(pageSize);
        page = PageUtils.clampPage(page);

        long total = commentMapper.countTopLevel(noteId);
        if (total == 0) {
            return PageVO.of(List.of(), page, pageSize, 0);
        }
        List<Comment> topLevel = commentMapper.selectTopLevel(noteId, PageUtils.offset(page, pageSize), pageSize);
        if (topLevel.isEmpty()) { // 页码越界: 不能走后续 IN () 查询(SQL 语法会炸)
            return PageVO.of(List.of(), page, pageSize, total);
        }

        // 楼中楼: 整页顶层评论的回复一次取齐
        List<Long> parentIds = topLevel.stream().map(Comment::getCommentId).toList();
        Map<Long, List<Comment>> repliesByParent = commentMapper.selectRepliesByParentIds(parentIds)
                .stream().collect(Collectors.groupingBy(Comment::getParentId));

        // 我的点赞标记(登录时): 覆盖顶层+回复
        Set<Long> likedIds = Set.of();
        if (currentUserId != null) {
            List<Long> allIds = new java.util.ArrayList<>(parentIds);
            repliesByParent.values().stream().flatMap(List::stream)
                    .forEach(r -> allIds.add(r.getCommentId()));
            if (!allIds.isEmpty()) {
                likedIds = new HashSet<>(commentMapper.selectLikedCommentIds(currentUserId, allIds));
            }
        }

        Set<Long> finalLiked = likedIds;
        List<CommentVO> vos = topLevel.stream()
                .map(c -> toVO(c, finalLiked, repliesByParent.getOrDefault(c.getCommentId(), List.of())))
                .toList();
        return PageVO.of(vos, page, pageSize, total);
    }

    private CommentVO toVO(Comment comment, Set<Long> likedIds, List<Comment> replies) {
        CommentVO vo = baseVO(comment, likedIds);
        vo.setReplies(replies.stream().map(r -> baseVO(r, likedIds)).toList());
        return vo;
    }

    private CommentVO baseVO(Comment comment, Set<Long> likedIds) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getCommentId());
        vo.setNoteId(comment.getNoteId());
        vo.setParentId(comment.getParentId());
        vo.setReplyToId(comment.getReplyToId());
        vo.setContent(comment.getIsDeleted() != null && comment.getIsDeleted() == 1
                ? "该评论已删除" : comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount());
        vo.setIsDeleted(comment.getIsDeleted());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setReplyToUserName(comment.getReplyToUserName());
        vo.setLiked(likedIds.contains(comment.getCommentId()));

        CommentVO.AuthorVO author = new CommentVO.AuthorVO();
        author.setUserId(comment.getAuthorId());
        author.setUsername(comment.getAuthorName());
        author.setAvatarUrl(comment.getAuthorAvatarUrl());
        vo.setAuthor(author);
        return vo;
    }
}
