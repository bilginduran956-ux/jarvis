package com.jarvis.android.core.model

/**
 * Arayüz bu durumları doğrudan Türkçe gösterir. Core, UI'a bağlı değildir.
 */
enum class JarvisStatus(val label: String) {
    READY("JARVIS HAZIR"),
    LISTENING("DİNLİYORUM…"),
    UNDERSTANDING("ANLIYORUM…"),
    PLANNING("İŞLEM PLANLANIYOR…"),
    WORKING("İŞLEM YAPILIYOR…"),
    VERIFYING("KONTROL EDİLİYOR…"),
    COMPLETE("İŞLEM TAMAMLANDI"),
    WARNING("UYARI"),
    ERROR("HATA")
}

enum class SystemPanel {
    WIFI,
    BLUETOOTH,
    NFC,
    NETWORK,
    SOUND,
    DISPLAY,
    STORAGE,
    APPLICATIONS,
    SECURITY,
    NOTIFICATION_ACCESS,
    LOCATION,
    GENERAL_SETTINGS
}

sealed interface JarvisAction {
    val label: String

    data object DeviceReport : JarvisAction {
        override val label = "Telefon durumunu analiz et"
    }

    data class OpenApp(val query: String) : JarvisAction {
        override val label = "$query uygulamasını aç"
    }

    data class OpenSystemPanel(val panel: SystemPanel) : JarvisAction {
        override val label = panel.name + " sistem panelini aç"
    }

    data class SetMediaVolume(val percent: Int) : JarvisAction {
        override val label = "Medya sesini %$percent yap"
    }

    data class SetTorch(val enabled: Boolean) : JarvisAction {
        override val label = if (enabled) "Feneri aç" else "Feneri kapat"
    }

    data object OpenCamera : JarvisAction {
        override val label = "Kamerayı aç"
    }

    data object OpenDialer : JarvisAction {
        override val label = "Telefon uygulamasını aç"
    }

    data object OpenFiles : JarvisAction {
        override val label = "Dosya seçiciyi aç"
    }

    data object PlayMusic : JarvisAction {
        override val label = "Müzik oynatıcıyı aç"
    }

    data class SearchWeb(val query: String) : JarvisAction {
        override val label = "Web'de araştır: $query"
    }

    data class SetAlarm(val hour: Int, val minute: Int, val message: String) : JarvisAction {
        override val label = "%02d:%02d alarmını hazırla".format(hour, minute)
    }

    data class CreateCalendarDraft(val title: String) : JarvisAction {
        override val label = "Takvim taslağı oluştur: $title"
    }

    data class DraftMessage(val recipient: String?, val message: String?) : JarvisAction {
        override val label = "Mesaj taslağını aç"
    }

    data class SaveMemory(val value: String) : JarvisAction {
        override val label = "Hafızaya kaydet"
    }

    data class ForgetMemory(val query: String) : JarvisAction {
        override val label = "Hafızadan kaldır"
    }

    data object NotificationSummary : JarvisAction {
        override val label = "Bildirim erişimini yönet"
    }

    data class Explain(val message: String) : JarvisAction {
        override val label = "Bilgi ver"
    }
}

data class CommandPlan(
    val rawCommand: String,
    val normalizedCommand: String,
    val actions: List<JarvisAction>,
    val shortSummary: String
)

data class ActionResult(
    val action: JarvisAction,
    val success: Boolean,
    val message: String,
    val needsUserAction: Boolean = false
)

data class CoreOutcome(
    val plan: CommandPlan,
    val results: List<ActionResult>,
    val reply: String,
    val deviceSnapshot: DeviceSnapshot? = null
)

data class ConversationEntry(
    val id: Long,
    val role: ConversationRole,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class ConversationRole {
    USER,
    JARVIS,
    SYSTEM
}

data class MemoryNote(
    val id: Long,
    val value: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class DeviceMetric(
    val value: String,
    val detail: String,
    val level: MetricLevel = MetricLevel.NORMAL,
    val available: Boolean = true
)

enum class MetricLevel {
    NORMAL,
    WARNING,
    CRITICAL,
    UNKNOWN
}

data class NetworkState(
    val wifi: String,
    val mobile: String,
    val bluetooth: String,
    val nfc: String,
    val vpn: String
)

data class DeviceSnapshot(
    val capturedAtMillis: Long,
    val cpu: DeviceMetric,
    val ram: DeviceMetric,
    val storage: DeviceMetric,
    val battery: DeviceMetric,
    val temperature: DeviceMetric,
    val gpu: DeviceMetric,
    val network: NetworkState,
    val installedAppCount: Int
)

data class JarvisPreferences(
    val operatorName: String = "Komutan",
    val speechEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val selectedAiMode: AiMode = AiMode.HYBRID
)

enum class AiMode(val label: String) {
    LOCAL("Yerel AI"),
    ONLINE("Çevrimiçi AI"),
    HYBRID("Hibrit AI")
}
