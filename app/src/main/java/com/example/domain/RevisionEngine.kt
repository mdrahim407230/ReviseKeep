package com.example.domain

import com.example.data.local.DueRevisionItem
import com.example.data.local.MilestoneStatus
import com.example.data.local.RevisionMilestoneEntity
import com.example.i18n.AppLang
import java.util.Calendar

data class GeminiModelCandidate(
    val id: String,
    val displayName: String,
    val supportedMethods: List<String>,
    val score: Int
)

object RevisionEngine {

    /**
     * Marks any ACTIVE_DUE milestone whose dueAt < effectiveNowMillis as MISSED.
     * Missed items are never deleted; they stay in the list until completed.
     */
    fun findNewlyMissedMilestones(
        milestones: List<RevisionMilestoneEntity>,
        effectiveNowMillis: Long
    ): List<RevisionMilestoneEntity> {
        return milestones.filter {
            it.status == MilestoneStatus.ACTIVE_DUE && it.dueAt > 0L && effectiveNowMillis > it.dueAt
        }.map {
            it.copy(status = MilestoneStatus.MISSED)
        }
    }

    /**
     * Serial order:
     * 1. Stage index ascending (2h first -> 24h -> 3d -> 7d -> 1m ... 12m)
     * 2. Within the same stage, oldest dueAt first.
     */
    fun sortSerialDueQueue(
        items: List<DueRevisionItem>,
        effectiveNowMillis: Long
    ): List<DueRevisionItem> {
        return items
            .filter { item ->
                val status = item.milestone.status
                (status == MilestoneStatus.MISSED) ||
                    (status == MilestoneStatus.ACTIVE_DUE && item.milestone.dueAt <= effectiveNowMillis)
            }
            .sortedWith(
                compareBy<DueRevisionItem>(
                    { it.milestone.stageIndex },
                    { it.milestone.dueAt }
                )
            )
    }

    fun isWithinQuietHours(nowMillis: Long, startHour: Int, endHour: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (startHour == endHour) {
            false
        } else if (startHour > endHour) {
            // e.g., 22 to 7 -> 22,23,0,1,2,3,4,5,6
            hour >= startHour || hour < endHour
        } else {
            // e.g., 1 to 6
            hour in startHour until endHour
        }
    }

    fun dayKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    fun previousDayKey(millis: Long): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return dayKey(cal.timeInMillis)
    }

    /**
     * Ranks Gemini models supporting generateContent by preference:
     * best Pro-class -> Flash -> Flash-Lite -> other Gemini text models.
     */
    fun rankGeminiModels(
        rawModels: List<Pair<String, List<String>>>
    ): List<GeminiModelCandidate> {
        val excludedTokens = listOf(
            "embedding", "imagen", "veo", "aqa",
            "-tts", "image", "audio", "live",
            "computer-use", "deep-research", "robotics", "banana"
        )
        return rawModels
            .filter { (name, methods) ->
                val lowerName = name.lowercase()
                methods.any { it.equals("generateContent", ignoreCase = true) } &&
                    lowerName.contains("gemini") &&
                    excludedTokens.none { token -> lowerName.contains(token) }
            }
            .map { (fullName, methods) ->
                val cleanId = fullName.removePrefix("models/")
                val lower = cleanId.lowercase()
                val tierScore = when {
                    lower.contains("pro") && !lower.contains("vision") -> 300
                    lower.contains("flash-lite") -> 100
                    lower.contains("flash") -> 200
                    else -> 50
                }
                val versionScore = extractVersionScore(lower)
                val stabilityBonus = if (lower.contains("exp") || lower.contains("preview")) 0 else 15
                GeminiModelCandidate(
                    id = cleanId,
                    displayName = cleanId,
                    supportedMethods = methods,
                    score = tierScore + versionScore + stabilityBonus
                )
            }
            .sortedByDescending { it.score }
    }

    fun shouldFallbackOnHttpStatus(code: Int): Boolean =
        code in setOf(400, 403, 404, 408, 429, 500, 502, 503, 504)

    private fun extractVersionScore(modelId: String): Int {
        val match = Regex("(\\d+(?:\\.\\d+)?)").find(modelId)
        val num = match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 1.0
        return (num * 10).toInt()
    }

    /**
     * Strips HTML/script payloads and control characters from user-authored text.
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .trim()
    }

    fun buildCompletionMessage(
        lang: AppLang,
        whyText: String,
        whoForText: String,
        studentName: String,
        nextStageLabel: String?
    ): String {
        val namePrefix = if (studentName.isNotBlank()) "$studentName, " else ""
        val anchor = when {
            whoForText.isNotBlank() -> whoForText
            whyText.isNotBlank() -> whyText
            else -> ""
        }
        return if (lang == AppLang.BN) {
            val line = if (anchor.isNotBlank()) {
                "${namePrefix}তোমার প্রতিটি ধাপ \"$anchor\"-এর স্বপ্নকে আরও কাছে আনছে।"
            } else {
                "${namePrefix}আজকের একটুখানি চেষ্টা ভবিষ্যতে অনেক বড় শক্তি হয়ে ফিরবে।"
            }
            if (nextStageLabel != null) "$line\nপরবর্তী রিভিশন: $nextStageLabel" else "$line\n১২ মাসের সব ধাপ সম্পন্ন!"
        } else {
            val line = if (anchor.isNotBlank()) {
                "${namePrefix}every quiet step honors \"$anchor\"."
            } else {
                "${namePrefix}small, steady reviews build unshakeable memory."
            }
            if (nextStageLabel != null) "$line\nNext timer unlocked: $nextStageLabel" else "$line\nAll 12-month stages complete!"
        }
    }

    fun buildFallbackNotificationText(
        lang: AppLang,
        dueTitles: List<String>,
        hasMissed: Boolean,
        missedCount: Int,
        dueCount: Int,
        whyText: String,
        whoForText: String,
        streakDays: Int
    ): Pair<String, String> {
        val anchor = whoForText.ifBlank { whyText }.take(55)
        val subjects = dueTitles.distinct().take(3).joinToString(", ")
        return if (lang == AppLang.BN) {
            val title = if (hasMissed) {
                "আজকের এই সময়ের রিভিশন দেওয়া হয়নি"
            } else {
                "রিভিশনের সময় হয়েছে (${dueCount}টি)"
            }
            val bodyParts = buildList {
                if (hasMissed && missedCount > 0) {
                    add("আজকের এই সময়ের রিভিশন দেওয়া হয়নি ($missedCount টি বাকি)।")
                }
                if (subjects.isNotBlank()) add("বিষয়: $subjects।")
                if (anchor.isNotBlank()) add("মনে রেখো: $anchor")
                if (streakDays > 0) add("স্ট্রিক: $streakDays দিন")
            }
            title to bodyParts.joinToString(" ")
        } else {
            val title = if (hasMissed) {
                "Today's revision for this time slot is not done yet."
            } else {
                "Time for a quick revision ($dueCount due)"
            }
            val bodyParts = buildList {
                if (hasMissed && missedCount > 0) {
                    add("Today's revision for this time slot is not done yet ($missedCount waiting).")
                }
                if (subjects.isNotBlank()) add("Notes: $subjects.")
                if (anchor.isNotBlank()) add("For $anchor.")
                if (streakDays > 0) add("${streakDays}d streak.")
            }
            title to bodyParts.joinToString(" ")
        }
    }
}
