package com.vemestael.archeryshotcounter.presentation

import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vemestael.archeryshotcounter.R
import java.text.SimpleDateFormat
import java.util.Date
import com.vemestael.archeryshotcounter.presentation.theme.AppButton
import com.vemestael.archeryshotcounter.presentation.theme.AppListScreen
import com.vemestael.archeryshotcounter.presentation.theme.IbmPlexMono
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.SectionTitle

@Composable
fun ShotDetailScreen(
    session: Session,
    shots: List<Shot>,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }
    val unitAccel = stringResource(R.string.unit_accel)
    val totalShots = shots.size

    AppListScreen(fillBackground = true) { transformationSpec ->
        item {
            SectionTitle(text = dateFormat.format(Date(session.startTime)))
        }

        if (shots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.shots_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppPalette.current.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // shots are already sorted DESC — index 0 = newest = highest number
            shots.forEachIndexed { index, shot ->
                val shotNumber = totalShots - index
                item {
                    AppButton(
                        onClick = {},
                        scope = this,
                        transformationSpec = transformationSpec
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "#$shotNumber",
                                fontFamily = IbmPlexMono,
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppPalette.current.textDim
                            )
                            Text(
                                text = timeFormat.format(Date(shot.timestamp)),
                                fontFamily = IbmPlexMono,
                                fontSize = 12.sp,
                                color = LocalAppPalette.current.text
                            )
                            Text(
                                text = if (shot.magnitude != null) "↑ ${"%.1f".format(shot.magnitude)} $unitAccel" else "—",
                                fontFamily = IbmPlexMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shot.magnitude != null) MaterialTheme.colorScheme.primary else LocalAppPalette.current.textMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            ListBottomSpacer()
        }
    }
}
