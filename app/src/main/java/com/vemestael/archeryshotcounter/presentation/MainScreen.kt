package com.vemestael.archeryshotcounter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vemestael.archeryshotcounter.R
import com.vemestael.archeryshotcounter.presentation.theme.AppButton
import com.vemestael.archeryshotcounter.presentation.theme.AppListScreen
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.PlayPauseIcon
import com.vemestael.archeryshotcounter.presentation.theme.ShotCounterDisplay
import com.vemestael.archeryshotcounter.presentation.theme.StatusIndicator
import com.vemestael.archeryshotcounter.presentation.theme.TimerIcon

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
    onManualAdjust: (Int) -> Unit,
    counterSize: CounterSize = CounterSize.SMALL
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

    val primaryLabel = when {
        !sessionExists -> stringResource(R.string.btn_start)
        isDetecting -> stringResource(R.string.btn_stop)
        else -> stringResource(R.string.btn_resume)
    }
    val stopping = isDetecting && sessionExists

    AppListScreen { transformationSpec ->
            item {
                StatusIndicator(
                    text = statusText,
                    dotColor = statusColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                )
            }

            if (shotsPerEnd > 0 && currentSession != null) {
                item {
                    val seriesIndex = shotCount / shotsPerEnd
                    val prevBoundary = seriesIndex * shotsPerEnd
                    val effectivePrev = if (shotCount == prevBoundary && prevBoundary > 0) prevBoundary - shotsPerEnd else prevBoundary
                    val seriesNumber = effectivePrev / shotsPerEnd + 1
                    Text(
                        text = "${stringResource(R.string.series_title)} $seriesNumber",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 1.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalAppPalette.current.textDim
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (shotsPerEnd > 0 && currentSession != null) {
                        val seriesIndex = shotCount / shotsPerEnd
                        val prevBoundary = seriesIndex * shotsPerEnd
                        val nextBoundary = prevBoundary + shotsPerEnd
                        val effectivePrev = if (shotCount == prevBoundary && prevBoundary > 0) prevBoundary - shotsPerEnd else prevBoundary
                        val leftDelta = shotCount - effectivePrev
                        val rightDelta = nextBoundary - shotCount
                        ShotCounterDisplay(count = shotCount, leftDelta = leftDelta, rightDelta = rightDelta, fontSize = counterSize.fontSizeSp.sp)
                    } else {
                        ShotCounterDisplay(count = shotCount, fontSize = counterSize.fontSizeSp.sp)
                    }
                    Text(
                        text = stringResource(R.string.shots_label),
                        modifier = Modifier.padding(top = 2.dp),
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
                val squareShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppButton(
                        onClick = { onManualAdjust(-1) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        shape = squareShape
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("−", fontWeight = FontWeight.Bold)
                        }
                    }
                    AppButton(
                        onClick = onPrimaryButton,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .semantics { contentDescription = primaryLabel },
                        selected = !stopping,
                        destructive = stopping,
                        shape = squareShape
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PlayPauseIcon(
                                playing = stopping,
                                color = LocalAppPalette.current.onAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    AppButton(
                        onClick = { onManualAdjust(1) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        selected = true,
                        shape = squareShape
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (sessionExists && autoPauseEnabled) {
                item {
                    val secondaryLabel = if (autoPauseSecondsLeft >= 0)
                        "+5$unitS"
                    else
                        stringResource(R.string.btn_auto_pause)
                    AppButton(
                        onClick = onSecondaryButton,
                        scope = this,
                        transformationSpec = transformationSpec
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerIcon(
                                color = LocalAppPalette.current.buttonTextDim,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(secondaryLabel)
                        }
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
