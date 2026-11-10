package com.notehub.mapper;

import com.notehub.model.dto.note.DailyCountRow;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.dto.note.NoteTotalsRow;
import com.notehub.model.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    int updateContent(@Param("noteId") Long noteId, @Param("content") String content,
                      @Param("searchVector") String searchVector);

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

    // ---- I1 搜索 ----
    /** FULLTEXT(ngram) 搜索, 按相关度排序 */
    List<Note> searchByKeyword(@Param("keyword") String keyword,
                               @Param("offset") int offset, @Param("limit") int limit);

    long countByKeyword(@Param("keyword") String keyword);

    // ---- I2 数据可视化 ----

    /** 某用户一年内按天聚合的笔记数(热力图缓存回填) */
    List<DailyCountRow> countDailyByAuthor(@Param("authorId") Long authorId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);

    /** 热度榜(累计): 直接使用 notes 上的计数列 */
    List<Note> selectRanklistAll(@Param("limit") int limit);

    /** 热度榜(窗口): 聚合 since 之后的每日互动统计 */
    List<Note> selectRanklistSince(@Param("since") LocalDate since, @Param("limit") int limit);

    /** 个人累计指标一条 SQL 拿齐 */
    NoteTotalsRow selectNoteTotals(@Param("authorId") Long authorId);

    /** 有笔记产出的日期(倒序, 连续提交计算用) */
    List<LocalDate> selectActiveDates(@Param("authorId") Long authorId);

    /** 某时间之后新增的笔记数(本月/本周卡片) */
    long countNotesSince(@Param("authorId") Long authorId, @Param("since") LocalDateTime since);

    /** 导出全部笔记(按时间正序) */
    List<Note> selectAllByAuthor(@Param("authorId") Long authorId);

    /** I5 上传清理: 正文引用该图片的笔记数 */
    long countByContentContaining(@Param("url") String url);
}
