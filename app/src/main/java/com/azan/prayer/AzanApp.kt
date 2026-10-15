package com.azan.prayer

import android.app.Application
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications

class AzanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        AlarmScheduler.rescheduleAll(this)
    }
}
