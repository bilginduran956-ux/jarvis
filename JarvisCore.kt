package com.jarvis.android.core

import com.jarvis.android.core.command.TurkishCommandParser
import com.jarvis.android.core.device.AndroidActionGateway
import com.jarvis.android.core.device.DeviceMonitor
import com.jarvis.android.core.model.ActionResult
import com.jarvis.android.core.model.CommandPlan
import com.jarvis.android.core.model.ConversationEntry
import com.jarvis.android.core.model.ConversationRole
import com.jarvis.android.core.model.CoreOutcome
import com.jarvis.android.core.model.DeviceSnapshot
import com.jarvis.android.core.model.JarvisAction
import com.jarvis.android.data.JarvisMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JARVIS çalışma hattı:
 * kullanıcı -> anlama -> plan -> araç seçimi -> işlem -> kontrol -> yanıt.
 *
 * Burada araçlar doğrudan serbest metin değil, tipli [JarvisAction] planı alır.
 * Bu sınır ileride local/online AI sağlayıcıları eklense dahi izin ve güvenlik
 * kontrolünün tek noktada kalmasını sağlar.
 */
class JarvisCore(
    private val parser: TurkishCommandParser,
    private val actionGateway: AndroidActionGateway,
    private val deviceMonitor: DeviceMonitor,
    private val memoryStore: JarvisMemoryStore
) {
    private var previousSubject: String? = null

    suspend fun process(command: String): CoreOutcome = withContext(Dispatchers.Default) {
        val plan = parser.plan(command, previousSubject)
        memoryStore.appendConversation(
            ConversationEntry(
                id = System.nanoTime(),
                role = ConversationRole.USER,
                text = command
            )
        )

        val results = mutableListOf<ActionResult>()
        var snapshot: DeviceSnapshot? = null
        plan.actions.forEach { action ->
            val result = when (action) {
                JarvisAction.DeviceReport -> {
                    snapshot = deviceMonitor.snapshot()
                    ActionResult(
                        action,
                        true,
                        "Sistem raporu yenilendi. RAM " + snapshot?.ram?.detail +
                            ", depolama " + snapshot?.storage?.detail + "."
                    )
                }
                is JarvisAction.SaveMemory -> {
                    memoryStore.saveNote(action.value)
                    previousSubject = action.value
                    ActionResult(action, true, "Bunu yerel hafızaya kaydettim.")
                }
                is JarvisAction.ForgetMemory -> {
                    val removed = memoryStore.forget(action.query)
                    ActionResult(
                        action,
                        removed > 0,
                        if (removed > 0) removed.toString() + " hafıza kaydı kaldırıldı."
                        else "Bu ifadeyle eşleşen bir hafıza kaydı bulamadım."
                    )
                }
                is JarvisAction.Explain -> ActionResult(action, true, action.message)
                is JarvisAction.SearchWeb -> {
                    previousSubject = action.query
                    withContext(Dispatchers.Main) { actionGateway.execute(action) }
                }
                else -> withContext(Dispatchers.Main) { actionGateway.execute(action) }
            }
            results += result
        }

        val reply = replyFor(plan, results)
        memoryStore.appendConversation(
            ConversationEntry(
                id = System.nanoTime(),
                role = ConversationRole.JARVIS,
                text = reply
            )
        )
        CoreOutcome(plan = plan, results = results, reply = reply, deviceSnapshot = snapshot)
    }

    private fun replyFor(plan: CommandPlan, results: List<ActionResult>): String {
        val completed = results.count { it.success }
        val issues = results.filterNot { it.success }
        val approved = results.filter { it.needsUserAction && !it.success }
        return when {
            results.isEmpty() -> "Plan oluşturuldu ancak çalıştırılacak bir araç bulunamadı."
            issues.isEmpty() -> {
                if (plan.actions.size == 1) results.first().message
                else completed.toString() + " işlem tamamlandı. " + results.joinToString(" ") { it.message }
            }
            completed > 0 -> completed.toString() + " işlem tamamlandı. " +
                issues.joinToString(" ") { it.message } +
                if (approved.isNotEmpty()) " Gerekli adımı Android sistem panelinde onaylayabilirsiniz." else ""
            else -> issues.joinToString(" ") { it.message }
        }
    }
}
