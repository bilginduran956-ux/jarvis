package com.jarvis.android.core.command

import com.jarvis.android.core.model.CommandPlan
import com.jarvis.android.core.model.JarvisAction
import com.jarvis.android.core.model.SystemPanel
import java.text.Normalizer

/**
 * Ağ bağımsız, açıklanabilir ilk katman. Karmaşık niyetler daha sonra online/local
 * LLM sağlayıcısına devredilebilir; cihaz eylemleri yine bu tipli planla yürütülür.
 */
class TurkishCommandParser {
    fun plan(command: String, previousSubject: String? = null): CommandPlan {
        val clean = command.trim()
        val normalized = normalize(clean)
        val actions = mutableListOf<JarvisAction>()

        if (containsAny(normalized, "telefonumu kontrol et", "sistem raporu", "durum raporu", "kendini kontrol et")) {
            actions += JarvisAction.DeviceReport
        }

        if (containsAny(normalized, "wifi", "wi fi")) {
            if (containsAny(normalized, "ac", "baglan", "etkinlestir")) {
                actions += JarvisAction.OpenSystemPanel(SystemPanel.WIFI)
            }
        }
        if (normalized.contains("bluetooth") && containsAny(normalized, "ac", "baglan", "etkinlestir")) {
            actions += JarvisAction.OpenSystemPanel(SystemPanel.BLUETOOTH)
        }
        if (normalized.contains("nfc") && containsAny(normalized, "ac", "etkinlestir")) {
            actions += JarvisAction.OpenSystemPanel(SystemPanel.NFC)
        }
        if (containsAny(normalized, "mobil veri", "internet ayarlari", "ag durumu", "baglanti durumu")) {
            actions += JarvisAction.OpenSystemPanel(SystemPanel.NETWORK)
        }
        if (containsAny(normalized, "bildirimlerimi ozetle", "bildirimleri ozetle", "bildirim erisimi")) {
            actions += JarvisAction.NotificationSummary
        }

        val volume = Regex("""(?:ses(?:i|ini)?|medya sesi).*?%?\s*(\d{1,3})""").find(normalized)
        if (volume != null) {
            actions += JarvisAction.SetMediaVolume(volume.groupValues[1].toInt().coerceIn(0, 100))
        } else if (containsAny(normalized, "sesi azalt", "ses azalt")) {
            actions += JarvisAction.OpenSystemPanel(SystemPanel.SOUND)
        }

        if (containsAny(normalized, "feneri ac", "fener ac")) actions += JarvisAction.SetTorch(true)
        if (containsAny(normalized, "feneri kapat", "fener kapat")) actions += JarvisAction.SetTorch(false)
        if (containsAny(normalized, "kamerayi ac", "kamera ac")) actions += JarvisAction.OpenCamera
        if (containsAny(normalized, "telefonu ac", "telefon uygulamasini ac", "arama ekranini ac")) actions += JarvisAction.OpenDialer
        if (containsAny(normalized, "dosyalari ac", "dosya yoneticisini ac", "indirilenler klasorunu ac")) actions += JarvisAction.OpenFiles
        if (containsAny(normalized, "muzigi baslat", "muzik baslat", "muzik cal")) actions += JarvisAction.PlayMusic
        if (containsAny(normalized, "ayarlari ac", "ayarlar ac", "sistem ayarlarini ac")) {
            actions += JarvisAction.OpenSystemPanel(SystemPanel.GENERAL_SETTINGS)
        }

        appAction(normalized)?.let(actions::add)

        if (isPronounOnly(normalized) && previousSubject != null) {
            actions += JarvisAction.SearchWeb(previousSubject)
        }
        Regex("""(?:internetten |webde )?(?:ara|arastir)\s*[:,-]?\s*(.+)""").find(normalized)?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { actions += JarvisAction.SearchWeb(if (isPronounOnly(normalize(it))) previousSubject ?: it else it) }

        Regex("""(?:alarm kur|alarm koy)\s*[:,-]?\s*(\d{1,2})[:.](\d{2})""").find(normalized)?.let { result ->
            val hour = result.groupValues[1].toIntOrNull()
            val minute = result.groupValues[2].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                actions += JarvisAction.SetAlarm(hour, minute, "JARVIS alarmı")
            }
        }

        Regex("""(?:takvime ekle|toplanti ekle|etkinlik ekle)\s*[:,-]?\s*(.+)""").find(normalized)?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { actions += JarvisAction.CreateCalendarDraft(it) }

        Regex("""(?:hatirla|bunu hatirla|hafizana al)\s*[:,-]?\s*(.+)""").find(normalized)?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { actions += JarvisAction.SaveMemory(it) }

        Regex("""(?:unut|bunu unut|hafizadan sil)\s*[:,-]?\s*(.*)""").find(normalized)?.let { result ->
            actions += JarvisAction.ForgetMemory(result.groupValues.getOrElse(1) { "" }.trim())
        }

        if (containsAny(normalized, "mesaj yaz", "mesaj hazirla")) {
            actions += JarvisAction.DraftMessage(recipient = null, message = null)
        }

        if (actions.isEmpty()) {
            actions += JarvisAction.Explain(
                "Bu komut için henüz yerel bir araç eşleştirmesi yok. " +
                    "Komutu daha açık söyleyebilir veya çevrimiçi AI sağlayıcısı yapılandırabilirsiniz."
            )
        }

        return CommandPlan(
            rawCommand = clean,
            normalizedCommand = normalized,
            actions = actions.distinct(),
            shortSummary = actions.size.toString() + " işlem planlandı"
        )
    }

    private fun appAction(normalized: String): JarvisAction.OpenApp? {
        val apps = listOf(
            "youtube" to "YouTube",
            "whatsapp" to "WhatsApp",
            "google maps" to "Google Maps",
            "haritalar" to "Google Maps",
            "spotify" to "Spotify",
            "chrome" to "Chrome",
            "galeri" to "Galeri"
        )
        val app = apps.firstOrNull { (key, _) ->
            normalized.contains(key) && containsAny(normalized, "ac", "baslat", "goster", "gec")
        } ?: return null
        return JarvisAction.OpenApp(app.second)
    }

    private fun containsAny(text: String, vararg terms: String): Boolean = terms.any(text::contains)

    private fun isPronounOnly(value: String): Boolean = value in setOf("bunu", "bunu arastir", "onu", "onu arastir")

    private fun normalize(value: String): String {
        val replacements = value
            .lowercase()
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
        return Normalizer.normalize(replacements, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .replace('-', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
