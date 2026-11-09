package com.notehub.mapper;

import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper {

    Note selectById(@Param("noteId") Long noteId);

    /** 批量取笔记(收藏夹内容用, 防 N+1) */
    List<Note> selectByIds(@Param("noteIds") List<Long> noteIds);

    /** 聚合列表: 联表作者/题目摘要, 按筛选+排序分页 (核心查询, 防 N+1 的第一步) */
    List<Note> listByQuery(@Param("param") NoteQueryParams param,
                           @Param("offset") int offset, @Param("limit") int limit);

    long countByQuery(@Param("param") NoteQueryParams param);

    /** 我在某题下的笔记(一题一笔记约束由应用层+查询保证) */
    Note selectByAuthorAndQuestion(@Param("authorId") Long authorId,
                                   @Param("questionId") Long questionId);

    int insert(Note note);

    int updateContent(@Param("noteId") Long noteId, @Param("content") String content);

    int deleteById(@Param("noteId") Long noteId);

    /** 原子计数: 1=增 -1=减, floor(0) 保护 */
    int adjustLikeCount(@Param("noteId") Long noteId, @Param("delta") int delta);

    int adjustCommentCount(@Param("noteId") Long noteId, @Param("delta") int delta);

    int adjustCollectCount(@Param("noteId") Long noteId, @Param("delta") int delta);

    /** 用户的笔记总数(个人主页统计) */
    long countByAuthor(@Param("authorId") Long authorId);

    /** 某题目下的笔记总数 */
    long countByQuestion(@Param("questionId") Long questionId);

    /** 批量取一组用户点赞过的笔记 ID(聚合列表第二步) */
    List<Long> selectLikedNoteIds(@Param("userId") Long userId, @Param("noteIds") List<Long> noteIds);

    /** 批量取一组用户收藏过的笔记 ID(聚合列表第二步) */
    List<Long> selectCollectedNoteIds(@Param("userId") Long userId, @Param("noteIds") List<Long> noteIds);

    // ---- 统计看板聚合(每日快照) ----
    long countTodayNotes();

    /** 当日提交笔记的人数(作者去重) */
    long countTodayAuthors();

    long countTotalNotes();
}
