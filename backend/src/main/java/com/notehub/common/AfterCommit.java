package com.notehub.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后执行副作用(缓存失效/文件清理); 无事务时立即执行(单测场景)
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
