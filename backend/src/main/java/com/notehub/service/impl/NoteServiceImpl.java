package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.event.NoteHeatmapEvent;
import com.notehub.mapper.CollectionNoteMapper;
import com.notehub.mapper.CommentMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.Question;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.service.AiSummaryService;
import com.notehub.service.ImageCleanupService;
import com.notehub.service.NoteDailyStatService;
import com.notehub.service.NoteService;
import com.notehub.service.TagService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
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
    private final TagService tagService;
    private final NoteDailyStatService noteDailyStatService;
    private final ImageCleanupService imageCleanupService;
    private final AiSummaryService aiSummaryService;
    private final ApplicationEventPublisher eventPublisher;

    public NoteServiceImpl(NoteMapper noteMapper, CommentMapper commentMapper,
                           CollectionNoteMapper collectionNoteMapper, QuestionMapper questionMapper,
                           TagService tagService, NoteDailyStatService noteDailyStatService,
                           ImageCleanupService imageCleanupService, AiSummaryService aiSummaryService,
                           ApplicationEventPublisher eventPublisher) {
        this.noteMapper = noteMapper;
        this.commentMapper = commentMapper;
        this.collectionNoteMapper = collectionNoteMapper;
        this.questionMapper = questionMapper;
        this.tagService = tagService;
        this.noteDailyStatService = noteDailyStatService;
        this.imageCleanupService = imageCleanupService;
        this.aiSummaryService = aiSummaryService;
        this.eventPublisher = eventPublisher;
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
    @Transactional
    public Long createNote(Long authorId, Long questionId, String content, List<String> tags) {
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
        // 笔记标签: 未传则继承题目标签
        tagService.syncNoteTags(note.getNoteId(), questionId, tags);
        // 热力图增量(位图已建立时生效, 否则等下次读取时回填)
        eventPublisher.publishEvent(new NoteHeatmapEvent(this, authorId, LocalDate.now(), 1));
        log.info("[笔记] 发布: noteId={}, author={}, question={}", note.getNoteId(), authorId, questionId);
        return note.getNoteId();
    }

    @Override
    @Transactional
    public void updateNote(Long noteId, Long operatorId, String content, List<String> tags) {
        Note note = requireNote(noteId);
        if (!note.getAuthorId().equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只能编辑自己的笔记");
        }
        String oldContent = note.getContent();
        noteMapper.updateContent(noteId, content,
                buildSearchVector(note.getQuestionTitle(), content));
        // tags=null 表示旧客户端未传, 保留原标签
        if (tags != null) {
            tagService.syncNoteTags(noteId, note.getQuestionId(), tags);
        }
        // 正文里被移除的图片: 事务提交后再删, 回滚不会误删文件
        runAfterCommit(() -> imageCleanupService.cleanupRemovedImages(oldContent, content));
        runAfterCommit(() -> aiSummaryService.evict(noteId));
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
        tagService.deleteNoteTags(noteId);
        noteDailyStatService.deleteByNoteId(noteId);
        noteMapper.deleteById(noteId);
        // 笔记已删, 事务提交后逐个检查其他引用并清理正文图片
        runAfterCommit(() -> imageCleanupService.cleanupNoteImages(note.getContent()));
        runAfterCommit(() -> aiSummaryService.evict(noteId));
        LocalDate publishedDay = note.getCreatedAt() == null
                ? LocalDate.now() : note.getCreatedAt().toLocalDate();
        eventPublisher.publishEvent(new NoteHeatmapEvent(this, note.getAuthorId(), publishedDay, -1));
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

    /** 有事务时提交后执行, 无事务(单测)直接执行 */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
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

    /** 批量组装: 主列表已联表作者/题目, 这里批量补互动状态与标签(与页大小无关) */
    private List<NoteVO> toVOs(List<Note> notes, Long currentUserId) {
        List<Long> ids = notes.isEmpty() ? List.of() : notes.stream().map(Note::getNoteId).toList();
        Set<Long> likedIds;
        Set<Long> collectedIds;
        if (currentUserId != null && !ids.isEmpty()) {
            likedIds = new HashSet<>(noteMapper.selectLikedNoteIds(currentUserId, ids));
            collectedIds = new HashSet<>(noteMapper.selectCollectedNoteIds(currentUserId, ids));
        } else {
            likedIds = Set.of();
            collectedIds = Set.of();
        }
        Map<Long, List<String>> tagsByNote = tagService.tagsByNoteIds(ids);
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
            vo.setTags(tagsByNote.getOrDefault(note.getNoteId(), List.of()));

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
