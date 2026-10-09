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
fun ClearDataConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AppDialog(onDismissRequest = onCancel) {
        SectionTitle(text = stringResource(R.string.clear_data_confirm_title), modifier = Modifier)
        Text(
            text = stringResource(R.string.clear_data_confirm_message),
            style = MaterialTheme.typography.bodySmall,
            color = LocalAppPalette.current.textDim,
            textAlign = TextAlign.Center
        )
        AppButton(
            text = stringResource(R.string.clear_data_confirm_ok),
            onClick = onConfirm,
            destructive = true
        )
        AppButton(
            text = stringResource(R.string.clear_data_confirm_cancel),
            onClick = onCancel
        )
    }
}
