package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.user.UserVO;

/**
 * 搜索服务(I1): 笔记走 FULLTEXT(ngram), 用户走模糊匹配
 */
public interface SearchService {

    /**
     * 搜索笔记
     * @param keyword 原始关键词(内部 Jieba 预处理)
     * @param currentUserId 当前用户(用于 myActions; 可为 null)
     */
    PageVO<NoteVO> searchNotes(String keyword, int page, int pageSize, Long currentUserId);

    PageVO<UserVO> searchUsers(String keyword, int page, int pageSize);
}
