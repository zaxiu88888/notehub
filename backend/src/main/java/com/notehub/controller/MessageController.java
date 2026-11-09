package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.message.MessageQueryParams;
import com.notehub.model.vo.message.MessageVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.MessageService;
import com.notehub.service.UnreadPushService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 站内消息 /api/messages (全部登录态)
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UnreadPushService unreadPushService;
    private final RequestContext requestContext;

    public MessageController(MessageService messageService, UnreadPushService unreadPushService,
                             RequestContext requestContext) {
        this.messageService = messageService;
        this.unreadPushService = unreadPushService;
        this.requestContext = requestContext;
    }

    @GetMapping
    @NeedLogin
    public ApiResponse<PageVO<MessageVO>> list(MessageQueryParams params) {
        return ApiResponse.ok(messageService.listMessages(requestContext.requireUserId(), params));
    }

    @GetMapping("/unread/count")
    @NeedLogin
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(messageService.unreadCount(requestContext.requireUserId()));
    }

    @PatchMapping("/{messageId}/read")
    @NeedLogin
    public ApiResponse<Void> markRead(@PathVariable Long messageId) {
        Long userId = requestContext.requireUserId();
        messageService.markRead(userId, messageId);
        unreadPushService.pushUnread(userId);
        return ApiResponse.ok();
    }

    @PatchMapping("/all/read")
    @NeedLogin
    public ApiResponse<Void> markAllRead() {
        Long userId = requestContext.requireUserId();
        messageService.markAllRead(userId);
        unreadPushService.pushUnread(userId);
        return ApiResponse.ok();
    }

    @PatchMapping("/batch/read")
    @NeedLogin
    public ApiResponse<Void> markBatchRead(@Valid @RequestBody BatchReadBody body) {
        Long userId = requestContext.requireUserId();
        messageService.markBatchRead(userId, body.getMessageIds());
        unreadPushService.pushUnread(userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{messageId}")
    @NeedLogin
    public ApiResponse<Void> delete(@PathVariable Long messageId) {
        Long userId = requestContext.requireUserId();
        messageService.deleteMessage(userId, messageId);
        unreadPushService.pushUnread(userId);
        return ApiResponse.ok();
    }

    @Data
    public static class BatchReadBody {
        @NotEmpty(message = "messageIds 不能为空")
        private List<Long> messageIds;
    }
}
