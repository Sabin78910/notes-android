package com.sabin.notes

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal const val KEY_REMINDERS = "reminders_enabled"
private const val CHANNEL = "reminders"
const val EXTRA_NOTE_ID = "com.sabin.notes.NOTE_ID"

/** Inexact alarms only, so no exact-alarm permission is needed. */
class AndroidAlarms(private val context: Context) : AlarmGateway {
    private val manager = context.getSystemService(AlarmManager::class.java)

    private fun pending(noteId: Long) = PendingIntent.getBroadcast(
        context, noteId.toInt(), Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_NOTE_ID, noteId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun set(noteId: Long, at: Long) = manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(noteId))
    override fun cancel(noteId: Long) = manager.cancel(pending(noteId))
}

object ReminderPermission {
    fun granted(context: Context) = android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (id < 0 || !prefs.getBoolean(KEY_REMINDERS, false) || !ReminderPermission.granted(context)) return
        val note = NoteStore.deserialize(prefs.getString(KEY, "") ?: "").all().firstOrNull { it.id == id } ?: return
        if (note.trashedAt != null) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, id.toInt(), Intent(context, MainActivity::class.java).putExtra(EXTRA_NOTE_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(id.toInt(), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Note reminder")
            .setContentText(note.text.lineSequence().first().take(100)).setContentIntent(open).setAutoCancel(true).build())
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMINDERS, false)) return
        ReminderScheduler(AndroidAlarms(context)).rescheduleAll(NoteStore.deserialize(prefs.getString(KEY, "") ?: "").all())
    }
}
