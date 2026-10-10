package com.vemestael.archeryshotcounter.presentation

import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.vemestael.archeryshotcounter.presentation.theme.AppListScreen
import com.vemestael.archeryshotcounter.presentation.theme.EditButton
import com.vemestael.archeryshotcounter.presentation.theme.IbmPlexMono
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.SectionTitle
import com.vemestael.archeryshotcounter.presentation.theme.Stepper

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
    onShowDetail: (Session) -> Unit,
    onShowEdit: (Session) -> Unit
) {
    val context = LocalContext.current

    val historyItems = buildHistoryItems(sessions, currentSession, activeShotCount)
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }

    AppListScreen { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.history_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
        }
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
                        val seriesSize = histItem.session.shotsPerEndAtStart
                        val durationAndSeriesText = if (seriesSize > 0)
                            "$durationText • ${stringResource(R.string.history_series_size, seriesSize)}"
                        else
                            durationText
                        AppButton(
                            onClick = { onShowDetail(histItem.session) },
                            scope = this,
                            transformationSpec = transformationSpec
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color = dotColor, shape = CircleShape)
                                        )
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LocalAppPalette.current.textDim
                                        )
                                        EditButton(
                                            onClick = { onShowEdit(histItem.session) },
                                            color = LocalAppPalette.current.textDim
                                        )
                                    }
                                    Text(
                                        text = "$startTimeText–$endTimeText",
                                        fontFamily = IbmPlexMono,
                                        fontSize = 11.sp,
                                        color = LocalAppPalette.current.text
                                    )
                                    Text(
                                        text = durationAndSeriesText,
                                        fontSize = 11.sp,
                                        color = LocalAppPalette.current.textDim
                                    )
                                }
                                Text(
                                    text = "${histItem.displayCount}",
                                    fontFamily = IbmPlexMono,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
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
fun EditSessionScreen(
    session: Session,
    onSave: (Session) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    var count by remember { mutableIntStateOf(session.shotCount) }
    var shotsPerEnd by remember { mutableIntStateOf(session.shotsPerEndAtStart) }
    var startTime by remember { mutableLongStateOf(session.startTime) }
    var endTime by remember { mutableLongStateOf(session.lastShotTime) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }
    val dateText = remember(session, locale) { dateFormat.format(Date(session.startTime)) }
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val is24Hour = remember(context) { AndroidDateFormat.is24HourFormat(context) }

    AppListScreen(fillBackground = true) {
        item {
            SectionTitle(
                text = stringResource(R.string.edit_session_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 0.dp)
            )
        }
        item {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = LocalAppPalette.current.textMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
        }
        item {
            TimeEditField(
                label = stringResource(R.string.edit_start_time),
                millis = startTime,
                timeFormat = timeFormat,
                is24Hour = is24Hour,
                locale = locale,
                onChange = { startTime = it }
            )
        }
        item {
            TimeEditField(
                label = stringResource(R.string.edit_end_time),
                millis = endTime,
                timeFormat = timeFormat,
                is24Hour = is24Hour,
                locale = locale,
                onChange = { endTime = it }
            )
        }
        item {
            CounterEditField(
                label = stringResource(R.string.edit_shots),
                value = count,
                minValue = 0,
                onChange = { count = it }
            )
        }
        item {
            CounterEditField(
                label = stringResource(R.string.edit_shots_per_end),
                value = shotsPerEnd,
                minValue = 0,
                maxValue = 99,
                zeroLabel = stringResource(R.string.series_off),
                onChange = { shotsPerEnd = it }
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.dialog_save),
                onClick = {
                    // The wheel only edits a clock time, not a date — if the chosen end time
                    // lands at or before the start, treat it as spilling into the next day
                    // rather than producing a negative duration.
                    val normalizedEndTime =
                        if (endTime <= startTime) endTime + 24L * 60 * 60 * 1000 else endTime
                    onSave(
                        session.copy(
                            shotCount = count,
                            startTime = startTime,
                            lastShotTime = normalizedEndTime,
                            shotsPerEndAtStart = shotsPerEnd
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                selected = true
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.dialog_delete),
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                destructive = true
            )
        }
        item {
            ListBottomSpacer()
        }
    }
}

/**
 * A labeled counter field: a button showing the current value that expands, on tap, into a
 * standard −/+ [Stepper] — the same expand-on-tap shell as [TimeEditField], so every field in the
 * session editor behaves the same way regardless of what it edits.
 */
@Composable
private fun CounterEditField(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE,
    zeroLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (value == 0 && zeroLabel != null) zeroLabel else "$value"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppPalette.current.text,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            textAlign = TextAlign.Center
        )
        AppButton(
            text = displayValue,
            onClick = { expanded = !expanded }
        )

        if (expanded) {
            Stepper(
                value = displayValue,
                onDecrement = { if (value > minValue) onChange(value - 1) },
                onIncrement = { if (value < maxValue) onChange(value + 1) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

/**
 * A labeled time-of-day field: a button showing the current value that expands, on tap, into
 * scrolling hour/minute wheels (plus an AM/PM wheel outside 24-hour locales) — the same
 * drum-picker interaction the phone companion app uses, ported to Wear's own
 * `LazyColumn`/`rememberSnapFlingBehavior` since Wear Compose Material3 has no TimePicker of its
 * own. Edits only the time-of-day; the calendar date is untouched.
 */
@Composable
private fun TimeEditField(
    label: String,
    millis: Long,
    timeFormat: java.text.DateFormat,
    is24Hour: Boolean,
    locale: java.util.Locale,
    onChange: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val amPmLabels = remember(locale) { java.text.DateFormatSymbols.getInstance(locale).amPmStrings.toList() }

    Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = LocalAppPalette.current.text,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        textAlign = TextAlign.Center
    )
    AppButton(
        text = timeFormat.format(Date(millis)),
        onClick = { expanded = !expanded }
    )

    if (expanded) {
        val initialCal = remember { Calendar.getInstance().apply { timeInMillis = millis } }
        val (initialHour12, initialIsPm) = hour24To12(initialCal.get(Calendar.HOUR_OF_DAY))

        var hourIndex by remember { mutableIntStateOf(if (is24Hour) initialCal.get(Calendar.HOUR_OF_DAY) else initialHour12 - 1) }
        var minuteIndex by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
        var periodIndex by remember { mutableIntStateOf(if (initialIsPm) 1 else 0) }

        fun commit() {
            val hour24 = if (is24Hour) hourIndex else hour12ToHour24(hourIndex + 1, periodIndex == 1)
            val newMillis = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, minuteIndex)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onChange(newMillis)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                values = if (is24Hour) (0..23).map { "$it".padStart(2, '0') } else (1..12).map { "$it" },
                selectedIndex = hourIndex,
                onSelectedIndexChange = { hourIndex = it; commit() },
                modifier = Modifier.width(44.dp)
            )
            Text(
                text = ":",
                fontFamily = IbmPlexMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LocalAppPalette.current.text,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            WheelColumn(
                values = (0..59).map { "$it".padStart(2, '0') },
                selectedIndex = minuteIndex,
                onSelectedIndexChange = { minuteIndex = it; commit() },
                modifier = Modifier.width(44.dp)
            )
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(6.dp))
                WheelColumn(
                    values = amPmLabels,
                    selectedIndex = periodIndex,
                    onSelectedIndexChange = { periodIndex = it; commit() },
                    modifier = Modifier.width(46.dp)
                )
            }
        }
    }
    }
}

private const val WHEEL_ITEM_HEIGHT_DP = 28
private const val WHEEL_VISIBLE_COUNT = 3

/**
 * A scrolling drum/wheel picker: snaps to whichever item sits nearest the centered highlight
 * band. Mirrors the phone companion app's hand-rolled picker (built there instead of pulling in
 * a picker library or Material3's clock-dial TimePicker) so the watch's time editor feels the
 * same to operate.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = WHEEL_ITEM_HEIGHT_DP.dp
    val visibleHeight = itemHeight * WHEEL_VISIBLE_COUNT
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedIndex
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - center) }?.index ?: selectedIndex
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) onSelectedIndexChange(centerIndex)
    }

    Box(modifier = modifier.height(visibleHeight), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(LocalAppPalette.current.bgElev2, RoundedCornerShape(6.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = (visibleHeight - itemHeight) / 2),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(values) { index, label ->
                val selected = index == centerIndex
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = IbmPlexMono,
                        fontSize = if (selected) 16.sp else 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) LocalAppPalette.current.accent else LocalAppPalette.current.textMuted
                    )
                }
            }
        }
    }
}

/** Returns (hour 1-12, isPm) for a 24-hour hour value. */
private fun hour24To12(hour24: Int): Pair<Int, Boolean> {
    val isPm = hour24 >= 12
    val hour12 = hour24 % 12
    return (if (hour12 == 0) 12 else hour12) to isPm
}

private fun hour12ToHour24(hour12: Int, isPm: Boolean): Int {
    val hour = hour12 % 12
    return if (isPm) hour + 12 else hour
}
