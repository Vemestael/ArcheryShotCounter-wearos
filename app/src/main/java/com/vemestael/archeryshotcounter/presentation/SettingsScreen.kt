package com.vemestael.archeryshotcounter.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.vemestael.archeryshotcounter.presentation.theme.AppSwitchButton
import com.vemestael.archeryshotcounter.presentation.theme.ListBottomSpacer
import com.vemestael.archeryshotcounter.presentation.theme.LocalAppPalette
import com.vemestael.archeryshotcounter.presentation.theme.PaletteChoice
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

/** The shot counter's digit size, chosen in Settings → Appearance. */
enum class CounterSize(@param:StringRes val labelRes: Int, val fontSizeSp: Int) {
    SMALL(R.string.counter_size_small, 52),
    MEDIUM(R.string.counter_size_medium, 62),
    LARGE(R.string.counter_size_large, 72)
}

/** Top-level Settings page: a short menu of sub-sections, shown directly in the main pager. */
@Composable
fun SettingsMenuScreen(
    onShowDetection: () -> Unit,
    onShowDisplay: () -> Unit,
    onShowAppearanceLanguage: () -> Unit,
    onShowData: () -> Unit
) {
    AppListScreen { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.settings_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.settings_menu_detection),
                onClick = onShowDetection,
                scope = this,
                transformationSpec = transformationSpec
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.settings_menu_display),
                onClick = onShowDisplay,
                scope = this,
                transformationSpec = transformationSpec
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.settings_menu_appearance_language),
                onClick = onShowAppearanceLanguage,
                scope = this,
                transformationSpec = transformationSpec
            )
        }
        item {
            AppButton(
                text = stringResource(R.string.export_title),
                onClick = onShowData,
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
fun DetectionSettingsScreen(
    sensitivity: Sensitivity,
    customThreshold: Int,
    shotCooldownSeconds: Int,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseDuration: Int,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit,
    onShotCooldownChange: (Int) -> Unit,
    onShotsPerEndChange: (Int) -> Unit,
    onAutoPauseEnabledChange: (Boolean) -> Unit,
    onAutoPauseDurationChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var infoDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    AppListScreen(fillBackground = true) { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.sensitivity_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                onInfoClick = { infoDialog = R.string.sensitivity_title to R.string.info_sensitivity_body }
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
            SectionTitle(
                text = stringResource(R.string.cooldown_title),
                onInfoClick = { infoDialog = R.string.cooldown_title to R.string.info_cooldown_body }
            )
        }

        item {
            Stepper(
                value = "$shotCooldownSeconds${stringResource(R.string.time_s)}",
                onDecrement = { if (shotCooldownSeconds > 1) onShotCooldownChange(shotCooldownSeconds - 1) },
                onIncrement = { if (shotCooldownSeconds < 30) onShotCooldownChange(shotCooldownSeconds + 1) }
            )
        }

        item {
            SectionTitle(
                text = stringResource(R.string.series_title),
                onInfoClick = { infoDialog = R.string.series_title to R.string.info_series_body }
            )
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
                AppSwitchButton(
                    checked = autoPauseEnabled,
                    onCheckedChange = onAutoPauseEnabledChange,
                    text = stringResource(R.string.auto_pause_title),
                    onInfoClick = { infoDialog = R.string.auto_pause_title to R.string.info_auto_pause_body },
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
            ListBottomSpacer()
        }
    }

    infoDialog?.let { (titleRes, bodyRes) ->
        InfoDialog(
            title = stringResource(titleRes),
            body = stringResource(bodyRes),
            onDismiss = { infoDialog = null }
        )
    }
}

@Composable
fun DisplaySettingsScreen(
    powerSavingEnabled: Boolean,
    useSystemAod: Boolean,
    dimBrightnessPercent: Int,
    onPowerSavingEnabledChange: (Boolean) -> Unit,
    onUseSystemAodChange: (Boolean) -> Unit,
    onDimBrightnessPercentChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var infoDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    AppListScreen(fillBackground = true) { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.settings_menu_display),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
        }
        item {
            AppSwitchButton(
                checked = powerSavingEnabled,
                onCheckedChange = onPowerSavingEnabledChange,
                text = stringResource(R.string.power_saving_title),
                onInfoClick = { infoDialog = R.string.power_saving_title to R.string.info_power_saving_body },
                scope = this,
                transformationSpec = transformationSpec
            )
        }

        if (powerSavingEnabled) {
            item {
                AppSwitchButton(
                    checked = useSystemAod,
                    onCheckedChange = onUseSystemAodChange,
                    text = stringResource(R.string.aod_prompt_title),
                    onInfoClick = { infoDialog = R.string.aod_prompt_title to R.string.info_aod_body },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }

            item {
                SectionTitle(
                    text = stringResource(R.string.dim_brightness_title),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    onInfoClick = { infoDialog = R.string.dim_brightness_title to R.string.info_dim_brightness_body }
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
            ListBottomSpacer()
        }
    }

    infoDialog?.let { (titleRes, bodyRes) ->
        InfoDialog(
            title = stringResource(titleRes),
            body = stringResource(bodyRes),
            onDismiss = { infoDialog = null }
        )
    }
}

@Composable
fun AppearanceLanguageSettingsScreen(
    currentLanguage: AppLanguage,
    paletteChoice: PaletteChoice,
    counterSize: CounterSize,
    onShowLanguagePicker: () -> Unit,
    onShowThemePicker: () -> Unit,
    onShowCounterSizePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AppListScreen(fillBackground = true) { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.lang_section_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
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
            SectionTitle(
                text = stringResource(R.string.appearance_title),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            AppButton(
                onClick = onShowThemePicker,
                scope = this,
                transformationSpec = transformationSpec
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = paletteChoice.palette.accent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Text(
                        text = stringResource(paletteChoice.labelRes),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        item {
            SectionTitle(
                text = stringResource(R.string.counter_size_title),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            AppButton(
                text = stringResource(counterSize.labelRes),
                onClick = onShowCounterSizePicker,
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
fun DataSettingsScreen(
    phoneSyncStatus: String?,
    onSyncData: () -> Unit,
    onClearData: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AppListScreen(fillBackground = true) { transformationSpec ->
        item {
            SectionTitle(
                text = stringResource(R.string.export_title),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)
            )
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

@Composable
fun ThemePickerScreen(
    currentPalette: PaletteChoice,
    onSelect: (PaletteChoice) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AppListScreen(fillBackground = true) { transformationSpec ->
        PaletteChoice.entries.forEach { choice ->
            item {
                AppButton(
                    onClick = { onSelect(choice) },
                    selected = currentPalette == choice,
                    scope = this,
                    transformationSpec = transformationSpec
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(choice.labelRes))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                choice.palette.bg,
                                choice.palette.bgElev,
                                choice.palette.accent,
                                choice.palette.pause,
                                choice.palette.active
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .border(1.dp, choice.palette.line, RoundedCornerShape(3.dp))
                                        .background(color = color, shape = RoundedCornerShape(3.dp))
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

@Composable
fun CounterSizePickerScreen(
    currentSize: CounterSize,
    onSelect: (CounterSize) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AppListScreen(fillBackground = true) { transformationSpec ->
        CounterSize.entries.forEach { size ->
            item {
                AppButton(
                    text = stringResource(size.labelRes),
                    onClick = { onSelect(size) },
                    selected = currentSize == size,
                    scope = this,
                    transformationSpec = transformationSpec
                )
            }
        }
        item {
            ListBottomSpacer()
        }
    }
}
