package com.mal.game_engine.engine.surface.thread

data class GLThreadState(
    val threadStatus: ThreadStatus,
    val pauseState: PauseState,
    val surfaceState: SurfaceState,
    val eglState: EglState
) {
    val needToExit = threadStatus == ThreadStatus.SHOULD_EXIT
            || threadStatus == ThreadStatus.EXITED


}
