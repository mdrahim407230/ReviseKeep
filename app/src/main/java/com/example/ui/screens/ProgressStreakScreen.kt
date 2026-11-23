package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MilestoneStatus
import com.example.data.local.RevisionActivityEntity
import com.example.data.local.RevisionMilestoneEntity
import com.example.domain.RevisionEngine
import com.example.domain.RevisionStage
import com.example.i18n.AppLang
import com.example.i18n.AppStrings
import com.example.ui.components.FrostedBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.ReviseTopBar
import com.example.ui.components.stageIconFor
import com.example.ui.theme.LocalGlassPalette
import java.util.Calendar

@Composable
fun ProgressStreakScreen(
    currentStreak: Int,
    bestStreak: Int,
    activities: List<RevisionActivityEntity>,
    milestones: List<RevisionMilestoneEntity>,
    effectiveNowMillis: Long,
    strings: AppStrings,
    lang: AppLang,
    isStudyingFocusMode: Boolean,
    onBack: () -> Unit,
    onToggleLang: () -> Unit,
    onToggleFocusMode: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val completedTotal = remember(milestones) {
        milestones.count { it.status == MilestoneStatus.COMPLETED }
    }
    val dueTotal = remember(milestones) {
        milestones.count { it.status == MilestoneStatus.ACTIVE_DUE || it.status == MilestoneStatus.MISSED }
    }
    val progressRatio = remember(completedTotal, dueTotal) {
        val total = (completedTotal + dueTotal).coerceAtLeast(1)
        (completedTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    val last14Days = remember(activities, effectiveNowMillis) {
        val map = activities.associateBy { it.dayKey }
        (13 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = effectiveNowMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val key = RevisionEngine.dayKey(cal.timeInMillis)
            map[key]?.completedCount ?: 0
        }
    }

    FrostedBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ReviseTopBar(
                    title = strings.progressStreak,
                    strings = strings,
                    onBackClick = onBack,
                    onToggleLang = onToggleLang,
                    isStudyingFocusMode = isStudyingFocusMode,
                    onToggleFocusMode = onToggleFocusMode
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual Streak Ring + Flame + Trophy (minimal text!)
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier.size(136.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 16.dp.toPx()
                                    drawArc(
                                        color = glass.glassBorder.copy(alpha = 0.35f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = glass.doneColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progressRatio,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = glass.doneColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "$completedTotal",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = glass.inkPrimary
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(glass.dueContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = glass.dueColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.streakDaysLabel(currentStreak),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = glass.inkPrimary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(glass.primaryAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = glass.primaryAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = strings.bestStreakLabel(bestStreak),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 14-day visual activity dots
                    GlassCard {
                        Text(
                            text = strings.activity14DaysTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            last14Days.forEach { count ->
                                val dotColor = when {
                                    count >= 3 -> glass.doneColor
                                    count >= 1 -> glass.doneColor.copy(alpha = 0.55f)
                                    else -> glass.glassBorder.copy(alpha = 0.35f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }

                    // 16-stage visual icons & completion count bars
                    GlassCard {
                        Text(
                            text = strings.stageMasteryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glass.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RevisionStage.entries.forEach { stage ->
                                val doneInStage = milestones.count {
                                    it.stageIndex == stage.index && it.status == MilestoneStatus.COMPLETED
                                }
                                val totalInStage = milestones.count { it.stageIndex == stage.index }.coerceAtLeast(1)
                                val ratio = (doneInStage.toFloat() / totalInStage.toFloat()).coerceIn(0f, 1f)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = stageIconFor(stage.index),
                                        contentDescription = stage.label(lang),
                                        tint = if (doneInStage > 0) glass.doneColor else glass.inkSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stage.shortLabel(lang),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = glass.inkPrimary,
                                        modifier = Modifier.widthIn(min = 38.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(glass.glassBorder.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(glass.doneColor)
                                        )
                                    }
                                    Text(
                                        text = "$doneInStage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.inkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
