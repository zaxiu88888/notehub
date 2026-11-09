package com.notehub.service.impl;

import com.notehub.mapper.NoteTagMapper;
import com.notehub.mapper.QuestionTagMapper;
import com.notehub.mapper.TagMapper;
import com.notehub.model.dto.tag.TagNameRow;
import com.notehub.model.entity.Tag;
import com.notehub.model.vo.tag.TagVO;
import com.notehub.service.TagService;
import com.notehub.utils.TagUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TagServiceImpl implements TagService {

    private static final int MAX_NOTE_TAGS = 5;
    private static final int DEFAULT_POPULAR_LIMIT = 20;
    private static final int MAX_POPULAR_LIMIT = 50;

    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final NoteTagMapper noteTagMapper;

    public TagServiceImpl(TagMapper tagMapper, QuestionTagMapper questionTagMapper,
                          NoteTagMapper noteTagMapper) {
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
        this.noteTagMapper = noteTagMapper;
    }

    @Override
    public List<TagVO> popularTags(Integer limit) {
        int size = limit == null ? DEFAULT_POPULAR_LIMIT
                : Math.min(Math.max(limit, 1), MAX_POPULAR_LIMIT);
        return tagMapper.selectPopular(size);
    }

    @Override
    public Map<Long, List<String>> tagsByQuestionIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return groupByOwner(questionTagMapper.selectNamesByQuestionIds(questionIds));
    }

    @Override
    public Map<Long, List<String>> tagsByNoteIds(List<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Map.of();
        }
        return groupByOwner(noteTagMapper.selectNamesByNoteIds(noteIds));
    }

    @Override
    public List<String> questionTags(Long questionId) {
        return questionTagMapper.selectNamesByQuestionId(questionId);
    }

    @Override
    @Transactional
    public void syncQuestionTags(Long questionId, String examPoint) {
        List<String> names = TagUtils.splitExamPoints(examPoint);
        List<Long> tagIds = ensureTagIds(names);
        questionTagMapper.deleteByQuestionId(questionId);
        tagIds.forEach(tagId -> questionTagMapper.insertIgnore(questionId, tagId));
        log.debug("[标签] 题目标签同步: questionId={}, tags={}", questionId, names);
    }

    @Override
    @Transactional
    public void syncNoteTags(Long noteId, Long questionId, List<String> requestedTags) {
        List<String> names = requestedTags == null
                ? questionTagMapper.selectNamesByQuestionId(questionId)
                : TagUtils.normalize(requestedTags, MAX_NOTE_TAGS);
        List<Long> tagIds = ensureTagIds(names);
        noteTagMapper.deleteByNoteId(noteId);
        tagIds.forEach(tagId -> noteTagMapper.insertIgnore(noteId, tagId));
        log.debug("[标签] 笔记标签同步: noteId={}, tags={}", noteId, names);
    }

    @Override
    public void deleteQuestionTags(Long questionId) {
        questionTagMapper.deleteByQuestionId(questionId);
    }

    @Override
    public void deleteNoteTags(Long noteId) {
        noteTagMapper.deleteByNoteId(noteId);
    }

    /** 名称 → tagId: 不存在则创建; 大小写不敏感命中已有标签; 按 tagId 去重 */
    private List<Long> ensureTagIds(List<String> names) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String name : names) {
            Tag tag = new Tag();
            tag.setName(name);
            tagMapper.upsert(tag);
            if (tag.getTagId() != null) {
                ids.add(tag.getTagId());
            }
        }
        return new ArrayList<>(ids);
    }

    private Map<Long, List<String>> groupByOwner(List<TagNameRow> rows) {
        return rows.stream().collect(Collectors.groupingBy(
                TagNameRow::getOwnerId,
                Collectors.mapping(TagNameRow::getName, Collectors.toList())));
    }
}
