package com.vemestael.archeryshotcounter.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vemestael.archeryshotcounter.R
import com.vemestael.archeryshotcounter.presentation.theme.AppButton
import com.vemestael.archeryshotcounter.presentation.theme.AppDialog
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.SectionTitle

/** Explains what a setting does — opened from its ⓘ ([com.vemestael.archeryshotcounter.presentation.theme.InfoButton]). */
@Composable
fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AppDialog(onDismissRequest = onDismiss) {
        SectionTitle(text = title, modifier = Modifier)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textDim,
            textAlign = TextAlign.Center
        )
        AppButton(
            text = stringResource(R.string.info_dialog_dismiss),
            onClick = onDismiss
        )
    }
}
