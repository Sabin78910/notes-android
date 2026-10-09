package com.sabin.notes

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class NotesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notes = NoteStore.deserialize(prefs.getString(KEY, "") ?: "").all()
        val state = WidgetState.from(notes)
        provideContent {
            GlanceTheme(colors = widgetColors()) { Content(context, state) }
        }
    }

    // Dynamic colour on Android 12+, otherwise the app's brand palette (light/dark follow the system).
    @Composable
    private fun widgetColors() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GlanceTheme.colors
        else ColorProviders(light = BrandColors.light.toLight(), dark = BrandColors.dark.toDark())

    @Composable
    private fun Content(context: Context, state: WidgetState) {
        fun intent(action: LaunchAction?) = Intent(context, MainActivity::class.java).apply {
            action?.let { putExtra(LaunchAction.EXTRA, it.wireName) }
        }
        val openApp = actionStartActivity(intent(null))
        val newNote = actionStartActivity(intent(LaunchAction.NEW_NOTE))
        val newChecklist = actionStartActivity(intent(LaunchAction.NEW_CHECKLIST))
        Scaffold(modifier = GlanceModifier.fillMaxSize().padding(4.dp)) {
            Column(GlanceModifier.fillMaxSize()) {
                Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(openApp)) {
                    Text(
                        state.preview,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                        maxLines = 5
                    )
                    state.progress?.let {
                        Text(it, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Button(context.getString(R.string.widget_new_note), newNote, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    Button(context.getString(R.string.widget_new_checklist), newChecklist, GlanceModifier.defaultWeight())
                }
            }
        }
    }

    companion object {
        /** Call after notes change so placed widgets redraw. */
        suspend fun refresh(context: Context) = NotesWidget().updateAll(context.applicationContext)
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
