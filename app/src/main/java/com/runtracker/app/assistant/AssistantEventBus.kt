package com.runtracker.app.assistant

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantEventBus @Inject constructor() {
    
    private val _events = MutableSharedFlow<AssistantTrigger>(extraBufferCapacity = 10)
    val events: SharedFlow<AssistantTrigger> = _events.asSharedFlow()
    
    suspend fun emit(trigger: AssistantTrigger) {
        _events.emit(trigger)
    }
    
    fun tryEmit(trigger: AssistantTrigger): Boolean {
        return _events.tryEmit(trigger)
    }
}
