package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.vo.collection.CollectionVO;
import com.notehub.model.vo.note.NoteVO;

import java.util.List;

/**
 * 收藏夹服务
 */
public interface CollectionService {

    /** 我的收藏夹列表(含每个夹内数量) */
    List<CollectionVO> listMyCollections(Long userId);

    CollectionVO createCollection(Long userId, String name, String description);

    /** 删除收藏夹(连带条目); 仅创建者 */
    void deleteCollection(Long collectionId, Long userId);

    /** 批量收藏笔记到夹(幂等); 返回实际新增收藏数 */
    int collectNotes(Long userId, Long collectionId, List<Long> noteIds);

    /** 收藏夹内笔记(复用笔记聚合) */
    PageVO<NoteVO> listCollectionNotes(Long userId, Long collectionId, int page, int pageSize);

    /** 从夹中移除 */
    void removeNote(Long userId, Long collectionId, Long noteId);
}
