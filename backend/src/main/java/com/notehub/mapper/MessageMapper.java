package com.notehub.mapper;

import com.notehub.model.dto.message.MessageQueryParams;
import com.notehub.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    /** 消息列表(联表发送者摘要, 按类型筛选分页) */
    List<Message> listByQuery(@Param("param") MessageQueryParams param,
                              @Param("offset") int offset, @Param("limit") int limit);

    long countByQuery(@Param("param") MessageQueryParams param);

    /** 各类型未读数: [{type, cnt}] */
    List<Map<String, Object>> countUnreadGroupByType(@Param("receiverId") Long receiverId);

    long countUnread(@Param("receiverId") Long receiverId);

    int insert(Message message);

    /** 单条已读(归属校验) */
    int markRead(@Param("messageId") Long messageId, @Param("receiverId") Long receiverId);

    int markAllRead(@Param("receiverId") Long receiverId);

    int markBatchRead(@Param("receiverId") Long receiverId, @Param("messageIds") List<Long> messageIds);

    int delete(@Param("messageId") Long messageId, @Param("receiverId") Long receiverId);
}
