package com.vemestael.archeryshotcounter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vemestael.archeryshotcounter.R
import com.vemestael.archeryshotcounter.presentation.theme.AppButton
import com.vemestael.archeryshotcounter.presentation.theme.AppListScreen
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.ShotCounterDisplay
import com.vemestael.archeryshotcounter.presentation.theme.StatusIndicator

@Composable
fun MainScreen(
    shotCount: Int,
    isDetecting: Boolean,
    currentSession: Session?,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseSecondsLeft: Int,
    lastShotMagnitude: Float?,
    onPrimaryButton: () -> Unit,
    onSecondaryButton: () -> Unit,
    onEnd: () -> Unit,
    onManualAdjust: (Int) -> Unit
) {
    val sessionExists = currentSession != null
    val pausedLabel = stringResource(R.string.status_paused)
    val unitM = stringResource(R.string.time_m)
    val unitS = stringResource(R.string.time_s)
    val statusText = when {
        !sessionExists -> stringResource(R.string.status_ready)
        autoPauseSecondsLeft >= 0 -> {
            val m = autoPauseSecondsLeft / 60
            val s = autoPauseSecondsLeft % 60
            val timeStr = if (m > 0) "${m}$unitM ${s.toString().padStart(2, '0')}$unitS" else "${s}$unitS"
            "$pausedLabel · $timeStr"
        }
        isDetecting -> stringResource(R.string.status_detecting)
        else -> pausedLabel
    }
    val statusColor = when {
        !sessionExists -> LocalAppPalette.current.textDim
        isDetecting -> LocalAppPalette.current.active
        else -> LocalAppPalette.current.pause
    }

    AppListScreen { transformationSpec ->
            item {
                StatusIndicator(
                    text = statusText,
                    dotColor = statusColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (shotsPerEnd > 0 && currentSession != null) {
                        val seriesIndex = shotCount / shotsPerEnd
                        val prevBoundary = seriesIndex * shotsPerEnd
                        val nextBoundary = prevBoundary + shotsPerEnd
                        val effectivePrev = if (shotCount == prevBoundary && prevBoundary > 0) prevBoundary - shotsPerEnd else prevBoundary
                        val leftDelta = shotCount - effectivePrev
                        val rightDelta = nextBoundary - shotCount
                        ShotCounterDisplay(count = shotCount, leftDelta = leftDelta, rightDelta = rightDelta)
                    } else {
                        ShotCounterDisplay(count = shotCount)
                    }
                    Text(
                        text = stringResource(R.string.shots_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppPalette.current.textDim
                    )
                    if (lastShotMagnitude != null) {
                        Text(
                            text = "↑ ${"%.1f".format(lastShotMagnitude)} ${stringResource(R.string.unit_accel)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalAppPalette.current.textMuted
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        text = "−1",
                        onClick = { onManualAdjust(-1) },
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    AppButton(
                        text = "+1",
                        onClick = { onManualAdjust(1) },
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        selected = true
                    )
                }
            }

            item {
                if (!sessionExists || !autoPauseEnabled) {
                    val label = when {
                        !sessionExists -> stringResource(R.string.btn_start)
                        isDetecting -> stringResource(R.string.btn_stop)
                        else -> stringResource(R.string.btn_resume)
                    }
                    val stopping = isDetecting && sessionExists
                    AppButton(
                        text = label,
                        onClick = onPrimaryButton,
                        fontWeight = FontWeight.Bold,
                        selected = !stopping,
                        destructive = stopping,
                        scope = this,
                        transformationSpec = transformationSpec
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val leftLabel = if (isDetecting)
                            stringResource(R.string.btn_stop)
                        else
                            stringResource(R.string.btn_resume)
                        AppButton(
                            text = leftLabel,
                            onClick = onPrimaryButton,
                            modifier = Modifier.weight(2f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            selected = !isDetecting,
                            destructive = isDetecting
                        )
                        val rightLabel = if (autoPauseSecondsLeft >= 0)
                            "+5$unitS"
                        else
                            stringResource(R.string.btn_auto_pause)
                        AppButton(
                            text = rightLabel,
                            onClick = onSecondaryButton,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                AppButton(
                    text = stringResource(R.string.btn_reset),
                    onClick = onEnd,
                    enabled = sessionExists,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }

            item {
                ListBottomSpacer()
            }
    }
}

@Composable
fun AmbientScreen(
    shotCount: Int,
    currentSession: Session?,
    isDetecting: Boolean
) {
    val statusText = when {
        currentSession == null -> stringResource(R.string.status_ready)
        isDetecting -> stringResource(R.string.status_detecting)
        else -> stringResource(R.string.status_paused)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShotCounterDisplay(count = shotCount, fontSize = 56.sp, color = Color.White)
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppPalette.current.textDim
            )
        }
    }
}

fun formatAutoPauseDuration(seconds: Int, unitM: String, unitS: String): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> "${s}$unitS"
        s == 0 -> "${m}$unitM"
        else -> "${m}$unitM ${s}$unitS"
    }
}
