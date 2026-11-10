package com.notehub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfterCommitTest {

    @Test
    @DisplayName("无事务: 立即执行")
    void runsImmediatelyWithoutTransaction() {
        AtomicInteger runs = new AtomicInteger();
        AfterCommit.run(runs::incrementAndGet);
        assertEquals(1, runs.get());
    }

    @Test
    @DisplayName("有事务: 提交后才执行")
    void runsAfterCommitWhenSynchronizationActive() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            AtomicInteger runs = new AtomicInteger();
            AfterCommit.run(runs::incrementAndGet);
            assertEquals(0, runs.get());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, runs.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
