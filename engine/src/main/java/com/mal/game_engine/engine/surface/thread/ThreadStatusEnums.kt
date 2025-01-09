package com.mal.game_engine.engine.surface.thread

/**
 * Общий жизненный цикл потока:
 * - RUNNING: поток выполняется
 * - SHOULD_EXIT: запрос на завершение
 * - EXITED: поток уже завершён
 */
enum class ThreadStatus {
    RUNNING,
    SHOULD_EXIT,
    EXITED
}

/**
 * Состояние паузы (нажатие home или onPause()):
 * - NOT_PAUSED: не на паузе
 * - PAUSED: на паузе
 */
enum class PauseState {
    NOT_PAUSED,
    PAUSED
}

/**
 * Состояние Surface (экрана):
 * - MISSING: Surface ещё не создан
 * - WAITING: Surface был уничтожен, ждём пересоздания
 * - VALID: Surface доступен и исправен
 * - BAD: Surface «поломан» (например, EGL-свап не удаётся)
 */
enum class SurfaceState {
    MISSING,
    WAITING,
    VALID,
    BAD
}

/**
 * Состояние EGL/GL:
 * - NONE: нет ни EGL-контекста, ни Surface
 * - CONTEXT_CREATED: есть EGL-контекст, но нет привязки к Surface
 * - SURFACE_CREATED: есть и контекст, и привязка к Surface
 * - LOST_CONTEXT: контекст потерян, нужно пересоздать
 */
enum class EglState {
    NONE,
    CONTEXT_CREATED,
    SURFACE_CREATED,
    LOST_CONTEXT
}