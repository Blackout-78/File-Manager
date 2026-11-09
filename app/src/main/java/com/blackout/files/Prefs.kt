package com.blackout.files

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TrashItem(val trashName: String, val original: String, val deletedAt: Long)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("files_prefs", Context.MODE_PRIVATE)

    var accentHue: Float
        get() = sp.getFloat("hue", 255f)
        set(v) { sp.edit().putFloat("hue", v).apply() }

    var showHidden: Boolean
        get() = sp.getBoolean("hidden", false)
        set(v) { sp.edit().putBoolean("hidden", v).apply() }

    var trashDays: Int
        get() = sp.getInt("trash_days", 30)
        set(v) { sp.edit().putInt("trash_days", v).apply() }

    var iconBack: Int
        get() = sp.getInt("icon_back", IconArt.DEF_BACK)
        set(v) { sp.edit().putInt("icon_back", v).apply() }

    var iconFront: Int
        get() = sp.getInt("icon_front", IconArt.DEF_FRONT)
        set(v) { sp.edit().putInt("icon_front", v).apply() }

    var iconKey: Int
        get() = sp.getInt("icon_key", IconArt.DEF_KEY)
        set(v) { sp.edit().putInt("icon_key", v).apply() }

    fun typeColor(t: FileType): Int = sp.getInt("c_" + t.name, t.defaultColor)

    fun setTypeColor(t: FileType, c: Int) {
        sp.edit().putInt("c_" + t.name, c).apply()
    }

    fun resetTypeColors() {
        val e = sp.edit()
        FileType.values().forEach { e.remove("c_" + it.name) }
        e.apply()
    }

    fun favorites(): Set<String> = (sp.getStringSet("fav", null) ?: emptySet()).toSet()

    fun setFavorites(s: Set<String>) {
        sp.edit().putStringSet("fav", HashSet(s)).apply()
    }

    fun trash(): List<TrashItem> {
        val a = JSONArray(sp.getString("trash", "[]") ?: "[]")
        val out = ArrayList<TrashItem>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(TrashItem(o.getString("t"), o.getString("o"), o.getLong("d")))
        }
        return out
    }

    fun setTrash(list: List<TrashItem>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("t", it.trashName).put("o", it.original).put("d", it.deletedAt))
        }
        sp.edit().putString("trash", a.toString()).apply()
    }
}
