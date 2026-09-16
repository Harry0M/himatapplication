package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Strictly local draft storage for in-progress Master records.
 * Uses Android SharedPreferences private storage.
 * Draft data is NEVER uploaded to cloud / Firebase until explicitly saved.
 */
object MasterDraftManager {
    private const val PREFS_NAME = "himat_master_drafts"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDraft(context: Context, masterType: String, data: Map<String, Any?>) {
        try {
            val json = JSONObject()
            data.forEach { (key, value) ->
                json.put(key, value ?: JSONObject.NULL)
            }
            getPrefs(context).edit().putString("draft_$masterType", json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getDraft(context: Context, masterType: String): Map<String, Any?>? {
        val str = getPrefs(context).getString("draft_$masterType", null) ?: return null
        return try {
            val json = JSONObject(str)
            val map = mutableMapOf<String, Any?>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = if (json.isNull(k)) null else json.get(k)
            }
            map
        } catch (_: Exception) {
            null
        }
    }

    fun hasDraft(context: Context, masterType: String): Boolean {
        return getPrefs(context).contains("draft_$masterType")
    }

    fun clearDraft(context: Context, masterType: String) {
        getPrefs(context).edit().remove("draft_$masterType").apply()
    }

    fun clearAllDrafts(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
