package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.mapper.CollectionNoteMapper;
import com.notehub.mapper.CommentMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.Question;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.service.NoteService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 笔记服务: 列表聚合防 N+1
 * SQL 次数 = 主表分页(1) + count(1) + 我赞过的(0/1) + 我收藏过的(0/1) = 与页大小无关
 */
@Log4j2
@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final CommentMapper commentMapper;
    private final CollectionNoteMapper collectionNoteMapper;
    private final QuestionMapper questionMapper;

    public NoteServiceImpl(NoteMapper noteMapper, CommentMapper commentMapper,
                           CollectionNoteMapper collectionNoteMapper, QuestionMapper questionMapper) {
        this.noteMapper = noteMapper;
        this.commentMapper = commentMapper;
        this.collectionNoteMapper = collectionNoteMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public PageVO<NoteVO> listNotes(NoteQueryParams params, Long currentUserId) {
        int pageSize = PageUtils.clampPageSize(params.getPageSize());
        int page = PageUtils.clampPage(params.getPage());
        params.setPage(page);
        params.setPageSize(pageSize);

        long total = noteMapper.countByQuery(params);
        List<Note> notes = noteMapper.listByQuery(params, PageUtils.offset(page, pageSize), pageSize);
        return PageVO.of(toVOs(notes, currentUserId), page, pageSize, total);
    }

    @Override
    public NoteVO getNote(Long noteId, Long currentUserId) {
        Note note = requireNote(noteId);
        return toVOs(List.of(note), currentUserId).get(0);
    }

    @Override
    public Long createNote(Long authorId, Long questionId, String content) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (noteMapper.selectByAuthorAndQuestion(authorId, questionId) != null) {
            throw BusinessException.of(ErrorCode.DUPLICATED, "你已在该题下写过笔记, 请编辑原有笔记");
        }
        Note note = new Note();
        note.setAuthorId(authorId);
        note.setQuestionId(questionId);
        note.setContent(content);
        note.setSearchVector(buildSearchVector(question.getTitle(), content));
        noteMapper.insert(note);
        log.info("[笔记] 发布: noteId={}, author={}, question={}", note.getNoteId(), authorId, questionId);
        return note.getNoteId();
    }

    @Override
    public void updateNote(Long noteId, Long operatorId, String content) {
        Note note = requireNote(noteId);
        if (!note.getAuthorId().equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只能编辑自己的笔记");
        }
        noteMapper.updateContent(noteId, content,
                buildSearchVector(note.getQuestionTitle(), content));
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, Long operatorId) {
        Note note = requireNote(noteId);
        if (!note.getAuthorId().equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只能删除自己的笔记");
        }
        // 连带清理: 软删评论 + 清空收藏夹条目(点赞记录随笔记物理删除自然消失)
        commentMapper.softDeleteByNoteId(noteId);
        collectionNoteMapper.deleteByNoteId(noteId);
        noteMapper.deleteById(noteId);
        log.info("[笔记] 删除(连带清理): noteId={}, author={}", noteId, operatorId);
    }

    @Override
    public Long findMyNoteId(Long authorId, Long questionId) {
        Note note = noteMapper.selectByAuthorAndQuestion(authorId, questionId);
        return note == null ? null : note.getNoteId();
    }

    @Override
    public PageVO<NoteVO> listNotesByIds(List<Long> noteIds, int page, int pageSize, Long currentUserId) {
        pageSize = PageUtils.clampPageSize(pageSize);
        page = PageUtils.clampPage(page);
        if (noteIds == null || noteIds.isEmpty()) {
            return PageVO.of(List.of(), page, pageSize, 0);
        }
        long total = noteIds.size();
        List<Long> pageIds = noteIds.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .toList();
        if (pageIds.isEmpty()) {
            return PageVO.of(List.of(), page, pageSize, total);
        }
        // 批量查(1 条 SQL, 防 N+1), 再按 pageIds 顺序还原(收藏时间序)
        Map<Long, Note> byId = noteMapper.selectByIds(pageIds).stream()
                .collect(Collectors.toMap(Note::getNoteId, n -> n));
        List<Note> notes = pageIds.stream().map(byId::get)
                .filter(Objects::nonNull).toList();
        return PageVO.of(toVOs(notes, currentUserId), page, pageSize, total);
    }

    @Override
    public List<NoteVO> toVOList(List<Note> notes, Long currentUserId) {
        return toVOs(notes, currentUserId);
    }

    /** 搜索向量 = 题目标题 + 笔记正文(供 ngram 全文索引) */
    private String buildSearchVector(String questionTitle, String content) {
        String title = questionTitle == null ? "" : questionTitle;
        return title + "\n" + (content == null ? "" : content);
    }

    private Note requireNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        return note;
    }

    /** 批量组装: 主列表已联表作者/题目, 这里只补互动状态(登录时 2 条 IN 查询) */
    private List<NoteVO> toVOs(List<Note> notes, Long currentUserId) {
        Set<Long> likedIds;
        Set<Long> collectedIds;
        if (currentUserId != null && !notes.isEmpty()) {
            List<Long> ids = notes.stream().map(Note::getNoteId).toList();
            likedIds = new HashSet<>(noteMapper.selectLikedNoteIds(currentUserId, ids));
            collectedIds = new HashSet<>(noteMapper.selectCollectedNoteIds(currentUserId, ids));
        } else {
            likedIds = Set.of();
            collectedIds = Set.of();
        }
        final Set<Long> liked = likedIds;
        final Set<Long> collected = collectedIds;
        return notes.stream().map(note -> {
            NoteVO vo = new NoteVO();
            vo.setNoteId(note.getNoteId());
            vo.setQuestionId(note.getQuestionId());
            vo.setContent(note.getContent());
            vo.setLikeCount(note.getLikeCount());
            vo.setCommentCount(note.getCommentCount());
            vo.setCollectCount(note.getCollectCount());
            vo.setCreatedAt(note.getCreatedAt());

            NoteVO.AuthorVO author = new NoteVO.AuthorVO();
            author.setUserId(note.getAuthorId());
            author.setUsername(note.getAuthorName());
            author.setAvatarUrl(note.getAuthorAvatarUrl());
            vo.setAuthor(author);

            NoteVO.QuestionVO question = new NoteVO.QuestionVO();
            question.setQuestionId(note.getQuestionId());
            question.setTitle(note.getQuestionTitle());
            question.setDifficulty(note.getQuestionDifficulty());
            vo.setQuestion(question);

            NoteVO.MyActionsVO actions = new NoteVO.MyActionsVO();
            actions.setLiked(liked.contains(note.getNoteId()));
            actions.setCollected(collected.contains(note.getNoteId()));
            vo.setMyActions(actions);
            return vo;
        }).toList();
    }
}
