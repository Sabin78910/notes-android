package com.sabin.notes

import android.app.Activity
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory

private const val KEY_FIRST_LAUNCH = "review_first_launch"
private const val KEY_LAST_ASKED = "review_last_asked"

class PrefsReviewState(private val prefs: SharedPreferences) : ReviewPrompt.State {
    override var firstLaunchAt: Long?
        get() = prefs.getLong(KEY_FIRST_LAUNCH, -1L).takeIf { it >= 0 }
        set(v) { prefs.edit().apply { if (v == null) remove(KEY_FIRST_LAUNCH) else putLong(KEY_FIRST_LAUNCH, v) }.apply() }
    override var lastAskedAt: Long?
        get() = prefs.getLong(KEY_LAST_ASKED, -1L).takeIf { it >= 0 }
        set(v) { prefs.edit().apply { if (v == null) remove(KEY_LAST_ASKED) else putLong(KEY_LAST_ASKED, v) }.apply() }
}

/** Google Play In-App Review; Play decides whether the dialog is actually shown. */
class PlayReviewGateway(private val activity: Activity) : ReviewPrompt.Gateway {
    override fun launch() {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnSuccessListener { info -> manager.launchReviewFlow(activity, info) }
    }
}
