package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.CategoryMapper;
import com.notehub.mapper.QuestionListItemMapper;
import com.notehub.mapper.QuestionListMapper;
import com.notehub.mapper.QuestionMapper;
import com.notehub.model.entity.Category;
import com.notehub.model.entity.Question;
import com.notehub.model.entity.QuestionList;
import com.notehub.model.entity.QuestionListItem;
import com.notehub.model.vo.category.CategoryVO;
import com.notehub.model.vo.questionList.QuestionListVO;
import com.notehub.model.vo.questionListItem.QuestionListItemVO;
import com.notehub.service.impl.CategoryServiceImpl;
import com.notehub.service.impl.QuestionListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryAndQuestionListServiceTest {

    private CategoryMapper categoryMapper;
    private CategoryServiceImpl categoryService;

    private QuestionListMapper questionListMapper;
    private QuestionListItemMapper questionListItemMapper;
    private QuestionMapper questionMapper;
    private QuestionListServiceImpl questionListService;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(CategoryMapper.class);
        categoryService = new CategoryServiceImpl(categoryMapper, com.notehub.TestCaches.passThrough());

        questionListMapper = mock(QuestionListMapper.class);
        questionListItemMapper = mock(QuestionListItemMapper.class);
        questionMapper = mock(QuestionMapper.class);
        questionListService = new QuestionListServiceImpl(
                questionListMapper, questionListItemMapper, questionMapper);
    }

    private Category category(long id, String name, Long parent) {
        Category c = new Category();
        c.setCategoryId(id);
        c.setName(name);
        c.setParentCategoryId(parent);
        return c;
    }

    // ---------- 分类树 ----------

    @Test
    @DisplayName("分类树: 顶级保序, 子分类挂到对应父节点")
    void categoryTree() {
        when(categoryMapper.selectAll()).thenReturn(List.of(
                category(2, "哈希表", 1L),
                category(1, "算法", null),
                category(3, "链表", 1L),
                category(4, "操作系统", null)));

        List<CategoryVO> tree = categoryService.getCategoryTree();

        assertEquals(2, tree.size()); // 两个顶级
        assertEquals("算法", tree.get(0).getName()); // 保序: id=1 在前
        assertEquals(2, tree.get(0).getChildren().size());
        assertEquals("哈希表", tree.get(0).getChildren().get(0).getName());
        assertEquals(0, tree.get(1).getChildren().size());
    }

    @Test
    @DisplayName("创建分类: 父分类必须是顶级, 否则 BAD_PARAM")
    void createCategoryTwoLevelOnly() {
        when(categoryMapper.selectById(9L)).thenReturn(category(9, "子分类", 1L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> categoryService.create("非法", 9L));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());

        when(categoryMapper.selectById(1L)).thenReturn(category(1, "顶级", null));
        doAnswer(inv -> {
            inv.getArgument(0, Category.class).setCategoryId(50L);
            return 1;
        }).when(categoryMapper).insert(any(Category.class));
        when(categoryMapper.selectById(50L)).thenReturn(category(50, "合法子分类", 1L));

        CategoryVO vo = categoryService.create("合法子分类", 1L);
        assertEquals(1L, vo.getParentCategoryId());
    }

    @Test
    @DisplayName("删除分类: 有子分类或有题目均拒绝")
    void deleteCategoryGuards() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1, "顶级", null));

        when(categoryMapper.countChildren(1L)).thenReturn(2L);
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> categoryService.delete(1L));
        assertEquals(ErrorCode.BAD_PARAM, e1.getErrorCode());

        when(categoryMapper.countChildren(1L)).thenReturn(0L);
        when(categoryMapper.countQuestions(1L)).thenReturn(5L);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> categoryService.delete(1L));
        assertEquals(ErrorCode.BAD_PARAM, e2.getErrorCode());

        when(categoryMapper.countQuestions(1L)).thenReturn(0L);
        categoryService.delete(1L);
        verify(categoryMapper).deleteById(1L);
    }

    // ---------- 题单 ----------

    private QuestionList questionList() {
        QuestionList l = new QuestionList();
        l.setQuestionListId(1L);
        l.setName("新手热身 10 题");
        l.setType(1);
        return l;
    }

    private QuestionListItem item(long listId, long questionId, int rank) {
        QuestionListItem i = new QuestionListItem();
        i.setQuestionListId(listId);
        i.setQuestionId(questionId);
        i.setSortRank(rank);
        i.setTitle("题" + questionId);
        i.setDifficulty(1);
        return i;
    }

    @Test
    @DisplayName("题单列表: 附带每单题目数(缺省 0)")
    void questionListsWithCounts() {
        when(questionListMapper.selectAll()).thenReturn(List.of(questionList()));
        when(questionListItemMapper.countGroupByListId()).thenReturn(
                List.of(Map.of("questionListId", 1L, "cnt", 6L)));

        List<QuestionListVO> lists = questionListService.listQuestionLists();
        assertEquals(1, lists.size());
        assertEquals(6, lists.get(0).getQuestionCount());
    }

    @Test
    @DisplayName("条目进度: 登录用户 hasMyNote 批量标记; 未登录恒 false")
    void itemsWithMyNoteProgress() {
        when(questionListMapper.selectById(1L)).thenReturn(questionList());
        when(questionListItemMapper.listByQuestionListId(1L)).thenReturn(List.of(
                item(1L, 101L, 1), item(1L, 102L, 2)));
        when(questionListItemMapper.selectQuestionIdsWithNote(eq(7L), anyList()))
                .thenReturn(List.of(101L));

        List<QuestionListItemVO> loggedIn = questionListService.listItems(1L, 7L);
        assertTrue(loggedIn.get(0).isHasMyNote());
        assertEquals(false, loggedIn.get(1).isHasMyNote());

        List<QuestionListItemVO> anonymous = questionListService.listItems(1L, null);
        assertEquals(false, anonymous.get(0).isHasMyNote());
    }

    @Test
    @DisplayName("添加条目: 重复添加 → DUPLICATED; 题目不存在 → NOT_FOUND")
    void addItemGuards() {
        when(questionListMapper.selectById(1L)).thenReturn(questionList());
        when(questionMapper.selectById(101L)).thenReturn(new Question());
        when(questionListItemMapper.selectOne(1L, 101L)).thenReturn(new QuestionListItem());

        BusinessException e = assertThrows(BusinessException.class,
                () -> questionListService.addItem(1L, 101L, null));
        assertEquals(ErrorCode.DUPLICATED, e.getErrorCode());

        when(questionListItemMapper.selectOne(1L, 101L)).thenReturn(null);
        when(questionMapper.selectById(404L)).thenReturn(null);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> questionListService.addItem(1L, 404L, null));
        assertEquals(ErrorCode.NOT_FOUND, e2.getErrorCode());
    }

    @Test
    @DisplayName("添加条目: 不传 rank 时追加到末尾(空题单=1, 有条目=max+1)")
    void addItemAppendsRank() {
        when(questionListMapper.selectById(anyLong())).thenReturn(questionList());
        when(questionMapper.selectById(anyLong())).thenReturn(new Question());
        when(questionListItemMapper.selectOne(anyLong(), anyLong())).thenReturn(null);

        // 空题单 → rank 1
        when(questionListItemMapper.selectMaxSortRank(1L)).thenReturn(null);
        questionListService.addItem(1L, 201L, null);
        org.mockito.ArgumentCaptor<QuestionListItem> c1 =
                org.mockito.ArgumentCaptor.forClass(QuestionListItem.class);
        verify(questionListItemMapper).insert(c1.capture());
        assertEquals(1, c1.getValue().getSortRank());

        // 已有 max=6 → rank 7
        when(questionListItemMapper.selectMaxSortRank(2L)).thenReturn(6);
        questionListService.addItem(2L, 202L, null);
        org.mockito.ArgumentCaptor<QuestionListItem> c2 =
                org.mockito.ArgumentCaptor.forClass(QuestionListItem.class);
        verify(questionListItemMapper, org.mockito.Mockito.times(2)).insert(c2.capture());
        assertEquals(7, c2.getValue().getSortRank());

        // 显式指定 → 透传
        questionListService.addItem(2L, 203L, 3);
        org.mockito.ArgumentCaptor<QuestionListItem> c3 =
                org.mockito.ArgumentCaptor.forClass(QuestionListItem.class);
        verify(questionListItemMapper, org.mockito.Mockito.times(3)).insert(c3.capture());
        assertEquals(3, c3.getValue().getSortRank());
    }

    @Test
    @DisplayName("批量排序: 含题单外题目 → BAD_PARAM; 合法则按顺序写 1..n")
    void sortItemsGuards() {
        when(questionListMapper.selectById(1L)).thenReturn(questionList());
        when(questionListItemMapper.listByQuestionListId(1L)).thenReturn(List.of(
                item(1L, 101L, 1), item(1L, 102L, 2)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> questionListService.sortItems(1L, List.of(101L, 999L)));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());

        questionListService.sortItems(1L, List.of(102L, 101L));
        verify(questionListItemMapper).updateSortRank(1L, 102L, 1);
        verify(questionListItemMapper).updateSortRank(1L, 101L, 2);
    }

    @Test
    @DisplayName("删除题单: 连带清空条目")
    void deleteListCascadesItems() {
        when(questionListMapper.selectById(1L)).thenReturn(questionList());
        questionListService.delete(1L);
        verify(questionListItemMapper).deleteByQuestionListId(1L);
        verify(questionListMapper).deleteById(1L);
    }
}
