package com.vemestael.archeryshotcounter.presentation

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.util.Calendar
import java.util.Date
import com.vemestael.archeryshotcounter.presentation.theme.AppButton
import com.vemestael.archeryshotcounter.presentation.theme.AppDialog
import com.vemestael.archeryshotcounter.presentation.theme.AppListScreen
import com.vemestael.archeryshotcounter.presentation.theme.IbmPlexMono
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.SectionTitle
import com.vemestael.archeryshotcounter.presentation.theme.Stepper
import com.vemestael.archeryshotcounter.presentation.theme.StatusIndicator

private sealed class HistoryListItem {
    data class SessionItem(
        val session: Session,
        val isActive: Boolean,
        val displayCount: Int
    ) : HistoryListItem()
    data class YearLabel(val year: Int) : HistoryListItem()
}

@Composable
fun HistoryScreen(
    sessions: List<Session>,
    currentSession: Session?,
    activeShotCount: Int,
    onEdit: (Session) -> Unit,
    onDelete: (Session) -> Unit,
    onShowDetail: (Session) -> Unit
) {
    val context = LocalContext.current
    val editingSession = remember { mutableStateOf<Session?>(null) }

    val historyItems = buildHistoryItems(sessions, currentSession, activeShotCount)
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }

    AppListScreen { transformationSpec ->
        if (historyItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppPalette.current.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            historyItems.forEach { histItem ->
                when (histItem) {
                    is HistoryListItem.YearLabel -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = histItem.year.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppPalette.current.textMuted
                            )
                        }
                    }
                    is HistoryListItem.SessionItem -> item {
                        val dotColor = if (histItem.isActive) LocalAppPalette.current.active else LocalAppPalette.current.textMuted
                        val dateText = dateFormat.format(Date(histItem.session.startTime))
                        val startTimeText = timeFormat.format(Date(histItem.session.startTime))
                        val endTimeText = timeFormat.format(Date(histItem.session.lastShotTime))
                        val durationMin = (histItem.session.lastShotTime - histItem.session.startTime) / 60000
                        val unitH = stringResource(R.string.time_h)
                        val unitM = stringResource(R.string.time_m)
                        val durationText = when {
                            durationMin < 1 -> "<1$unitM"
                            durationMin < 60 -> "${durationMin}$unitM"
                            else -> {
                                val h = durationMin / 60
                                val m = durationMin % 60
                                if (m == 0L) "${h}$unitH" else "${h}$unitH ${m}$unitM"
                            }
                        }
                        val shotsLabel = stringResource(R.string.shots_label)

                        AppButton(
                            onClick = { editingSession.value = histItem.session },
                            scope = this,
                            transformationSpec = transformationSpec
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                StatusIndicator(
                                    text = dateText,
                                    dotColor = dotColor,
                                    textColor = LocalAppPalette.current.textDim
                                )
                                Text(
                                    text = "$startTimeText – $endTimeText",
                                    fontFamily = IbmPlexMono,
                                    fontSize = 12.sp,
                                    color = LocalAppPalette.current.text
                                )
                                Text(
                                    text = "${histItem.displayCount} $shotsLabel • $durationText",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ListBottomSpacer()
        }
    }

    editingSession.value?.let { session ->
        EditSessionDialog(
            session = session,
            onShowDetail = {
                editingSession.value = null
                onShowDetail(session)
            },
            onSave = { updated ->
                onEdit(updated)
                editingSession.value = null
            },
            onDelete = {
                onDelete(session)
                editingSession.value = null
            },
            onDismiss = { editingSession.value = null }
        )
    }
}

private fun buildHistoryItems(
    sessions: List<Session>,
    currentSession: Session?,
    activeShotCount: Int
): List<HistoryListItem> {
    val allSessions = if (currentSession != null && sessions.none { it.id == currentSession.id }) {
        listOf(currentSession.copy(shotCount = activeShotCount)) + sessions
    } else {
        sessions
    }

    val sorted = allSessions.sortedByDescending { it.startTime }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val result = mutableListOf<HistoryListItem>()
    var lastYear: Int? = null

    sorted.forEach { session ->
        val sessionYear = Calendar.getInstance().apply { timeInMillis = session.startTime }.get(Calendar.YEAR)
        if (sessionYear != currentYear && sessionYear != lastYear) {
            result.add(HistoryListItem.YearLabel(sessionYear))
            lastYear = sessionYear
        }
        val isActive = currentSession?.id == session.id
        val displayCount = if (isActive) activeShotCount else session.shotCount
        result.add(HistoryListItem.SessionItem(session, isActive, displayCount))
    }

    return result
}

@Composable
private fun EditSessionDialog(
    session: Session,
    onShowDetail: () -> Unit,
    onSave: (Session) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableIntStateOf(session.shotCount) }
    var shotsPerEnd by remember { mutableIntStateOf(session.shotsPerEndAtStart) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }
    val dateText = remember(session, locale) { dateFormat.format(Date(session.startTime)) }

    AppDialog(onDismissRequest = onDismiss) {
        SectionTitle(text = dateText, modifier = Modifier)
        AppButton(
            text = stringResource(R.string.btn_detail),
            onClick = onShowDetail
        )
        Stepper(
            value = "$count",
            onDecrement = { if (count > 0) count-- },
            onIncrement = { count++ },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.series_title),
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppPalette.current.text
        )
        Stepper(
            value = if (shotsPerEnd == 0) stringResource(R.string.series_off) else "$shotsPerEnd",
            onDecrement = { if (shotsPerEnd > 0) shotsPerEnd-- },
            onIncrement = { if (shotsPerEnd < 99) shotsPerEnd++ },
            modifier = Modifier.fillMaxWidth()
        )
        AppButton(
            text = stringResource(R.string.dialog_save),
            onClick = { onSave(session.copy(shotCount = count, shotsPerEndAtStart = shotsPerEnd)) },
            selected = true
        )
        AppButton(
            text = stringResource(R.string.dialog_delete),
            onClick = onDelete,
            destructive = true
        )
    }
}
