package com.magtek

import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IData

// Callback interface for processEvent
interface ProcessEventCallback {
    fun invoke(eventType: EventType?, data: IData?)
} 