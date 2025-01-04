package com.mal.game_engine.engine.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal const val DISPATCHER_IO = "DISPATCHER_IO"
internal const val DISPATCHER_MAIN = "DISPATCHER_MAIN"
internal const val DISPATCHER_DEFAULT = "DISPATCHER_MAIN"

internal val coreModule = module {
    single<CoroutineDispatcher>(named(DISPATCHER_IO)) {
        Dispatchers.IO
    }
    single<CoroutineDispatcher>(named(DISPATCHER_MAIN)) {
        Dispatchers.Main
    }
    single<CoroutineDispatcher>(named(DISPATCHER_DEFAULT)) {
        Dispatchers.Default
    }
}