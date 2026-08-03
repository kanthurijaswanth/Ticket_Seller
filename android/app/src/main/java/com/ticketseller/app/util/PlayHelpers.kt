package com.ticketseller.app.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

object PlayHelpers {
    /**
     * Play In-App Review. Shows Google's native rating card in-app (no leaving the app).
     * Only actually appears when installed from Play and within Google's quota; a no-op otherwise.
     */
    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                runCatching { manager.launchReviewFlow(activity, task.result) }
            }
        }
    }
}
