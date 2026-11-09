package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.mapper.CategoryMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.dto.question.CreateQuestionBatchBody;
import com.notehub.model.dto.question.QuestionQueryParam;
import com.notehub.model.entity.Category;
import com.notehub.model.entity.Question;
import com.notehub.model.vo.question.BatchImportResultVO;
import com.notehub.model.vo.question.QuestionVO;
import com.notehub.service.QuestionService;
import com.notehub.service.TagService;
import com.notehub.utils.QuestionMarkdownParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final CategoryMapper categoryMapper;
    private final NoteMapper noteMapper;
    private final TagService tagService;

    public QuestionServiceImpl(QuestionMapper questionMapper, CategoryMapper categoryMapper,
                               NoteMapper noteMapper, TagService tagService) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.noteMapper = noteMapper;
        this.tagService = tagService;
    }

    @Override
    public PageVO<QuestionVO> listQuestions(QuestionQueryParam param) {
        if (param.getDifficulty() != null
                && (param.getDifficulty() < 1 || param.getDifficulty() > 3)) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "difficulty 取值 1/2/3");
        }
        int pageSize = PageUtils.clampPageSize(param.getPageSize());
        int page = PageUtils.clampPage(param.getPage());
        param.setPage(page);
        param.setPageSize(pageSize);

        long total = questionMapper.countByQuery(param);
        List<Question> rows = questionMapper.listByQuery(param, PageUtils.offset(page, pageSize), pageSize);
        Map<Long, List<String>> tagsByQuestion = tagService.tagsByQuestionIds(
                rows.stream().map(Question::getQuestionId).toList());
        List<QuestionVO> list = rows.stream()
                .map(q -> toListVO(q, tagsByQuestion.getOrDefault(q.getQuestionId(), List.of())))
                .toList();
        return PageVO.of(list, page, pageSize, total);
    }

    @Override
    public QuestionVO getQuestionDetail(Long questionId, Long currentUserId) {
        Question question = requireQuestion(questionId);
        questionMapper.incrementViewCount(questionId); // 浏览计数原子自增
        QuestionVO vo = toDetailVO(question, tagService.questionTags(questionId));
        // 登录用户: 回填"我在该题下的笔记"(前端据此显示 编辑/已写 标记)
        vo.setMyNoteId(currentUserId == null
                ? null : findMyNoteId(currentUserId, questionId));
        return vo;
    }

    private Long findMyNoteId(Long userId, Long questionId) {
        com.notehub.model.entity.Note note =
                noteMapper.selectByAuthorAndQuestion(userId, questionId);
        return note == null ? null : note.getNoteId();
    }

    @Override
    @Transactional
    public QuestionVO create(Long categoryId, String title, Integer difficulty,
                             String examPoint, String description) {
        requireCategory(categoryId);
        Question question = new Question();
        question.setCategoryId(categoryId);
        question.setTitle(title);
        question.setDifficulty(difficulty);
        question.setExamPoint(examPoint);
        question.setDescription(description);
        questionMapper.insert(question);
        tagService.syncQuestionTags(question.getQuestionId(), examPoint);
        return toDetailVO(questionMapper.selectById(question.getQuestionId()),
                tagService.questionTags(question.getQuestionId()));
    }

    @Override
    @Transactional
    public QuestionVO update(Long questionId, Long categoryId, String title, Integer difficulty,
                             String examPoint, String description) {
        requireQuestion(questionId);
        if (categoryId != null) {
            requireCategory(categoryId);
        }
        Question question = new Question();
        question.setQuestionId(questionId);
        question.setCategoryId(categoryId);
        question.setTitle(title);
        question.setDifficulty(difficulty);
        question.setExamPoint(examPoint);
        question.setDescription(description);
        questionMapper.update(question);
        Question updated = questionMapper.selectById(questionId);
        // examPoint 未传时沿用原值, 保证标签不丢失
        tagService.syncQuestionTags(questionId, updated.getExamPoint());
        return toDetailVO(updated, tagService.questionTags(questionId));
    }

    @Override
    @Transactional
    public void delete(Long questionId) {
        requireQuestion(questionId);
        if (questionMapper.countNotesByQuestionId(questionId) > 0) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "该题下已存在笔记, 无法删除");
        }
        tagService.deleteQuestionTags(questionId);
        questionMapper.deleteById(questionId);
    }

    @Override
    @Transactional
    public BatchImportResultVO batchImport(CreateQuestionBatchBody body) {
        requireCategory(body.getCategoryId());
        QuestionMarkdownParser.ParseResult parsed =
                QuestionMarkdownParser.parse(body.getMarkdown());

        List<BatchImportResultVO.Failure> failures = new ArrayList<>();
        int success = 0;
        for (QuestionMarkdownParser.ParsedQuestion q : parsed.getQuestions()) {
            Question question = new Question();
            question.setCategoryId(body.getCategoryId());
            question.setTitle(q.getTitle());
            question.setDifficulty(q.getDifficulty());
            question.setExamPoint(q.getExamPoint());
            question.setDescription(q.getDescription());
            questionMapper.insert(question);
            tagService.syncQuestionTags(question.getQuestionId(), q.getExamPoint());
            success++;
        }
        parsed.getErrors().forEach(e -> failures.add(new BatchImportResultVO.Failure(e.getTitle(), e.getReason())));

        int total = success + failures.size();
        log.info("[题库] 批量导入完成: total={}, success={}, failed={}", total, success, failures.size());
        return new BatchImportResultVO(total, success, failures);
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private QuestionVO toListVO(Question question, List<String> tags) {
        QuestionVO vo = new QuestionVO();
        vo.setQuestionId(question.getQuestionId());
        vo.setCategoryId(question.getCategoryId());
        vo.setCategoryName(question.getCategoryName());
        vo.setTitle(question.getTitle());
        vo.setDifficulty(question.getDifficulty());
        vo.setExamPoint(question.getExamPoint());
        vo.setTags(tags == null ? List.of() : tags);
        vo.setViewCount(question.getViewCount());
        vo.setCreatedAt(question.getCreatedAt());
        return vo; // 列表不带题干
    }

    private QuestionVO toDetailVO(Question question, List<String> tags) {
        QuestionVO vo = toListVO(question, tags);
        vo.setDescription(question.getDescription());
        return vo;
    }
}
