package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.collection.BatchCollectBody;
import com.notehub.model.dto.collection.CreateCollectionBody;
import com.notehub.model.vo.collection.CollectionVO;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.CollectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 收藏夹 /api/collections, /api/collection-notes
 */
@RestController
@RequestMapping("/api")
@Validated
public class CollectionController {

    private final CollectionService collectionService;
    private final RequestContext requestContext;

    public CollectionController(CollectionService collectionService, RequestContext requestContext) {
        this.collectionService = collectionService;
        this.requestContext = requestContext;
    }

    @GetMapping("/collections")
    @NeedLogin
    public ApiResponse<List<CollectionVO>> listMine() {
        return ApiResponse.ok(collectionService.listMyCollections(requestContext.requireUserId()));
    }

    @PostMapping("/collections")
    @NeedLogin
    public ApiResponse<CollectionVO> create(@Valid @RequestBody CreateCollectionBody body) {
        return ApiResponse.ok("收藏夹已创建",
                collectionService.createCollection(requestContext.requireUserId(),
                        body.getName(), body.getDescription()));
    }

    @DeleteMapping("/collections/{collectionId}")
    @NeedLogin
    public ApiResponse<Void> delete(@PathVariable Long collectionId) {
        collectionService.deleteCollection(collectionId, requestContext.requireUserId());
        return ApiResponse.ok();
    }

    /** 批量收藏(幂等): 返回实际新增数 */
    @PostMapping("/collections/batch")
    @NeedLogin
    public ApiResponse<Map<String, Integer>> batchCollect(@Valid @RequestBody BatchCollectBody body) {
        int added = collectionService.collectNotes(requestContext.requireUserId(),
                body.getCollectionId(), body.getNoteIds());
        return ApiResponse.ok(Map.of("added", added));
    }

    @GetMapping("/collection-notes")
    @NeedLogin
    public ApiResponse<PageVO<NoteVO>> listNotes(
            @RequestParam @NotNull(message = "collectionId 不能为空") Long collectionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(collectionService.listCollectionNotes(
                requestContext.requireUserId(), collectionId, page, pageSize));
    }

    @DeleteMapping("/collection-notes/{collectionId}/{noteId}")
    @NeedLogin
    public ApiResponse<Void> removeNote(@PathVariable Long collectionId, @PathVariable Long noteId) {
        collectionService.removeNote(requestContext.requireUserId(), collectionId, noteId);
        return ApiResponse.ok();
    }
}
