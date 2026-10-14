package com.jarvis.android.core.device

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import com.jarvis.android.core.model.DeviceMetric
import com.jarvis.android.core.model.DeviceSnapshot
import com.jarvis.android.core.model.MetricLevel
import com.jarvis.android.core.model.NetworkState

/**
 * Android SDK'nin uygulamalara güvenilir biçimde açtığı metrikleri toplar.
 * CPU/GPU yüzde ve üreticiye özgü ısı sensörleri standart uygulamalara her zaman açık değildir;
 * bu nedenle bilinmeyen veriyi tahmin etmez.
 */
class DeviceMonitor(private val context: Context) {
    fun snapshot(): DeviceSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val totalRam = memoryInfo.totalMem
        val usedRam = (totalRam - memoryInfo.availMem).coerceAtLeast(0)
        val ramPercent = if (totalRam > 0) (usedRam * 100 / totalRam).toInt() else 0

        val stats = StatFs(Environment.getDataDirectory().absolutePath)
        val totalStorage = stats.blockCountLong * stats.blockSizeLong
        val availableStorage = stats.availableBlocksLong * stats.blockSizeLong
        val usedStorage = (totalStorage - availableStorage).coerceAtLeast(0)
        val storagePercent = if (totalStorage > 0) (usedStorage * 100 / totalStorage).toInt() else 0

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPercent = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) level * 100 / scale else -1
        } ?: -1
        val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) in setOf(
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL
        )
        val temperatureRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperature = temperatureRaw?.takeIf { it != Int.MIN_VALUE }?.div(10f)

        return DeviceSnapshot(
            capturedAtMillis = System.currentTimeMillis(),
            cpu = DeviceMetric(
                value = "—",
                detail = "Android standart uygulamalara güvenilir canlı CPU yüzdesi sunmaz",
                level = MetricLevel.UNKNOWN,
                available = false
            ),
            ram = DeviceMetric(
                value = Formatter.formatFileSize(context, usedRam) + " / " + Formatter.formatFileSize(context, totalRam),
                detail = "%$ramPercent kullanım",
                level = if (ramPercent >= 88) MetricLevel.WARNING else MetricLevel.NORMAL
            ),
            storage = DeviceMetric(
                value = Formatter.formatFileSize(context, usedStorage) + " / " + Formatter.formatFileSize(context, totalStorage),
                detail = "%$storagePercent kullanım · " + Formatter.formatFileSize(context, availableStorage) + " boş",
                level = when {
                    storagePercent >= 92 -> MetricLevel.CRITICAL
                    storagePercent >= 82 -> MetricLevel.WARNING
                    else -> MetricLevel.NORMAL
                }
            ),
            battery = DeviceMetric(
                value = if (batteryPercent >= 0) "%$batteryPercent" else "Bilinmiyor",
                detail = if (isCharging) "Şarj oluyor" else "Şarjda değil",
                level = when {
                    batteryPercent in 0..14 -> MetricLevel.CRITICAL
                    batteryPercent in 15..29 -> MetricLevel.WARNING
                    else -> MetricLevel.NORMAL
                },
                available = batteryPercent >= 0
            ),
            temperature = DeviceMetric(
                value = temperature?.let { "%.1f°C".format(it) } ?: "Bilinmiyor",
                detail = "Batarya sensörü",
                level = when {
                    temperature == null -> MetricLevel.UNKNOWN
                    temperature >= 45f -> MetricLevel.CRITICAL
                    temperature >= 40f -> MetricLevel.WARNING
                    else -> MetricLevel.NORMAL
                },
                available = temperature != null
            ),
            gpu = DeviceMetric(
                value = "—",
                detail = "GPU kullanımı standart Android API'sinde uygulamalara açılmaz",
                level = MetricLevel.UNKNOWN,
                available = false
            ),
            network = readNetwork(),
            installedAppCount = context.packageManager.getInstalledApplications(0).size
        )
    }

    private fun readNetwork(): NetworkState {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities)
        val wifi = if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) "BAĞLI" else "KAPALI"
        val mobile = if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) "AKTİF" else "KAPALI"
        val vpn = if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) "BAĞLI" else "KAPALI"
        return NetworkState(
            wifi = wifi,
            mobile = mobile,
            bluetooth = bluetoothState(),
            nfc = nfcState(),
            vpn = vpn
        )
    }

    @Suppress("DEPRECATION")
    private fun bluetoothState(): String = runCatching {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return "DESTEK YOK"
        if (adapter.isEnabled) "AÇIK" else "KAPALI"
    }.getOrElse { "İZİN GEREKİYOR" }

    private fun nfcState(): String = runCatching {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return "DESTEK YOK"
        if (adapter.isEnabled) "AÇIK" else "KAPALI"
    }.getOrElse { "ERİŞİM YOK" }
}
