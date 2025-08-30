package com.notireader.app.domain.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notireader.app.domain.models.MessageModel
import com.notireader.app.domain.repository.NotiRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var notiRepository: NotiRepository

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.packageName == "com.whatsapp") {
            val title = sbn.notification.extras.getString("android.title") ?: ""
            val message = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
            val timeStamp = sbn.postTime
            val messageModel = MessageModel(
                sender = title,
                message = message,
                timestamp = timeStamp,
                isDeleted = false
            )
            CoroutineScope(Dispatchers.IO).launch {
                notiRepository.onWhatsAppNotificationReceived(messageModel)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        if (sbn?.packageName == "com.whatsapp") {
            if (reason == REASON_APP_CANCEL) {
                val title = sbn.notification.extras.getString("android.title") ?: ""
                val message = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
                val timeStamp = sbn.postTime
                CoroutineScope(Dispatchers.IO).launch {
                    notiRepository.markMessageAsDeletedByDetails(title, message, timeStamp)
                }
            }
        }
    }
}