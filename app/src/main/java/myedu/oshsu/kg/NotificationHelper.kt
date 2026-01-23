package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

object NotificationHelper {
    
    fun getLocalizedContext(context: Context, prefs: PrefsManager): Context {
        val lang = prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun checkForUpdates(
        oldList: List<SessionResponse>,
        newList: List<SessionResponse>,
        context: Context
    ): Pair<List<String>, List<String>> {
        val gradeUpdates = ArrayList<String>()
        val portalUpdates = ArrayList<String>()
        val lang = context.resources.configuration.locales.get(0).language
        
        // FIX: Map by Marklist ID to handle duplicate subjects (e.g. retakes vs main course)
        // If marklist is null (rare), we fall back to a composite key of subject ID to attempt uniqueness
        val oldMap = oldList.flatMap { it.subjects ?: emptyList() }
            .associateBy { wrapper ->
                wrapper.marklist?.id?.toString() ?: "sub_${wrapper.subject?.id ?: -1}"
            }

        newList.flatMap { it.subjects ?: emptyList() }.forEach { newWrapper ->
            val key = newWrapper.marklist?.id?.toString() ?: "sub_${newWrapper.subject?.id ?: -1}"
            val name = newWrapper.subject?.get(lang) ?: context.getString(R.string.unknown)
            
            val oldWrapper = oldMap[key]
            
            val oldMarks = oldWrapper?.marklist
            val newMarks = newWrapper.marklist
            
            fun check(label: String, oldVal: Double?, newVal: Double?) {
                val v2 = newVal ?: 0.0
                val v1 = oldVal ?: 0.0
                
                if (v2 > v1 && v2 > 0) {
                    // STRICT CHECK: Only notify if we found a matching previous record.
                    // This prevents notifications when the app is first installed or data is cleared,
                    // and crucially prevents "0 -> >0 " spam if the sorting of duplicates flips.
                    if (oldWrapper != null) {
                        gradeUpdates.add(
                            context.getString(R.string.notif_grades_msg_single, name, label, v2.toInt())
                        )
                    }
                }
            }
            
            if (newMarks != null) {
                check(context.getString(R.string.m1), oldMarks?.point1, newMarks.point1)
                check(context.getString(R.string.m2), oldMarks?.point2, newMarks.point2)
                check(context.getString(R.string.exam_short), oldMarks?.finalScore, newMarks.finalScore)
            }
            
            if (oldWrapper?.graphic == null && newWrapper.graphic != null) {
                // For portals, we also check if oldWrapper existed to avoid spam on first sync
                if (oldWrapper != null) {
                    portalUpdates.add(context.getString(R.string.notif_portal_opened, name))
                }
            }
        }
        
        return Pair(gradeUpdates, portalUpdates)
    }

    fun sendNotification(context: Context, updates: List<String>, isPortalOpening: Boolean) {
        val title = if (isPortalOpening) {
            context.getString(R.string.notif_portal_opened_title)
        } else {
            context.getString(R.string.notif_new_grades_title)
        }
        
        val message = if (updates.size > 4) {
            if (isPortalOpening) {
                context.getString(R.string.notif_portal_opened_multiple, updates.size)
            } else {
                context.getString(R.string.notif_grades_msg_multiple, updates.size)
            }
        } else {
            updates.joinToString("\n")
        }
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TITLE", title)
            putExtra("MESSAGE", message)
            putExtra("ID", if (isPortalOpening) 778 else 777)
        }
        context.sendBroadcast(intent)
    }
}
