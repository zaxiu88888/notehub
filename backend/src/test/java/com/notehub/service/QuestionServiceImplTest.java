package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageVO;
import com.notehub.mapper.CategoryMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.dto.question.CreateQuestionBatchBody;
import com.notehub.model.dto.question.QuestionQueryParam;
import com.notehub.model.entity.Category;
import com.notehub.model.entity.Question;
import com.notehub.model.vo.question.BatchImportResultVO;
import com.notehub.model.vo.question.QuestionVO;
import com.notehub.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceImplTest {

    private QuestionMapper questionMapper;
    private CategoryMapper categoryMapper;
    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        questionService = new QuestionServiceImpl(questionMapper, categoryMapper);
    }

    private Category category() {
        Category c = new Category();
        c.setCategoryId(10L);
        c.setName("哈希表");
        c.setParentCategoryId(1L);
        return c;
    }

    private Question question() {
        Question q = new Question();
        q.setQuestionId(2L);
        q.setCategoryId(10L);
        q.setTitle("两数之和");
        q.setDifficulty(1);
        q.setExamPoint("数组;哈希表");
        q.setViewCount(5);
        q.setCategoryName("哈希表");
        return q;
    }

    @Test
    @DisplayName("列表: 分页收敛 + 不泄露题干(description=null)")
    void listQuestionsPagination() {
        when(questionMapper.countByQuery(any())).thenReturn(12L);
        when(questionMapper.listByQuery(any(), anyInt(), anyInt())).thenReturn(List.of(question()));

        QuestionQueryParam param = new QuestionQueryParam();
        param.setPage(0);
        param.setPageSize(999);
        PageVO<QuestionVO> page = questionService.listQuestions(param);

        assertEquals(1, page.getPage());
        assertEquals(50, page.getPageSize());
        assertEquals(12, page.getTotal());
        assertNull(page.getList().get(0).getDescription());
        assertEquals("哈希表", page.getList().get(0).getCategoryName());
    }

    @Test
    @DisplayName("详情: 浏览数自增; 未登录 myNoteId 为 null")
    void detailIncrementsView() {
        when(questionMapper.selectById(2L)).thenReturn(question());

        QuestionVO vo = questionService.getQuestionDetail(2L, null);

        verify(questionMapper).incrementViewCount(2L);
        assertNull(vo.getMyNoteId());
        assertEquals("两数之和", vo.getTitle());
    }

    @Test
    @DisplayName("详情: 题目不存在 → NOT_FOUND, 且不自增浏览数")
    void detailNotFound() {
        when(questionMapper.selectById(404L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> questionService.getQuestionDetail(404L, null));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
        verify(questionMapper, never()).incrementViewCount(anyLong());
    }

    @Test
    @DisplayName("创建: 分类必须存在")
    void createRequiresExistingCategory() {
        when(categoryMapper.selectById(10L)).thenReturn(category());
        doAnswer(inv -> {
            inv.getArgument(0, Question.class).setQuestionId(300L);
            return 1;
        }).when(questionMapper).insert(any(Question.class));
        when(questionMapper.selectById(300L)).thenReturn(question());

        questionService.create(10L, "新题", 1, null, null);
        verify(questionMapper).insert(any(Question.class));

        when(categoryMapper.selectById(999L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> questionService.create(999L, "新题", 1, null, null));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
    }

    @Test
    @DisplayName("删除: 题下有笔记 → BAD_PARAM 拒绝")
    void deleteBlockedByNotes() {
        when(questionMapper.selectById(2L)).thenReturn(question());
        when(questionMapper.countNotesByQuestionId(2L)).thenReturn(3L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> questionService.delete(2L));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());
        verify(questionMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("批量导入: 正常题入库, 缺难度进 failures, 明细准确")
    void batchImportMixed() {
        when(categoryMapper.selectById(10L)).thenReturn(category());
        doAnswer(inv -> {
            inv.getArgument(0, Question.class).setQuestionId(100L);
            return 1;
        }).when(questionMapper).insert(any(Question.class));

        CreateQuestionBatchBody body = new CreateQuestionBatchBody();
        body.setCategoryId(10L);
        body.setMarkdown("""
                # 好题
                难度：简单
                题干。

                # 坏题
                忘了写难度。
                """);

        BatchImportResultVO result = questionService.batchImport(body);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(1, result.getFailures().size());
        assertEquals("坏题", result.getFailures().get(0).getTitle());

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper, times(1)).insert(captor.capture());
        assertEquals("好题", captor.getValue().getTitle());
        assertEquals(1, captor.getValue().getDifficulty());
    }

    @Test
    @DisplayName("批量导入: 分类不存在 → NOT_FOUND 且不解析")
    void batchImportCategoryMissing() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        CreateQuestionBatchBody body = new CreateQuestionBatchBody();
        body.setCategoryId(999L);
        body.setMarkdown("# 任意\n难度：简单\n");

        BusinessException e = assertThrows(BusinessException.class,
                () -> questionService.batchImport(body));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
        verify(questionMapper, never()).insert(any(Question.class));
    }
}
