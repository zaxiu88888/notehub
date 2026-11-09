package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageUtils;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.user.UserVO;
import com.notehub.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索 /api/search (I1)
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final RequestContext requestContext;

    public SearchController(SearchService searchService, RequestContext requestContext) {
        this.searchService = searchService;
        this.requestContext = requestContext;
    }

    @GetMapping("/notes")
    public ApiResponse<PageVO<NoteVO>> searchNotes(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long currentUserId = requestContext.isLogin() ? requestContext.getUserId() : null;
        return ApiResponse.ok(searchService.searchNotes(keyword, page, pageSize, currentUserId));
    }

    @GetMapping("/users")
    public ApiResponse<PageVO<UserVO>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(searchService.searchUsers(keyword,
                PageUtils.clampPage(page), PageUtils.clampPageSize(pageSize)));
    }
}
