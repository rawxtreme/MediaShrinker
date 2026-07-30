package com.aaditya.mediashrinker

import java.util.Calendar

// TEMPORARY (cosmetic Independence Day theme) — self-deactivates automatically
// once 15th August passes, since isIndependenceDay() will simply return false.
// Not required to delete for correctness, but can be removed in a future
// update for code cleanup if desired.
object IndependenceDayTheme {
    fun isIndependenceDay(): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.MONTH) == Calendar.AUGUST && today.get(Calendar.DAY_OF_MONTH) == 15
    }
}
