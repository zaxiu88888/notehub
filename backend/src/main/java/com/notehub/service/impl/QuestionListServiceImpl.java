package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.QuestionListMapper;
import com.notehub.mapper.QuestionListItemMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.entity.Question;
import com.notehub.model.entity.QuestionList;
import com.notehub.model.entity.QuestionListItem;
import com.notehub.model.vo.questionList.QuestionListVO;
import com.notehub.model.vo.questionListItem.QuestionListItemVO;
import com.notehub.service.QuestionListService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionListServiceImpl implements QuestionListService {

    private final QuestionListMapper questionListMapper;
    private final QuestionListItemMapper questionListItemMapper;
    private final QuestionMapper questionMapper;

    public QuestionListServiceImpl(QuestionListMapper questionListMapper,
                                   QuestionListItemMapper questionListItemMapper,
                                   QuestionMapper questionMapper) {
        this.questionListMapper = questionListMapper;
        this.questionListItemMapper = questionListItemMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<QuestionListVO> listQuestionLists() {
        Map<Long, Long> counts = questionListItemMapper.countGroupByListId().stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("questionListId")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue()));
        return questionListMapper.selectAll().stream()
                .map(this::toVO)
                .peek(vo -> vo.setQuestionCount(counts.getOrDefault(vo.getQuestionListId(), 0L)))
                .toList();
    }

    @Override
    public QuestionListVO getQuestionList(Long questionListId) {
        QuestionList list = requireQuestionList(questionListId);
        QuestionListVO vo = toVO(list);
        vo.setQuestionCount(questionListItemMapper.countByListId(questionListId));
        return vo;
    }

    @Override
    public QuestionListVO create(String name, Integer type, String description) {
        QuestionList list = new QuestionList();
        list.setName(name);
        list.setType(type);
        list.setDescription(description);
        questionListMapper.insert(list);
        return toVO(questionListMapper.selectById(list.getQuestionListId()));
    }

    @Override
    public QuestionListVO update(Long questionListId, String name, Integer type, String description) {
        QuestionList list = requireQuestionList(questionListId);
        if (name != null) list.setName(name);
        if (type != null) list.setType(type);
        if (description != null) list.setDescription(description);
        questionListMapper.update(list);
        return toVO(questionListMapper.selectById(questionListId));
    }

    @Override
    @Transactional
    public void delete(Long questionListId) {
        requireQuestionList(questionListId);
        questionListItemMapper.deleteByQuestionListId(questionListId);
        questionListMapper.deleteById(questionListId);
    }

    @Override
    public List<QuestionListItemVO> listItems(Long questionListId, Long currentUserId) {
        requireQuestionList(questionListId);
        List<QuestionListItem> items = questionListItemMapper.listByQuestionListId(questionListId);
        return toVOs(items, currentUserId);
    }

    @Override
    public List<QuestionListItemVO> listItemsForAdmin(Long questionListId) {
        requireQuestionList(questionListId);
        return toVOs(questionListItemMapper.listByQuestionListId(questionListId), null);
    }

    @Override
    public void addItem(Long questionListId, Long questionId, Integer sortRank) {
        requireQuestionList(questionListId);
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (questionListItemMapper.selectOne(questionListId, questionId) != null) {
            throw BusinessException.of(ErrorCode.DUPLICATED, "该题已在题单中");
        }
        QuestionListItem item = new QuestionListItem();
        item.setQuestionListId(questionListId);
        item.setQuestionId(questionId);
        item.setSortRank(sortRank);
        questionListItemMapper.insert(item);
    }

    @Override
    public void removeItem(Long questionListId, Long questionId) {
        requireQuestionList(questionListId);
        questionListItemMapper.delete(questionListId, questionId);
    }

    @Override
    @Transactional
    public void sortItems(Long questionListId, List<Long> questionIdsInOrder) {
        requireQuestionList(questionListId);
        Set<Long> existing = questionListItemMapper.listByQuestionListId(questionListId).stream()
                .map(QuestionListItem::getQuestionId)
                .collect(Collectors.toSet());

        Set<Long> requested = new HashSet<>(questionIdsInOrder);
        // 请求里的题目必须都在题单内, 防止越权排序
        if (!existing.containsAll(requested)) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "排序列表包含不在题单内的题目");
        }
        int rank = 1;
        for (Long questionId : questionIdsInOrder) {
            questionListItemMapper.updateSortRank(questionListId, questionId, rank++);
        }
    }

    private List<QuestionListItemVO> toVOs(List<QuestionListItem> items, Long currentUserId) {
        Set<Long> myNotedQuestionIds = currentUserId == null || items.isEmpty()
                ? Set.of()
                : new HashSet<>(questionListItemMapper.selectQuestionIdsWithNote(
                        currentUserId, items.stream().map(QuestionListItem::getQuestionId).toList()));

        return items.stream().map(item -> {
            QuestionListItemVO vo = new QuestionListItemVO();
            vo.setQuestionId(item.getQuestionId());
            vo.setTitle(item.getTitle());
            vo.setDifficulty(item.getDifficulty());
            vo.setExamPoint(item.getExamPoint());
            vo.setCategoryId(item.getCategoryId());
            vo.setCategoryName(item.getCategoryName());
            vo.setSortRank(item.getSortRank());
            vo.setHasMyNote(myNotedQuestionIds.contains(item.getQuestionId()));
            return vo;
        }).toList();
    }

    private QuestionList requireQuestionList(Long questionListId) {
        QuestionList list = questionListMapper.selectById(questionListId);
        if (list == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "题单不存在");
        }
        return list;
    }

    private QuestionListVO toVO(QuestionList list) {
        QuestionListVO vo = new QuestionListVO();
        vo.setQuestionListId(list.getQuestionListId());
        vo.setName(list.getName());
        vo.setType(list.getType());
        vo.setDescription(list.getDescription());
        vo.setCreatedAt(list.getCreatedAt());
        return vo;
    }
}
