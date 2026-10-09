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

@Composable
fun AodPromptDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(onDismissRequest = onDismiss) {
        SectionTitle(text = stringResource(R.string.aod_prompt_title), modifier = Modifier)
        Text(
            text = stringResource(R.string.aod_prompt_message),
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textDim,
            textAlign = TextAlign.Center
        )
        AppButton(
            text = stringResource(R.string.aod_prompt_open_settings),
            onClick = onOpenSettings,
            selected = true
        )
        AppButton(
            text = stringResource(R.string.aod_prompt_dismiss),
            onClick = onDismiss
        )
    }
}
