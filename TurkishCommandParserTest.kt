package com.jarvis.android.core.command

import com.jarvis.android.core.model.JarvisAction
import com.jarvis.android.core.model.SystemPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishCommandParserTest {
    private val parser = TurkishCommandParser()

    @Test
    fun complexPhoneCommandIsSplitIntoAnOrderedPlan() {
        val plan = parser.plan(
            "Wi-Fi'yi aç, Bluetooth'u aç, sesi yüzde 30'a getir ve müziği başlat."
        )

        assertEquals(
            listOf(
                JarvisAction.OpenSystemPanel(SystemPanel.WIFI),
                JarvisAction.OpenSystemPanel(SystemPanel.BLUETOOTH),
                JarvisAction.SetMediaVolume(30),
                JarvisAction.PlayMusic
            ),
            plan.actions
        )
    }

    @Test
    fun contextualResearchUsesPreviousSubject() {
        val plan = parser.plan("Bunu araştır", previousSubject = "Android 15 pil optimizasyonu")

        assertTrue(plan.actions.contains(JarvisAction.SearchWeb("Android 15 pil optimizasyonu")))
    }

    @Test
    fun wifiCommandOpensOfficialSettingsPanel() {
        val plan = parser.plan("Wi-Fi'yi aç")

        assertEquals(JarvisAction.OpenSystemPanel(SystemPanel.WIFI), plan.actions.single())
    }
}
