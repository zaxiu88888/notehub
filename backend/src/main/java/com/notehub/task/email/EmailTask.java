package com.notehub.task.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件发送任务: 序列化为 JSON 入 Redis 队列
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTask {

    private String email;

    private String code;

    private long timestamp;
}
