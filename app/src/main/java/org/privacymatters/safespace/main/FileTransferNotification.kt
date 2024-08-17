package org.privacymatters.safespace.main

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.privacymatters.safespace.R

/**
 * Class to handle file transfer notifications
 */
class FileTransferNotification(private val context: Context, private val notificationId: Int) {

    companion object {
        const val CHANNEL_ID = "file_transfer_channel_id"
        const val CHANNEL_NAME = "File Transfer Notifications"
    }

    /**
     * Enum class for notification types
     */
    enum class NotificationType { Export, Import }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    /**
     * Show a progress notification
     * @param fileName Name of the file being copied
     * @param progress Current progress
     * @param type Type of notification
     */
    fun showProgressNotification(fileName: String, progress: Int, type: NotificationType) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val (icon, title) = when (type) {
            NotificationType.Export -> Pair(
                android.R.drawable.stat_sys_upload,
                "${context.getString(R.string.notif_export)} $fileName"
            )

            NotificationType.Import -> Pair(
                android.R.drawable.stat_sys_download,
                "${context.getString(R.string.notif_import)} $fileName"
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("$progress% / 100%")
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Show a success notification
     * @param fileName Name of the file copied
     * @param type Type of notification
     */
    fun showSuccessNotification(
        fileName: String,
        type: NotificationType,
        isBackupFile: Boolean = false
    ) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainnActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val (icon, title) = when (type) {
            NotificationType.Export -> android.R.drawable.stat_sys_upload to
                    if (isBackupFile) context.getString(R.string.notif_exp_bckp_success) else context.getString(
                        R.string.notif_exp_file_success
                    )

            NotificationType.Import -> android.R.drawable.stat_sys_download to
                    if (isBackupFile) context.getString(R.string.notif_imp_bckp_success) else context.getString(
                        R.string.notif_imp_file_success
                    )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("$fileName ${context.getString(R.string.notif_exp_file_caption_s)}")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.cancel(notificationId)
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Show a failure notification
     * @param fileName Name of the file being copied
     * @param exception Exception that caused the failure
     */
    fun showFailureNotification(fileName: String, exception: Throwable) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(context, MainnActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("${context.getString(R.string.notif_exp_file_caption_e)} $fileName")
            .setContentText(exception.message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.cancel(notificationId)
        notificationManager.notify(notificationId, builder.build())
    }
}