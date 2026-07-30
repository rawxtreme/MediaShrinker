package com.aaditya.mediashrinker

import java.util.Calendar

// TEMPORARY (big update announcement banner) — self-deactivates automatically
// once 15th August 2026 passes, since shouldShow() will simply return false.
// Not required to delete for correctness, but can be removed in a future
// update for code cleanup if desired.
object UpdateAnnouncementBanner {
    private const val TARGET_YEAR = 2026
    private const val TARGET_MONTH = Calendar.AUGUST
    private const val TARGET_DAY = 15

    fun shouldShow(): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(TARGET_YEAR, TARGET_MONTH, TARGET_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return today.before(target)
    }
}
