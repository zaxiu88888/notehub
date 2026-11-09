package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.model.vo.tag.TagVO;
import com.notehub.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签 /api/tags
 */
@RestController
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> list(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(tagService.popularTags(limit));
    }
}
