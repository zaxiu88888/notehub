package com.notehub.event;

import com.notehub.mapper.MessageMapper;
import com.notehub.model.entity.Message;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 互动事件监听: 异步写站内消息
 * @TransactionalEventListener(AFTER_COMMIT): 业务事务提交成功才发消息,
 * 避免事务回滚后产生幽灵消息
 */
@Log4j2
@Component
public class InteractionEventListener {

    private final MessageMapper messageMapper;

    public InteractionEventListener(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInteraction(InteractionEvent event) {
        try {
            Message message = new Message();
            message.setReceiverId(event.getReceiverId());
            message.setSenderId(event.getSenderId());
            message.setType(event.getType());
            message.setTargetType(event.getTargetType());
            message.setTargetId(event.getTargetId());
            message.setContent(event.getContent());
            messageMapper.insert(message);
            log.debug("[消息] 已投递: receiver={}, type={}, target={}",
                    event.getReceiverId(), event.getType(), event.getTargetId());
        } catch (Exception e) {
            // 消息失败不影响主流程(点赞/评论已成功), 记日志即可
            log.error("[消息] 投递失败: receiver={}, target={}, err={}",
                    event.getReceiverId(), event.getTargetId(), e.getMessage(), e);
        }
    }
}
