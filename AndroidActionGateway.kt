package com.jarvis.android.core.device

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.SearchManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.android.core.model.ActionResult
import com.jarvis.android.core.model.JarvisAction
import com.jarvis.android.core.model.SystemPanel

/**
 * Sadece Android'in herkese açık, desteklenen niyetleri ve API'leri kullanılır.
 * Kısıtlı ayarlarda doğrudan değişiklik yapmak yerine resmî sistem ekranı açılır.
 */
class AndroidActionGateway(private val context: Context) {
    fun execute(action: JarvisAction): ActionResult = when (action) {
        is JarvisAction.OpenApp -> openApp(action)
        is JarvisAction.OpenSystemPanel -> openPanel(action)
        is JarvisAction.SetMediaVolume -> setVolume(action)
        is JarvisAction.SetTorch -> setTorch(action)
        JarvisAction.OpenCamera -> launch(
            action,
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            "Kamera açıldı."
        )
        JarvisAction.OpenDialer -> launch(action, Intent(Intent.ACTION_DIAL), "Telefon uygulaması açıldı.")
        JarvisAction.OpenFiles -> launch(
            action,
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            },
            "Dosya seçici açıldı."
        )
        JarvisAction.PlayMusic -> launch(
            action,
            Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER),
            "Müzik oynatıcı açıldı."
        )
        is JarvisAction.SearchWeb -> launch(
            action,
            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, action.query),
            "Araştırma için tarayıcı araması açıldı."
        )
        is JarvisAction.SetAlarm -> launch(
            action,
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false),
            "Alarm uygulamasında onaylanmaya hazır."
        )
        is JarvisAction.CreateCalendarDraft -> launch(
            action,
            Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, action.title),
            "Takvim etkinliği taslağı açıldı."
        )
        is JarvisAction.DraftMessage -> launch(
            action,
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                action.recipient?.let { putExtra("address", it) }
                action.message?.let { putExtra("sms_body", it) }
            },
            "Mesaj taslağı açıldı. Göndermeden önce siz onaylarsınız."
        )
        JarvisAction.NotificationSummary -> openPanel(
            JarvisAction.OpenSystemPanel(SystemPanel.NOTIFICATION_ACCESS)
        )
        is JarvisAction.DeviceReport,
        is JarvisAction.SaveMemory,
        is JarvisAction.ForgetMemory,
        is JarvisAction.Explain -> ActionResult(action, true, "Core tarafından işlendi.")
    }

    private fun openApp(action: JarvisAction.OpenApp): ActionResult {
        val packageManager = context.packageManager
        val target = packageManager.getInstalledApplications(0).firstOrNull { app ->
            packageManager.getApplicationLabel(app).toString().contains(action.query, ignoreCase = true)
        }
        if (target == null) {
            return ActionResult(
                action,
                success = false,
                message = action.query + " bu cihazda bulunamadı veya Android uygulama görünürlüğü bunu listelemeye izin vermedi.",
                needsUserAction = true
            )
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(target.packageName)
        return if (launchIntent == null) {
            ActionResult(action, false, "Bu uygulama başlatılabilir bir giriş noktası sunmuyor.", true)
        } else {
            launch(action, launchIntent, action.query + " açıldı.")
        }
    }

    private fun openPanel(action: JarvisAction.OpenSystemPanel): ActionResult {
        val intent = when (action.panel) {
            SystemPanel.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
            SystemPanel.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            SystemPanel.NFC -> Intent(Settings.ACTION_NFC_SETTINGS)
            SystemPanel.NETWORK -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            SystemPanel.SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
            SystemPanel.DISPLAY -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            SystemPanel.STORAGE -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            SystemPanel.APPLICATIONS -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            SystemPanel.SECURITY -> Intent(Settings.ACTION_SECURITY_SETTINGS)
            SystemPanel.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            SystemPanel.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            SystemPanel.GENERAL_SETTINGS -> Intent(Settings.ACTION_SETTINGS)
        }
        return launch(
            action,
            intent,
            "Android güvenlik kuralları nedeniyle ilgili resmî ayar paneli açıldı; değişikliği orada onaylayabilirsiniz."
        )
    }

    private fun setVolume(action: JarvisAction.SetMediaVolume): ActionResult = runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * action.percent / 100f).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        ActionResult(action, true, "Medya sesi %" + action.percent + " olarak ayarlandı.")
    }.getOrElse {
        ActionResult(action, false, "Medya sesi değiştirilemedi: " + (it.message ?: "bilinmeyen hata"), true)
    }

    private fun setTorch(action: JarvisAction.SetTorch): ActionResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return ActionResult(action, false, "Fener için Kamera izni gerekiyor.", true)
        }
        return runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult(action, false, "Bu cihazda kullanılabilir fener bulunamadı.", true)
            manager.setTorchMode(cameraId, action.enabled)
            ActionResult(action, true, if (action.enabled) "Fener açıldı." else "Fener kapatıldı.")
        }.getOrElse {
            ActionResult(action, false, "Fener değiştirilemedi: " + (it.message ?: "bilinmeyen hata"), true)
        }
    }

    private fun launch(action: JarvisAction, sourceIntent: Intent, message: String): ActionResult = runCatching {
        sourceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(sourceIntent)
        ActionResult(action, true, message)
    }.getOrElse {
        ActionResult(
            action,
            false,
            "Bu işlem için desteklenen bir uygulama bulunamadı: " + (it.message ?: "bilinmeyen hata"),
            true
        )
    }
}
