package com.vemestael.archeryshotcounter.presentation

import android.content.Context
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.edit
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPageIndicator
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vemestael.archeryshotcounter.R
import com.vemestael.archeryshotcounter.presentation.theme.ArcheryShotCounterTheme
import com.vemestael.archeryshotcounter.presentation.theme.PaletteChoice
import java.util.Locale
import java.util.concurrent.Executors

private const val PREFS_NAME = "settings"
private const val KEY_LANGUAGE = "language"
private const val KEY_SENSITIVITY = "sensitivity"
private const val KEY_CUSTOM_THRESHOLD = "custom_threshold"
private const val KEY_PENDING_ID = "pending_id"
private const val KEY_PENDING_START = "pending_start"
private const val KEY_PENDING_LAST = "pending_last"
private const val KEY_PENDING_COUNT = "pending_count"
private const val KEY_SHOT_COOLDOWN_SECONDS = "shot_cooldown_seconds"
private const val KEY_SHOTS_PER_END = "shots_per_end"
private const val KEY_AUTO_PAUSE_ENABLED = "auto_pause_enabled"
private const val KEY_AUTO_PAUSE_DURATION = "auto_pause_duration"
private const val KEY_POWER_SAVING_ENABLED = "power_saving_enabled"
private const val KEY_USE_SYSTEM_AOD = "use_system_aod"
private const val KEY_DIM_BRIGHTNESS_PERCENT = "dim_brightness_percent"
private const val DEFAULT_DIM_BRIGHTNESS_PERCENT = 8
private const val KEY_PALETTE = "palette"
private const val KEY_COUNTER_SIZE = "counter_size"
private const val TAP_BRIGHTEN_DURATION_MS = 5000L

/**
 * Whether the system's Always On Display / ambient mode is available to fall back on.
 * Read once from the undocumented `ambient_enabled` Settings.Global key (present on Wear OS
 * devices regardless of manufacturer) so we know whether to force a dimmed screen ourselves.
 */
enum class AmbientAvailability { ENABLED, DISABLED, UNKNOWN }

class MainActivity : ComponentActivity() {

    private lateinit var shotDetector: ShotDetector
    private lateinit var vibrator: Vibrator
    private lateinit var database: AppDatabase
    private val dbExecutor = Executors.newSingleThreadExecutor()

    private var shotCount by mutableIntStateOf(0)
    private var isDetecting by mutableStateOf(false)
    private var currentSession by mutableStateOf<Session?>(null)
    private val sessions = mutableStateListOf<Session>()

    private var sensitivity by mutableStateOf(Sensitivity.MEDIUM)
    private var customThreshold by mutableIntStateOf(15)
    private var shotCooldownSeconds by mutableIntStateOf((ShotDetector.DEFAULT_COOLDOWN_MS / 1000L).toInt())
    private var currentLanguage by mutableStateOf(AppLanguage.SYSTEM)

    private var shotsPerEnd by mutableIntStateOf(0)
    private var autoPauseEnabled by mutableStateOf(false)
    private var autoPauseDuration by mutableIntStateOf(60)
    private var autoPauseSecondsLeft by mutableIntStateOf(-1)
    private var autoPauseTimer: CountDownTimer? = null

    private var lastShotMagnitude by mutableStateOf<Float?>(null)
    private val magnitudeHandler = Handler(Looper.getMainLooper())
    private val magnitudeHideRunnable = Runnable { lastShotMagnitude = null }

    private var detailSession by mutableStateOf<Session?>(null)
    private val detailShots = mutableStateListOf<Shot>()

    private var editingSession by mutableStateOf<Session?>(null)

    private var isAmbient by mutableStateOf(false)
    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
        }
        override fun onExitAmbient() {
            isAmbient = false
        }
    }
    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    private var phoneSyncStatus by mutableStateOf<String?>(null)
    private val phoneSyncStatusHandler = Handler(Looper.getMainLooper())
    private val phoneSyncStatusHideRunnable = Runnable { phoneSyncStatus = null }
    private var showClearDataConfirm by mutableStateOf(false)

    private var ambientAvailability = AmbientAvailability.UNKNOWN

    private var powerSavingEnabled by mutableStateOf(true)
    private var useSystemAod by mutableStateOf(true)
    private var dimBrightnessPercent by mutableIntStateOf(DEFAULT_DIM_BRIGHTNESS_PERCENT)
    private var paletteChoice by mutableStateOf(PaletteChoice.BRASS)
    private var counterSize by mutableStateOf(CounterSize.SMALL)
    private var isScreenDimmed = false
    private val brightenHandler = Handler(Looper.getMainLooper())
    private val reDimRunnable = Runnable { dimScreenBrightness() }

    override fun attachBaseContext(newBase: Context) {
        val code = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        currentLanguage = AppLanguage.entries.find { it.code == code } ?: AppLanguage.SYSTEM
        if (code == AppLanguage.SYSTEM.code) {
            super.attachBaseContext(newBase)
        } else {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(Locale.forLanguageTag(code))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }

    private fun changeLanguage(lang: AppLanguage) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putString(KEY_LANGUAGE, lang.code) }
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.attributes = window.attributes.also { it.preferredRefreshRate = 60f }
        lifecycle.addObserver(ambientObserver)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        database = AppDatabase.getInstance(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        shotDetector = ShotDetector(applicationContext, sensorManager) { magnitude ->
            runOnUiThread {
                shotCount++
                lastShotMagnitude = magnitude
                magnitudeHandler.removeCallbacks(magnitudeHideRunnable)
                magnitudeHandler.postDelayed(magnitudeHideRunnable, 5000)
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                recordShot(magnitude)
            }
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sensitivity = Sensitivity.entries.find { it.name == prefs.getString(KEY_SENSITIVITY, null) }
            ?: Sensitivity.MEDIUM
        customThreshold = prefs.getInt(KEY_CUSTOM_THRESHOLD, 15)
        shotCooldownSeconds = prefs.getInt(KEY_SHOT_COOLDOWN_SECONDS, (ShotDetector.DEFAULT_COOLDOWN_MS / 1000L).toInt())
        shotDetector.sensitivity = sensitivity
        shotDetector.customThreshold = customThreshold.toFloat()
        shotDetector.cooldownMs = shotCooldownSeconds * 1000L
        shotsPerEnd = prefs.getInt(KEY_SHOTS_PER_END, 0)
        autoPauseEnabled = prefs.getBoolean(KEY_AUTO_PAUSE_ENABLED, false)
        autoPauseDuration = prefs.getInt(KEY_AUTO_PAUSE_DURATION, 60)
        powerSavingEnabled = prefs.getBoolean(KEY_POWER_SAVING_ENABLED, true)
        useSystemAod = prefs.getBoolean(KEY_USE_SYSTEM_AOD, true)
        dimBrightnessPercent = prefs.getInt(KEY_DIM_BRIGHTNESS_PERCENT, DEFAULT_DIM_BRIGHTNESS_PERCENT)
        paletteChoice = PaletteChoice.entries.find { it.name == prefs.getString(KEY_PALETTE, null) }
            ?: PaletteChoice.BRASS
        counterSize = CounterSize.entries.find { it.name == prefs.getString(KEY_COUNTER_SIZE, null) }
            ?: CounterSize.SMALL

        ambientAvailability = detectAmbientAvailability()

        dbExecutor.execute {
            if (database.sessionDao().getAll().isEmpty()) {
                SessionStorage(applicationContext).load()
                    .forEach { database.sessionDao().insertOrUpdate(it) }
            }
            val all = database.sessionDao().getAll()
            runOnUiThread {
                sessions.addAll(all)
                val pendingId = prefs.getLong(KEY_PENDING_ID, -1L)
                if (pendingId != -1L) {
                    val restored = Session(
                        id = pendingId,
                        startTime = prefs.getLong(KEY_PENDING_START, pendingId),
                        lastShotTime = prefs.getLong(KEY_PENDING_LAST, pendingId),
                        shotCount = prefs.getInt(KEY_PENDING_COUNT, 0)
                    )
                    currentSession = restored
                    shotCount = restored.shotCount
                }
            }
        }

        setContent {
            ArcheryShotCounterTheme(palette = paletteChoice.palette) {
                ArcheryApp(
                    shotCount = shotCount,
                    isDetecting = isDetecting,
                    isAmbient = isAmbient,
                    currentSession = currentSession,
                    sessions = sessions,
                    sensitivity = sensitivity,
                    customThreshold = customThreshold,
                    currentLanguage = currentLanguage,
                    shotCooldownSeconds = shotCooldownSeconds,
                    shotsPerEnd = shotsPerEnd,
                    autoPauseEnabled = autoPauseEnabled,
                    autoPauseDuration = autoPauseDuration,
                    powerSavingEnabled = powerSavingEnabled,
                    useSystemAod = useSystemAod,
                    dimBrightnessPercent = dimBrightnessPercent,
                    paletteChoice = paletteChoice,
                    counterSize = counterSize,
                    autoPauseSecondsLeft = autoPauseSecondsLeft,
                    lastShotMagnitude = lastShotMagnitude,
                    phoneSyncStatus = phoneSyncStatus,
                    onSyncData = ::syncData,
                    onScreenTap = ::onScreenTapped,
                    showClearDataConfirm = showClearDataConfirm,
                    onClearData = ::startClearData,
                    onConfirmClearData = ::confirmClearData,
                    onCancelClearData = ::cancelClearData,
                    onStartOrToggle = ::onPrimaryButton,
                    onSecondaryButton = ::onSecondaryButton,
                    onEnd = ::endSession,
                    onManualAdjust = ::manualAdjust,
                    onSensitivityChange = { s ->
                        sensitivity = s
                        shotDetector.sensitivity = s
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putString(KEY_SENSITIVITY, s.name) }
                    },
                    onCustomThresholdChange = { value ->
                        customThreshold = value
                        shotDetector.customThreshold = value.toFloat()
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_CUSTOM_THRESHOLD, value) }
                    },
                    onShotCooldownChange = { value ->
                        shotCooldownSeconds = value
                        shotDetector.cooldownMs = value * 1000L
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_SHOT_COOLDOWN_SECONDS, value) }
                    },
                    onLanguageChange = ::changeLanguage,
                    onThemeChange = { choice ->
                        paletteChoice = choice
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putString(KEY_PALETTE, choice.name) }
                    },
                    onCounterSizeChange = { size ->
                        counterSize = size
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putString(KEY_COUNTER_SIZE, size.name) }
                    },
                    onEditSession = ::editSession,
                    onDeleteSession = ::deleteSession,
                    detailSession = detailSession,
                    detailShots = detailShots,
                    onShowDetail = { session ->
                        detailSession = session
                        detailShots.clear()
                        dbExecutor.execute {
                            val shots = database.shotDao().getBySession(session.id)
                            runOnUiThread { detailShots.addAll(shots) }
                        }
                    },
                    onDismissDetail = { detailSession = null; detailShots.clear() },
                    editingSession = editingSession,
                    onShowEdit = { session -> editingSession = session },
                    onDismissEdit = { editingSession = null },
                    onShotsPerEndChange = { value ->
                        shotsPerEnd = value
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_SHOTS_PER_END, value) }
                    },
                    onAutoPauseEnabledChange = { enabled ->
                        autoPauseEnabled = enabled
                        if (!enabled) cancelAutoPause()
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putBoolean(KEY_AUTO_PAUSE_ENABLED, enabled) }
                    },
                    onAutoPauseDurationChange = { value ->
                        autoPauseDuration = value
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_AUTO_PAUSE_DURATION, value) }
                    },
                    onPowerSavingEnabledChange = { enabled ->
                        powerSavingEnabled = enabled
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putBoolean(KEY_POWER_SAVING_ENABLED, enabled) }
                        if (isDetecting) applyTrackingScreenMode() else if (isPaused()) applyScreenPowerMode()
                    },
                    onUseSystemAodChange = { enabled ->
                        useSystemAod = enabled
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putBoolean(KEY_USE_SYSTEM_AOD, enabled) }
                        // Never consulted while detecting — tracking always uses the manual dim
                        // fallback, never real system Ambient Mode.
                        if (isPaused()) applyScreenPowerMode()
                    },
                    onDimBrightnessPercentChange = { value ->
                        dimBrightnessPercent = value
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_DIM_BRIGHTNESS_PERCENT, value) }
                        if (isScreenDimmed) dimScreenBrightness()
                    }
                )
            }
        }
    }

    private fun detectAmbientAvailability(): AmbientAvailability =
        try {
            val value = Settings.Global.getInt(contentResolver, "ambient_enabled")
            if (value != 0) AmbientAvailability.ENABLED else AmbientAvailability.DISABLED
        } catch (e: Settings.SettingNotFoundException) {
            AmbientAvailability.UNKNOWN
        } catch (e: SecurityException) {
            AmbientAvailability.UNKNOWN
        }

    /** Paused (manually or via auto-pause) with a session still open — the only state real
     * system Ambient Mode is allowed to engage in, since it can make live shot detection stall. */
    private fun isPaused(): Boolean = autoPauseSecondsLeft >= 0 || (currentSession != null && !isDetecting)

    /** Dims the screen while tracking if energy efficiency is on, same as [applyScreenPowerMode]'s
     * manual fallback — but never hands off to real system Ambient Mode, since that can make the
     * sensor stall mid-session. This is the only power-saving path allowed during live detection. */
    private fun applyTrackingScreenMode() {
        brightenHandler.removeCallbacks(reDimRunnable)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (powerSavingEnabled) {
            dimScreenBrightness()
        } else {
            isScreenDimmed = false
            window.attributes = window.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        }
    }

    /** Forces a dimmed always-on screen when energy efficiency is on and the system doesn't
     * offer (or the user opted out of) working Ambient Mode. Only used while paused — real
     * system Ambient Mode is safe here since nothing is actively sensing shots. */
    private fun applyScreenPowerMode() {
        brightenHandler.removeCallbacks(reDimRunnable)
        if (!powerSavingEnabled) {
            isScreenDimmed = false
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            return
        }
        if (useSystemAod && ambientAvailability == AmbientAvailability.ENABLED) {
            isScreenDimmed = false
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dimScreenBrightness()
    }

    private fun dimScreenBrightness() {
        isScreenDimmed = true
        window.attributes = window.attributes.also { it.screenBrightness = dimBrightnessPercent / 100f }
    }

    private fun clearScreenPowerMode() {
        isScreenDimmed = false
        brightenHandler.removeCallbacks(reDimRunnable)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
    }

    /** Tapping the dimmed screen restores full brightness briefly so the wearer can read it. */
    private fun onScreenTapped() {
        if (!isScreenDimmed) return
        window.attributes = window.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        brightenHandler.removeCallbacks(reDimRunnable)
        brightenHandler.postDelayed(reDimRunnable, TAP_BRIGHTEN_DURATION_MS)
    }

    private fun startAutoPause() {
        stopDetection()
        applyScreenPowerMode()
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
        autoPauseSecondsLeft = autoPauseDuration
        autoPauseTimer = object : CountDownTimer(autoPauseDuration * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                autoPauseSecondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
            }
            override fun onFinish() {
                autoPauseSecondsLeft = -1
                autoPauseTimer = null
                startDetection()
                shotDetector.resetCooldown()
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.start()
    }

    private fun cancelAutoPause() {
        autoPauseTimer?.cancel()
        autoPauseTimer = null
        autoPauseSecondsLeft = -1
        clearScreenPowerMode()
    }

    private fun extendAutoPause(seconds: Int) {
        autoPauseTimer?.cancel()
        autoPauseSecondsLeft += seconds
        val remaining = autoPauseSecondsLeft * 1000L
        autoPauseTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                autoPauseSecondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
            }
            override fun onFinish() {
                autoPauseSecondsLeft = -1
                autoPauseTimer = null
                startDetection()
                shotDetector.resetCooldown()
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.start()
    }

    private fun onPrimaryButton() {
        when {
            currentSession == null -> {
                val now = System.currentTimeMillis()
                val session = Session(id = now, startTime = now, lastShotTime = now, shotCount = shotCount, shotsPerEndAtStart = shotsPerEnd, lastModified = now)
                currentSession = session
                if (shotCount > 0) sessions.add(0, session)
                dbExecutor.execute {
                    database.sessionDao().insertOrUpdate(session)
                    syncSessionToPhone(session, database.shotDao().getBySession(session.id))
                }
                pushActiveSessionState(session.id)
                startDetection()
            }
            autoPauseSecondsLeft >= 0 -> { cancelAutoPause(); startDetection(); shotDetector.resetCooldown() }
            isDetecting -> { stopDetection(); applyScreenPowerMode() }
            else -> startDetection()
        }
    }

    private fun onSecondaryButton() {
        if (autoPauseSecondsLeft >= 0) extendAutoPause(5) else startAutoPause()
    }

    private fun startDetection() {
        shotDetector.sensitivity = sensitivity
        shotDetector.customThreshold = customThreshold.toFloat()
        shotDetector.cooldownMs = shotCooldownSeconds * 1000L
        shotDetector.start()
        isDetecting = true
        applyTrackingScreenMode()
    }

    private fun stopDetection() {
        shotDetector.stop()
        isDetecting = false
        clearScreenPowerMode()
    }

    /** Pushes a DataItem to the phone companion app. Call from a background thread. */
    private fun syncSessionToPhone(session: Session, shots: List<Shot>) {
        val request = PutDataMapRequest.create("/session/${session.id}").apply {
            dataMap.putString("json", buildSessionJson(session, shots))
            dataMap.putLong("syncedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(request)
    }

    /** Lets the phone show a session as still in progress before it's ended, mirroring the
     * watch's own live History entry. Safe to call from the main thread. */
    private fun pushActiveSessionState(sessionId: Long?) {
        val request = PutDataMapRequest.create("/activeSession").apply {
            dataMap.putLong("sessionId", sessionId ?: -1L)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(request)
    }

    /** Full bidirectional reconcile: push every local session (tombstones included, so deletes
     * propagate) and merge in whatever the phone's DataItems currently hold. */
    private fun syncData() {
        dbExecutor.execute {
            val allSessions = database.sessionDao().getAllIncludingDeleted()
            val shotsBySession = database.shotDao().getAll().groupBy { it.sessionId }
            allSessions.forEach { session ->
                syncSessionToPhone(session, shotsBySession[session.id].orEmpty())
            }
        }
        Wearable.getDataClient(this).dataItems
            .addOnSuccessListener { buffer ->
                dbExecutor.execute {
                    buffer.forEach { item ->
                        if (item.uri.path.orEmpty().startsWith("/session")) {
                            DataMapItem.fromDataItem(item).dataMap.getString("json")?.let { json ->
                                try {
                                    val (session, shots) = parseSessionJson(json)
                                    database.mergeIncomingSession(session, shots)
                                } catch (_: Exception) {
                                    // skip malformed item, keep reconciling the rest
                                }
                            }
                        }
                    }
                    buffer.release()
                    val all = database.sessionDao().getAll()
                    runOnUiThread {
                        sessions.clear()
                        sessions.addAll(all)
                        phoneSyncStatus = getString(R.string.phone_sync_success, all.size)
                        phoneSyncStatusHandler.removeCallbacks(phoneSyncStatusHideRunnable)
                        phoneSyncStatusHandler.postDelayed(phoneSyncStatusHideRunnable, 6000)
                    }
                }
            }
    }

    private fun startClearData() {
        showClearDataConfirm = true
    }

    private fun cancelClearData() {
        showClearDataConfirm = false
    }

    /** Local wipe only — deliberately does not push a tombstone for every session, since that
     * would delete everything on the phone too on next sync. Sync afterwards to restore from
     * the phone if it still has the data. */
    private fun confirmClearData() {
        showClearDataConfirm = false
        cancelAutoPause()
        if (isDetecting) stopDetection()
        if (currentSession != null) pushActiveSessionState(null)
        currentSession = null
        shotCount = 0
        clearPendingSession()
        sessions.clear()
        dbExecutor.execute {
            database.clearAllLocalData()
            runOnUiThread {
                phoneSyncStatus = getString(R.string.clear_data_success)
                phoneSyncStatusHandler.removeCallbacks(phoneSyncStatusHideRunnable)
                phoneSyncStatusHandler.postDelayed(phoneSyncStatusHideRunnable, 3000)
            }
        }
    }

    private fun endSession() {
        cancelAutoPause()
        if (isDetecting) stopDetection()
        val session = currentSession ?: return
        if (shotCount > 0) {
            val now = System.currentTimeMillis()
            val updated = session.copy(lastShotTime = now, shotCount = shotCount, lastModified = now)
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
            dbExecutor.execute {
                database.sessionDao().insertOrUpdate(updated)
                syncSessionToPhone(updated, database.shotDao().getBySession(updated.id))
            }
        } else {
            sessions.removeIf { it.id == session.id }
            dbExecutor.execute { database.sessionDao().delete(session) }
        }
        pushActiveSessionState(null)
        currentSession = null
        shotCount = 0
        clearPendingSession()
    }

    private fun recordShot(magnitude: Float) {
        val session = currentSession ?: return
        val now = System.currentTimeMillis()
        val updated = session.copy(lastShotTime = now, shotCount = shotCount, lastModified = now)
        currentSession = updated
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
        dbExecutor.execute {
            database.sessionDao().insertOrUpdate(updated)
            database.shotDao().insert(Shot(sessionId = session.id, timestamp = now, magnitude = magnitude))
            syncSessionToPhone(updated, database.shotDao().getBySession(updated.id))
        }
        if (shotsPerEnd > 0 && autoPauseEnabled && shotCount % shotsPerEnd == 0) {
            startAutoPause()
        }
    }

    private fun manualAdjust(delta: Int) {
        val actualDelta = if (delta < 0) -minOf(-delta, shotCount) else delta
        if (actualDelta == 0) return
        val newCount = shotCount + actualDelta
        shotCount = newCount
        val now = System.currentTimeMillis()
        if (actualDelta > 0) {
            if (currentSession == null) {
                val session = Session(id = now, startTime = now, lastShotTime = now, shotCount = newCount, shotsPerEndAtStart = shotsPerEnd, lastModified = now)
                currentSession = session
                sessions.add(0, session)
                dbExecutor.execute {
                    database.sessionDao().insertOrUpdate(session)
                    repeat(actualDelta) { database.shotDao().insert(Shot(sessionId = session.id, timestamp = now, magnitude = null)) }
                    syncSessionToPhone(session, database.shotDao().getBySession(session.id))
                }
                pushActiveSessionState(session.id)
                return
            }
            val session = currentSession!!
            val updated = session.copy(lastShotTime = now, shotCount = newCount, lastModified = now)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
            dbExecutor.execute {
                database.sessionDao().insertOrUpdate(updated)
                repeat(actualDelta) { database.shotDao().insert(Shot(sessionId = session.id, timestamp = now, magnitude = null)) }
                syncSessionToPhone(updated, database.shotDao().getBySession(updated.id))
            }
        } else {
            val session = currentSession ?: return
            val updated = session.copy(shotCount = newCount, lastModified = now)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) {
                sessions[idx] = updated
                dbExecutor.execute {
                    database.sessionDao().insertOrUpdate(updated)
                    database.shotDao().deleteLatest(session.id, -actualDelta)
                    syncSessionToPhone(updated, database.shotDao().getBySession(updated.id))
                }
            }
        }
    }

    /** [original] is the session as it was before the edit dialog opened it, used only to detect
     * what changed (shot count, narrowed start/end) so the underlying shot rows can be kept
     * consistent with it — never persisted itself. */
    private fun editSession(original: Session, edited: Session) {
        val updated = edited.copy(lastModified = System.currentTimeMillis())
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            sessions[idx] = updated
            val countDelta = updated.shotCount - original.shotCount
            dbExecutor.execute {
                database.sessionDao().insertOrUpdate(updated)
                // Shots added/removed by hand through the counter (not real detections) are
                // timestamped at the session's end so they show up in the per-shot history too.
                if (countDelta > 0) {
                    repeat(countDelta) {
                        database.shotDao().insert(Shot(sessionId = updated.id, timestamp = updated.lastShotTime, magnitude = null))
                    }
                } else if (countDelta < 0) {
                    database.shotDao().deleteLatest(updated.id, -countDelta)
                }
                // Narrowing the window (end moved earlier / start moved later) can leave shots
                // outside it — pull those back inside instead of leaving them stranded.
                if (updated.lastShotTime < original.lastShotTime) {
                    database.shotDao().clampTimestampsAfter(updated.id, updated.lastShotTime)
                }
                if (updated.startTime > original.startTime) {
                    database.shotDao().clampTimestampsBefore(updated.id, updated.startTime)
                }
                syncSessionToPhone(updated, database.shotDao().getBySession(updated.id))
            }
        }
        if (currentSession?.id == updated.id) {
            currentSession = updated
            shotCount = updated.shotCount
        }
    }

    /** Tombstones rather than hard-deletes, so the deletion propagates to the phone instead of
     * the phone's still-existing copy getting re-synced back down and resurrecting it. */
    private fun deleteSession(session: Session) {
        sessions.removeIf { it.id == session.id }
        val now = System.currentTimeMillis()
        val tombstone = session.copy(deletedAt = now, lastModified = now)
        dbExecutor.execute {
            database.sessionDao().insertOrUpdate(tombstone)
            database.shotDao().deleteAllForSession(session.id)
            syncSessionToPhone(tombstone, emptyList())
        }
        if (currentSession?.id == session.id) {
            if (isDetecting) stopDetection()
            currentSession = null
            shotCount = 0
            clearPendingSession()
        }
    }

    private fun clearPendingSession() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            remove(KEY_PENDING_ID)
            remove(KEY_PENDING_START)
            remove(KEY_PENDING_LAST)
            remove(KEY_PENDING_COUNT)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDetecting) shotDetector.stop()
        cancelAutoPause()
    }

    override fun onResume() {
        super.onResume()
        if (isDetecting) shotDetector.start()
    }

    override fun onStop() {
        super.onStop()
        val session = currentSession
        if (session != null && shotCount > 0) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                putLong(KEY_PENDING_ID, session.id)
                putLong(KEY_PENDING_START, session.startTime)
                putLong(KEY_PENDING_LAST, session.lastShotTime)
                putInt(KEY_PENDING_COUNT, shotCount)
            }
        } else {
            clearPendingSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoPause()
        magnitudeHandler.removeCallbacks(magnitudeHideRunnable)
        phoneSyncStatusHandler.removeCallbacks(phoneSyncStatusHideRunnable)
        brightenHandler.removeCallbacks(reDimRunnable)
        shotDetector.stop()
        dbExecutor.shutdown()
    }
}

@Composable
fun ArcheryApp(
    shotCount: Int,
    isDetecting: Boolean,
    isAmbient: Boolean,
    currentSession: Session?,
    sessions: List<Session>,
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
    paletteChoice: PaletteChoice,
    counterSize: CounterSize,
    autoPauseSecondsLeft: Int,
    lastShotMagnitude: Float?,
    phoneSyncStatus: String?,
    onSyncData: () -> Unit,
    onScreenTap: () -> Unit,
    showClearDataConfirm: Boolean,
    onClearData: () -> Unit,
    onConfirmClearData: () -> Unit,
    onCancelClearData: () -> Unit,
    onStartOrToggle: () -> Unit,
    onSecondaryButton: () -> Unit,
    onEnd: () -> Unit,
    onManualAdjust: (Int) -> Unit,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit,
    onShotCooldownChange: (Int) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (PaletteChoice) -> Unit,
    onCounterSizeChange: (CounterSize) -> Unit,
    onEditSession: (Session, Session) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onShotsPerEndChange: (Int) -> Unit,
    onAutoPauseEnabledChange: (Boolean) -> Unit,
    onAutoPauseDurationChange: (Int) -> Unit,
    onPowerSavingEnabledChange: (Boolean) -> Unit,
    onUseSystemAodChange: (Boolean) -> Unit,
    onDimBrightnessPercentChange: (Int) -> Unit,
    detailSession: Session?,
    detailShots: List<Shot>,
    onShowDetail: (Session) -> Unit,
    onDismissDetail: () -> Unit,
    editingSession: Session?,
    onShowEdit: (Session) -> Unit,
    onDismissEdit: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showCounterSizePicker by remember { mutableStateOf(false) }
    var showDetectionSettings by remember { mutableStateOf(false) }
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showAppearanceLanguageSettings by remember { mutableStateOf(false) }
    var showDataSettings by remember { mutableStateOf(false) }
    AppScaffold {
        if (isAmbient) {
            AmbientScreen(
                shotCount = shotCount,
                currentSession = currentSession,
                isDetecting = isDetecting
            )
            return@AppScaffold
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onScreenTap) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        onScreenTap()
                    }
                }
        ) {
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> HistoryScreen(
                        sessions = sessions,
                        currentSession = currentSession,
                        activeShotCount = shotCount,
                        onShowDetail = onShowDetail,
                        onShowEdit = onShowEdit
                    )
                    1 -> MainScreen(
                        shotCount = shotCount,
                        isDetecting = isDetecting,
                        currentSession = currentSession,
                        shotsPerEnd = shotsPerEnd,
                        autoPauseEnabled = autoPauseEnabled,
                        autoPauseSecondsLeft = autoPauseSecondsLeft,
                        lastShotMagnitude = lastShotMagnitude,
                        onPrimaryButton = onStartOrToggle,
                        onSecondaryButton = onSecondaryButton,
                        onEnd = onEnd,
                        onManualAdjust = onManualAdjust,
                        counterSize = counterSize
                    )
                    2 -> SettingsMenuScreen(
                        onShowDetection = { showDetectionSettings = true },
                        onShowDisplay = { showDisplaySettings = true },
                        onShowAppearanceLanguage = { showAppearanceLanguageSettings = true },
                        onShowData = { showDataSettings = true }
                    )
                }
            }
            HorizontalPageIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            if (detailSession != null) {
                ShotDetailScreen(
                    session = detailSession,
                    shots = detailShots,
                    onDismiss = onDismissDetail
                )
            }
            if (editingSession != null) {
                EditSessionScreen(
                    session = editingSession,
                    onSave = { updated ->
                        onEditSession(editingSession, updated)
                        onDismissEdit()
                    },
                    onDelete = {
                        onDeleteSession(editingSession)
                        onDismissEdit()
                    },
                    onDismiss = onDismissEdit
                )
            }
            if (showDetectionSettings) {
                DetectionSettingsScreen(
                    sensitivity = sensitivity,
                    customThreshold = customThreshold,
                    shotCooldownSeconds = shotCooldownSeconds,
                    shotsPerEnd = shotsPerEnd,
                    autoPauseEnabled = autoPauseEnabled,
                    autoPauseDuration = autoPauseDuration,
                    onSensitivityChange = onSensitivityChange,
                    onCustomThresholdChange = onCustomThresholdChange,
                    onShotCooldownChange = onShotCooldownChange,
                    onShotsPerEndChange = onShotsPerEndChange,
                    onAutoPauseEnabledChange = onAutoPauseEnabledChange,
                    onAutoPauseDurationChange = onAutoPauseDurationChange,
                    onDismiss = { showDetectionSettings = false }
                )
            }
            if (showDisplaySettings) {
                DisplaySettingsScreen(
                    powerSavingEnabled = powerSavingEnabled,
                    useSystemAod = useSystemAod,
                    dimBrightnessPercent = dimBrightnessPercent,
                    onPowerSavingEnabledChange = onPowerSavingEnabledChange,
                    onUseSystemAodChange = onUseSystemAodChange,
                    onDimBrightnessPercentChange = onDimBrightnessPercentChange,
                    onDismiss = { showDisplaySettings = false }
                )
            }
            if (showAppearanceLanguageSettings) {
                AppearanceLanguageSettingsScreen(
                    currentLanguage = currentLanguage,
                    paletteChoice = paletteChoice,
                    counterSize = counterSize,
                    onShowLanguagePicker = { showLanguagePicker = true },
                    onShowThemePicker = { showThemePicker = true },
                    onShowCounterSizePicker = { showCounterSizePicker = true },
                    onDismiss = { showAppearanceLanguageSettings = false }
                )
            }
            if (showDataSettings) {
                DataSettingsScreen(
                    phoneSyncStatus = phoneSyncStatus,
                    onSyncData = onSyncData,
                    onClearData = onClearData,
                    onDismiss = { showDataSettings = false }
                )
            }
            if (showLanguagePicker) {
                LanguagePickerScreen(
                    currentLanguage = currentLanguage,
                    onSelect = { lang ->
                        showLanguagePicker = false
                        onLanguageChange(lang)
                    },
                    onDismiss = { showLanguagePicker = false }
                )
            }
            if (showThemePicker) {
                ThemePickerScreen(
                    currentPalette = paletteChoice,
                    onSelect = { choice ->
                        showThemePicker = false
                        onThemeChange(choice)
                    },
                    onDismiss = { showThemePicker = false }
                )
            }
            if (showCounterSizePicker) {
                CounterSizePickerScreen(
                    currentSize = counterSize,
                    onSelect = { size ->
                        showCounterSizePicker = false
                        onCounterSizeChange(size)
                    },
                    onDismiss = { showCounterSizePicker = false }
                )
            }
            if (showClearDataConfirm) {
                ClearDataConfirmDialog(
                    onConfirm = onConfirmClearData,
                    onCancel = onCancelClearData
                )
            }
        }
    }
}
