package com.vemestael.archeryshotcounter.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.vemestael.archeryshotcounter.presentation.theme.SectionTitle
import com.vemestael.archeryshotcounter.presentation.theme.Stepper

enum class AppLanguage(val code: String, val nativeName: String, val englishName: String) {
    SYSTEM("system", "", "System"),
    ENGLISH("en", "English", "English"),
    RUSSIAN("ru", "Русский", "Russian"),
    SPANISH("es", "Español", "Spanish"),
    FRENCH("fr", "Français", "French"),
    GERMAN("de", "Deutsch", "German"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    CHINESE("zh", "中文", "Chinese"),
    JAPANESE("ja", "日本語", "Japanese"),
    KOREAN("ko", "한국어", "Korean"),
    ARABIC("ar", "العربية", "Arabic"),
    TURKISH("tr", "Türkçe", "Turkish"),
    HINDI("hi", "हिन्दी", "Hindi")
}

@Composable
fun SettingsScreen(
    sensitivity: Sensitivity,
    customThreshold: Int,
    currentLanguage: AppLanguage,
    shotCooldownSeconds: Int,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseDuration: Int,
    powerSavingEnabled: Boolean,
    useSystemAod: Boolean,
    dimBrightnessPercent: Int,
    phoneSyncStatus: String?,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit,
    onShowLanguagePicker: () -> Unit,
    onShotCooldownChange: (Int) -> Unit,
    onShotsPerEndChange: (Int) -> Unit,
    onAutoPauseEnabledChange: (Boolean) -> Unit,
    onAutoPauseDurationChange: (Int) -> Unit,
    onPowerSavingEnabledChange: (Boolean) -> Unit,
    onUseSystemAodChange: (Boolean) -> Unit,
    onDimBrightnessPercentChange: (Int) -> Unit,
    onSyncData: () -> Unit,
    onClearData: () -> Unit
) {
    AppListScreen { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.sensitivity_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
        }

        Sensitivity.entries.forEach { s ->
            item {
                AppButton(
                    text = stringResource(s.labelRes),
                    onClick = { onSensitivityChange(s) },
                    selected = sensitivity == s,
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }

            if (s == Sensitivity.CUSTOM && sensitivity == Sensitivity.CUSTOM) {
                item {
                    Stepper(
                        value = "$customThreshold",
                        onDecrement = { if (customThreshold > 5) onCustomThresholdChange(customThreshold - 1) },
                        onIncrement = { if (customThreshold < 50) onCustomThresholdChange(customThreshold + 1) }
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.sensitivity_range),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalAppPalette.current.textMuted
                    )
                }
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.cooldown_title))
        }

        item {
            Stepper(
                value = "$shotCooldownSeconds${stringResource(R.string.time_s)}",
                onDecrement = { if (shotCooldownSeconds > 1) onShotCooldownChange(shotCooldownSeconds - 1) },
                onIncrement = { if (shotCooldownSeconds < 30) onShotCooldownChange(shotCooldownSeconds + 1) }
            )
        }

        item {
            SectionTitle(text = stringResource(R.string.series_title))
        }

        item {
            Stepper(
                value = if (shotsPerEnd == 0) stringResource(R.string.series_off) else "$shotsPerEnd",
                onDecrement = { if (shotsPerEnd > 0) onShotsPerEndChange(shotsPerEnd - 1) },
                onIncrement = { if (shotsPerEnd < 99) onShotsPerEndChange(shotsPerEnd + 1) }
            )
        }

        if (shotsPerEnd > 0) {
            item {
                AppButton(
                    text = stringResource(if (autoPauseEnabled) R.string.auto_pause_on else R.string.auto_pause_off),
                    onClick = { onAutoPauseEnabledChange(!autoPauseEnabled) },
                    selected = autoPauseEnabled,
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }

            if (autoPauseEnabled) {
                item {
                    SectionTitle(
                        text = stringResource(R.string.pause_duration_title),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                item {
                    Stepper(
                        value = formatAutoPauseDuration(autoPauseDuration, stringResource(R.string.time_m), stringResource(R.string.time_s)),
                        onDecrement = { if (autoPauseDuration > 5) onAutoPauseDurationChange(autoPauseDuration - 5) },
                        onIncrement = { onAutoPauseDurationChange(autoPauseDuration + 5) },
                        valueFontSize = 18.sp
                    )
                }
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.power_saving_title))
        }

        item {
            AppButton(
                text = stringResource(if (powerSavingEnabled) R.string.power_saving_on else R.string.power_saving_off),
                onClick = { onPowerSavingEnabledChange(!powerSavingEnabled) },
                selected = powerSavingEnabled,
                scope = this,
                transformationSpec = transformationSpec
            )
        }

        if (powerSavingEnabled) {
            item {
                AppButton(
                    text = stringResource(if (useSystemAod) R.string.use_aod_on else R.string.use_aod_off),
                    onClick = { onUseSystemAodChange(!useSystemAod) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    selected = useSystemAod,
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }

            item {
                SectionTitle(
                    text = stringResource(R.string.dim_brightness_title),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                Stepper(
                    value = "$dimBrightnessPercent%",
                    onDecrement = { if (dimBrightnessPercent > 1) onDimBrightnessPercentChange(dimBrightnessPercent - 1) },
                    onIncrement = { if (dimBrightnessPercent < 50) onDimBrightnessPercentChange(dimBrightnessPercent + 1) }
                )
            }
        }

        item {
            SectionTitle(text = stringResource(R.string.lang_section_title))
        }

        item {
            val currentLabel = if (currentLanguage == AppLanguage.SYSTEM)
                stringResource(R.string.lang_system)
            else
                currentLanguage.nativeName
            AppButton(
                text = currentLabel,
                onClick = onShowLanguagePicker,
                scope = this,
                transformationSpec = transformationSpec
            )
        }

        item {
            SectionTitle(text = stringResource(R.string.export_title))
        }

        item {
            AppButton(
                text = stringResource(R.string.phone_sync_button),
                onClick = onSyncData,
                scope = this,
                transformationSpec = transformationSpec
            )
        }

        if (phoneSyncStatus != null) {
            item {
                Text(
                    text = phoneSyncStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppPalette.current.textDim
                )
            }
        }

        item {
            AppButton(
                text = stringResource(R.string.clear_data_button),
                onClick = onClearData,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                destructive = true,
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
fun LanguagePickerScreen(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AppListScreen(fillBackground = true) { transformationSpec ->
        AppLanguage.entries.forEach { lang ->
            item {
                val mainName = if (lang == AppLanguage.SYSTEM)
                    stringResource(R.string.lang_system)
                else
                    lang.nativeName
                val subtitle = if (lang != AppLanguage.SYSTEM && lang.nativeName != lang.englishName)
                    lang.englishName
                else
                    null
                AppButton(
                    onClick = { onSelect(lang) },
                    selected = currentLanguage == lang,
                    scope = this,
                    transformationSpec = transformationSpec
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = mainName,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (currentLanguage == lang)
                                    LocalAppPalette.current.onAccent.copy(alpha = 0.6f)
                                else
                                    LocalAppPalette.current.textMuted
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
