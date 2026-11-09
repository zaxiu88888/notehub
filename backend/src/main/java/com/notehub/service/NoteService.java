package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.vo.note.NoteVO;

import java.util.List;

/**
 * 笔记服务
 */
public interface NoteService {

    /** 聚合列表(公开): 联表作者/题目 + 批量互动状态; 未登录 myActions 恒 false */
    PageVO<NoteVO> listNotes(NoteQueryParams params, Long currentUserId);

    NoteVO getNote(Long noteId, Long currentUserId);

    /** 发布: 一题一笔记, 已写过返回既有笔记语义由前端引导编辑 */
    default Long createNote(Long authorId, Long questionId, String content) {
        return createNote(authorId, questionId, content, null);
    }

    /** 发布(带标签): tags=null 继承题目标签 */
    Long createNote(Long authorId, Long questionId, String content, List<String> tags);

    /** 编辑: 仅作者 */
    default void updateNote(Long noteId, Long operatorId, String content) {
        updateNote(noteId, operatorId, content, null);
    }

    /** 编辑(带标签): tags=null 不修改标签 */
    void updateNote(Long noteId, Long operatorId, String content, List<String> tags);

    /** 删除: 仅作者; 连带清理 点赞/收藏夹条目/软删评论 */
    void deleteNote(Long noteId, Long operatorId);

    /** 我在某题下的笔记 ID(题目详情页跳编辑用); 无则 null */
    Long findMyNoteId(Long authorId, Long questionId);

    /** 供搜索等场景复用: 把实体列表组装为 VO(批量补互动状态/标签) */
    List<NoteVO> toVOList(List<com.notehub.model.entity.Note> notes, Long currentUserId);

    /** 收藏夹内容: 按笔记列表复用聚合逻辑 */
    PageVO<NoteVO> listNotesByIds(List<Long> noteIds, int page, int pageSize, Long currentUserId);
}
