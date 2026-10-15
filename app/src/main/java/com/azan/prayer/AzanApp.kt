package com.azan.prayer

import android.app.Application
import com.azan.prayer.alarm.AlarmScheduler
import com.azan.prayer.alarm.Notifications
import com.azan.prayer.sync.SyncWorker

class AzanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        AlarmScheduler.rescheduleAll(this)
        SyncWorker.schedulePeriodic(this)
    }
}
