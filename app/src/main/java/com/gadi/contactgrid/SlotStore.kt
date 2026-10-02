package com.gadi.contactgrid

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One contact placed in a widget square. */
data class Slot(
    val contactId: Long,
    val lookupKey: String?,
    val name: String,
    val number: String,
)

/** Persists the 9 slots of each widget instance (each widget has its own list). */
object SlotStore {
    const val SLOT_COUNT = 9
    private const val PREFS = "contact_grid"

    private fun key(widgetId: Int) = "w$widgetId"

    fun load(ctx: Context, widgetId: Int): Array<Slot?> =
        fromJson(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(widgetId), null))

    fun save(ctx: Context, widgetId: Int, slots: Array<Slot?>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(widgetId), toJson(slots))
            .apply()
    }

    fun delete(ctx: Context, widgetId: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(key(widgetId))
            .apply()
    }

    fun toJson(slots: Array<Slot?>): String {
        val arr = JSONArray()
        for (s in slots) {
            if (s == null) {
                arr.put(JSONObject.NULL)
            } else {
                arr.put(
                    JSONObject()
                        .put("id", s.contactId)
                        .put("lookup", s.lookupKey ?: "")
                        .put("name", s.name)
                        .put("number", s.number)
                )
            }
        }
        return arr.toString()
    }

    fun fromJson(raw: String?): Array<Slot?> {
        val out = arrayOfNulls<Slot>(SLOT_COUNT)
        if (raw.isNullOrEmpty()) return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until minOf(arr.length(), SLOT_COUNT)) {
                val o = arr.optJSONObject(i) ?: continue
                out[i] = Slot(
                    contactId = o.optLong("id", -1L),
                    lookupKey = o.optString("lookup").ifEmpty { null },
                    name = o.optString("name"),
                    number = o.optString("number"),
                )
            }
        } catch (e: JSONException) {
            // corrupted prefs -> start empty
        }
        return out
    }
}
