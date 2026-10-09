package com.vemestael.archeryshotcounter.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * A section/dialog title: the bright half of the app's confirmed color hierarchy — section and
 * dialog titles stay bright ([LocalAppPalette.text]) while every button caption below is dim
 * (see [AppButton]). Default modifier matches the common Settings-screen spacing (centered,
 * top 12dp / bottom 4dp); pass a different one for dialogs (already centered by their own
 * Column) or where spacing differs.
 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 4.dp)
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = LocalAppPalette.current.text,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The app's one button style. Unselected (the default) is an elevated pill with a dim caption;
 * [selected] swaps it to the accent pill with dark-on-accent text, and [destructive] swaps it to
 * the pause/error-colored pill with that same dark text (Pause/Stop, Clear data, Delete) — both
 * are bright-background cases where dark text reads better than the usual dim caption. One-off
 * buttons needing a background neither of these covers pass [containerColor] directly, keeping
 * whichever text color (dim by default, or [contentColor] itself) fits that background.
 *
 * Pass [scope] and [transformationSpec] (the `item { }` lambda's `this` and the column's
 * `rememberTransformationSpec()`) for a full-width item directly inside a `TransformingLazyColumn`
 * so it grows/shrinks at the screen edge like the rest of the list; omit both for buttons inside a
 * `Row` (steppers, side-by-side actions) or inside a plain `Dialog`.
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    containerColor: Color = when {
        destructive -> LocalAppPalette.current.pause
        selected -> LocalAppPalette.current.accent
        else -> LocalAppPalette.current.bgElev2
    },
    contentColor: Color = if (selected || destructive) LocalAppPalette.current.onAccent else LocalAppPalette.current.textDim,
    disabledContainerColor: Color = LocalAppPalette.current.bgElev2,
    disabledContentColor: Color = LocalAppPalette.current.textMuted,
    shape: Shape = ButtonDefaults.shape,
    scope: TransformingLazyColumnItemScope? = null,
    transformationSpec: TransformationSpec? = null,
    content: @Composable RowScope.() -> Unit
) {
    val sizedModifier = if (scope != null && transformationSpec != null)
        modifier.transformedHeight(scope, transformationSpec)
    else
        modifier
    val surfaceTransformation = if (scope != null && transformationSpec != null)
        scope.SurfaceTransformation(transformationSpec)
    else
        null
    Button(
        onClick = onClick,
        modifier = sizedModifier,
        enabled = enabled,
        shape = shape,
        transformation = surfaceTransformation,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        content = content
    )
}

/** [AppButton] convenience overload for the common single-line-caption case. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    containerColor: Color = when {
        destructive -> LocalAppPalette.current.pause
        selected -> LocalAppPalette.current.accent
        else -> LocalAppPalette.current.bgElev2
    },
    contentColor: Color = if (selected || destructive) LocalAppPalette.current.onAccent else LocalAppPalette.current.textDim,
    disabledContainerColor: Color = LocalAppPalette.current.bgElev2,
    disabledContentColor: Color = LocalAppPalette.current.textMuted,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    shape: Shape = ButtonDefaults.shape,
    scope: TransformingLazyColumnItemScope? = null,
    transformationSpec: TransformationSpec? = null
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        destructive = destructive,
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        shape = shape,
        scope = scope,
        transformationSpec = transformationSpec
    ) {
        Text(
            text = text,
            fontWeight = fontWeight,
            fontSize = fontSize,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A "−value+" row: a dim [AppButton] to decrement, the current value in the mono numeric style,
 * and an accent [AppButton] to increment. Used by every numeric setting on the watch (sensitivity
 * threshold, cooldown, series size, pause duration, dim brightness, and the history edit dialog's
 * count/series fields) so their shared sizing (button weight, value font size, row spacing) lives
 * in one place instead of 7 call sites.
 */
@Composable
fun Stepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
    valueFontSize: TextUnit = 20.sp,
    decrementLabel: String = "−",
    incrementLabel: String = "+",
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppButton(
            text = decrementLabel,
            onClick = onDecrement,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontFamily = IbmPlexMono,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            color = LocalAppPalette.current.accent
        )
        AppButton(
            text = incrementLabel,
            onClick = onIncrement,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            selected = true
        )
    }
}

/**
 * The shot counter itself: a big mono digit, with the series delta ("−N"/"+N") flanking it on
 * either side when both deltas are given. Used by [MainScreen]'s two counter states and by
 * `AmbientScreen`'s always-on-display variant (smaller, white-on-black, no deltas).
 */
@Composable
fun ShotCounterDisplay(
    count: Int,
    modifier: Modifier = Modifier,
    leftDelta: Int? = null,
    rightDelta: Int? = null,
    fontSize: TextUnit = 52.sp,
    color: Color = LocalAppPalette.current.accent
) {
    if (leftDelta != null && rightDelta != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (leftDelta > 0) "−$leftDelta" else "",
                fontFamily = IbmPlexMono,
                fontSize = 13.sp,
                color = LocalAppPalette.current.textMuted,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Text(
                text = "$count",
                fontFamily = IbmPlexMono,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = color
            )
            Text(
                text = "+$rightDelta",
                fontFamily = IbmPlexMono,
                fontSize = 13.sp,
                color = LocalAppPalette.current.textMuted,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    } else {
        Text(
            text = "$count",
            fontFamily = IbmPlexMono,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = modifier,
            color = color
        )
    }
}

/**
 * A colored dot followed by a label — the session/detection status row on [MainScreen] and each
 * history card's active/inactive marker. [textColor] defaults to matching the dot (the status-row
 * case); pass a fixed color when the label shouldn't track the dot (the history card's date, which
 * stays [AppPalette.textDim] regardless of dot color).
 */
@Composable
fun StatusIndicator(
    text: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = dotColor,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = dotColor, shape = CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

/** The blank space every scrollable list ends on, so content doesn't sit flush against the edge. */
@Composable
fun ListBottomSpacer(height: Dp = 16.dp) {
    Spacer(modifier = Modifier.fillMaxWidth().height(height))
}

/**
 * The rounded, scrollable card every watch dialog opens: clipped corners, elevated background,
 * centered content with consistent vertical spacing. Used by the Aod/Battery prompts, the clear-data
 * confirmation, and the history edit dialog.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(LocalAppPalette.current.bgElev)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}

/**
 * The scrollable-screen boilerplate every watch screen repeats: a [TransformingLazyColumnState] +
 * [TransformationSpec] feeding a [ScreenScaffold] wrapped around a [TransformingLazyColumn]. The
 * [TransformationSpec] is handed back into [content] so items can pass it (and their own `item { }`
 * scope) into [AppButton]/[AppListScreen] calls that need `transformedHeight`. Pass
 * [fillBackground] = true for a screen pushed on top of another (language picker, shot detail) that
 * needs its own opaque background instead of relying on the window's.
 */
@Composable
fun AppListScreen(
    fillBackground: Boolean = false,
    content: TransformingLazyColumnScope.(TransformationSpec) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scaffold = @Composable {
        ScreenScaffold(
            scrollState = listState,
            timeText = { if (!listState.canScrollBackward) TimeText() }
        ) { contentPadding ->
            TransformingLazyColumn(
                contentPadding = contentPadding,
                state = listState
            ) {
                content(transformationSpec)
            }
        }
    }
    if (fillBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            scaffold()
        }
    } else {
        scaffold()
    }
}

/**
 * A hand-drawn play/pause glyph — two bars when [playing] (tapping pauses), a right-pointing
 * triangle otherwise (tapping starts/resumes). Avoids pulling in an icon library for two shapes.
 */
@Composable
fun PlayPauseIcon(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (playing) {
            val barWidth = size.minDimension * 0.18f
            val barHeight = size.minDimension * 0.6f
            val gap = size.minDimension * 0.16f
            val top = (size.height - barHeight) / 2f
            val cornerRadius = CornerRadius(barWidth * 0.3f)
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width / 2f - gap / 2f - barWidth, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width / 2f + gap / 2f, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        } else {
            val w = size.minDimension * 0.55f
            val h = size.minDimension * 0.62f
            val left = (size.width - w) / 2f + w * 0.12f
            val top = (size.height - h) / 2f
            val path = Path().apply {
                moveTo(left, top)
                lineTo(left, top + h)
                lineTo(left + w, top + h / 2f)
                close()
            }
            drawPath(path, color = color)
        }
    }
}

/** A hand-drawn clock/timer glyph — stem, ring and two hands — for the auto-pause button. */
@Composable
fun TimerIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.1f
        val radius = size.minDimension * 0.36f
        val center = Offset(size.width / 2f, size.height / 2f + size.minDimension * 0.06f)
        drawLine(
            color = color,
            start = Offset(center.x, center.y - radius - size.minDimension * 0.16f),
            end = Offset(center.x, center.y - radius - size.minDimension * 0.02f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.45f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
