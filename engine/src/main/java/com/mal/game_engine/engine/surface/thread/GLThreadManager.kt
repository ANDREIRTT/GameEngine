package com.mal.game_engine.engine.surface.thread

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * Менеджер для синхронизации GLThread’ов.
 */
internal object GLThreadManager {
    val lock = ReentrantLock()
    val condition: Condition = lock.newCondition()

    fun threadExiting(thread: GLThread) {
        lock.lock()
        try {
            condition.signalAll()
        } finally {
            lock.unlock()
        }
    }
}