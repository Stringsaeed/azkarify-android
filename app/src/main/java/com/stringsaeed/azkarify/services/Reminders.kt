package com.stringsaeed.azkarify.services

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stringsaeed.azkarify.MainActivity
import com.stringsaeed.azkarify.R
import com.stringsaeed.azkarify.data.appLanguage
import com.stringsaeed.azkarify.data.uiStrings
import java.util.Calendar

object Reminders {
    private fun intent(context: Context, kind: String) =
        PendingIntent.getBroadcast(
            context,
            if (kind == "morning") 1 else 2,
            Intent(context, ReminderReceiver::class.java).putExtra("kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun allowed(context: Context) =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun schedule(context: Context, kind: String) {
        val prefs = context.getSharedPreferences("azkarify", 0)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = intent(context, kind)
        if (!prefs.getBoolean("reminder.$kind.enabled", false)) {
            alarm.cancel(pending)
            return
        }
        val next =
            Calendar.getInstance().apply {
                set(
                    Calendar.HOUR_OF_DAY,
                    prefs.getInt("reminder.$kind.hour", if (kind == "morning") 8 else 18),
                )
                set(Calendar.MINUTE, prefs.getInt("reminder.$kind.minute", 0))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        if (kind != "morning" && kind != "evening") return
        val prefs = context.getSharedPreferences("azkarify", 0)
        if (!prefs.getBoolean("reminder.$kind.enabled", false)) return
        val copy = context.uiStrings(context.appLanguage())
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "azkar-reminders",
                copy.getValue("Azkar reminders"),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        if (Reminders.allowed(context)) {
            val title =
                copy.getValue(
                    if (kind == "morning") "🌅 Morning reminder" else "🌙 Evening reminder"
                )
            val body =
                copy.getValue(
                    if (kind == "morning") "Time for morning azkar" else "Time for evening azkar"
                )
            val tap =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            manager.notify(
                if (kind == "morning") 1 else 2,
                NotificationCompat.Builder(context, "azkar-reminders")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .build(),
            )
        }
        Reminders.schedule(context, kind)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        listOf("morning", "evening").forEach { Reminders.schedule(context, it) }
    }
}
