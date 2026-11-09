package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.PageVO;
import com.notehub.mapper.CollectionMapper;
import com.notehub.mapper.CollectionNoteMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Collection;
import com.notehub.model.entity.CollectionNote;
import com.notehub.model.entity.Note;
import com.notehub.model.vo.collection.CollectionVO;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.service.CollectionService;
import com.notehub.service.NoteService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final CollectionNoteMapper collectionNoteMapper;
    private final NoteMapper noteMapper;
    private final NoteService noteService;

    public CollectionServiceImpl(CollectionMapper collectionMapper,
                                 CollectionNoteMapper collectionNoteMapper,
                                 NoteMapper noteMapper, NoteService noteService) {
        this.collectionMapper = collectionMapper;
        this.collectionNoteMapper = collectionNoteMapper;
        this.noteMapper = noteMapper;
        this.noteService = noteService;
    }

    @Override
    public List<CollectionVO> listMyCollections(Long userId) {
        Map<Long, Long> counts = collectionMapper.countGroupByCollection(userId).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("collectionId")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue()));
        return collectionMapper.listByCreator(userId).stream().map(c -> {
            CollectionVO vo = new CollectionVO();
            vo.setCollectionId(c.getCollectionId());
            vo.setName(c.getName());
            vo.setDescription(c.getDescription());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setNoteCount(counts.getOrDefault(c.getCollectionId(), 0L));
            return vo;
        }).toList();
    }

    @Override
    public CollectionVO createCollection(Long userId, String name, String description) {
        Collection collection = new Collection();
        collection.setName(name);
        collection.setDescription(description);
        collection.setCreatorId(userId);
        collectionMapper.insert(collection);

        CollectionVO vo = new CollectionVO();
        vo.setCollectionId(collection.getCollectionId());
        vo.setName(name);
        vo.setDescription(description);
        vo.setNoteCount(0);
        vo.setCreatedAt(collection.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional
    public void deleteCollection(Long collectionId, Long userId) {
        requireOwnCollection(collectionId, userId);
        // 必须先取条目再删(顺序错误会导致收藏数永不递减)
        List<Long> noteIds = collectionNoteMapper.selectNoteIdsByCollection(collectionId);
        collectionNoteMapper.deleteByCollectionId(collectionId);
        for (Long noteId : noteIds) {
            if (!hasOtherCollectionNote(userId, noteId)) { // 该用户其他夹还有则不递减
                noteMapper.adjustCollectCount(noteId, -1);
            }
        }
        collectionMapper.deleteById(collectionId);
        log.info("[收藏夹] 删除(连带条目): collectionId={}, user={}, notes={}", collectionId, userId, noteIds.size());
    }

    /** 该用户的其它收藏夹是否也收了这篇笔记 */
    private boolean hasOtherCollectionNote(Long userId, Long noteId) {
        return collectionNoteMapper.countByCreatorAndNote(userId, noteId) > 0;
    }

    @Override
    @Transactional
    public int collectNotes(Long userId, Long collectionId, List<Long> noteIds) {
        requireOwnCollection(collectionId, userId);
        int added = 0;
        for (Long noteId : noteIds) {
            Note note = noteMapper.selectById(noteId);
            if (note == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在: " + noteId);
            }
            CollectionNote cn = new CollectionNote();
            cn.setCollectionId(collectionId);
            cn.setNoteId(noteId);
            if (collectionNoteMapper.insert(cn) > 0) { // INSERT IGNORE 幂等
                noteMapper.adjustCollectCount(noteId, 1);
                added++;
            }
        }
        return added;
    }

    @Override
    public PageVO<NoteVO> listCollectionNotes(Long userId, Long collectionId, int page, int pageSize) {
        requireOwnCollection(collectionId, userId);
        List<Long> noteIds = collectionNoteMapper.selectNoteIdsByCollection(collectionId);
        return noteService.listNotesByIds(noteIds, page, pageSize, userId);
    }

    @Override
    @Transactional
    public void removeNote(Long userId, Long collectionId, Long noteId) {
        requireOwnCollection(collectionId, userId);
        int deleted = collectionNoteMapper.delete(collectionId, noteId);
        if (deleted > 0 && !hasOtherCollectionNote(userId, noteId)) {
            noteMapper.adjustCollectCount(noteId, -1); // 用户其他夹还收着就不算取消收藏
        }
    }

    private Collection requireOwnCollection(Long collectionId, Long userId) {
        Collection collection = collectionMapper.selectById(collectionId);
        if (collection == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "收藏夹不存在");
        }
        if (!collection.getCreatorId().equals(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只能操作自己的收藏夹");
        }
        return collection;
    }
}
