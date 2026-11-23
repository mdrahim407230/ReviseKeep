package com.example.domain

import com.example.i18n.AppLang
import com.example.i18n.StringsBnEn
import java.util.Calendar

enum class RevisionStage(
    val index: Int,
    val hours: Int = 0,
    val days: Int = 0,
    val months: Int = 0
) {
    H2(0, hours = 2),
    H24(1, hours = 24),
    D3(2, days = 3),
    D7(3, days = 7),
    M1(4, months = 1),
    M2(5, months = 2),
    M3(6, months = 3),
    M4(7, months = 4),
    M5(8, months = 5),
    M6(9, months = 6),
    M7(10, months = 7),
    M8(11, months = 8),
    M9(12, months = 9),
    M10(13, months = 10),
    M11(14, months = 11),
    M12(15, months = 12);

    fun label(lang: AppLang): String = StringsBnEn.forLang(lang).stageLabels[index]
    fun shortLabel(lang: AppLang): String = StringsBnEn.forLang(lang).stageShortLabels[index]

    fun calculateDueFrom(anchorMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = anchorMillis
        when {
            hours > 0 -> cal.add(Calendar.HOUR_OF_DAY, hours)
            days > 0 -> cal.add(Calendar.DAY_OF_YEAR, days)
            months > 0 -> cal.add(Calendar.MONTH, months)
        }
        return cal.timeInMillis
    }

    fun nextStage(): RevisionStage? = entries.getOrNull(index + 1)

    companion object {
        fun fromIndex(index: Int): RevisionStage =
            entries.firstOrNull { it.index == index } ?: H2
    }
}
