package com.example.onyx

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FavoritesManager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorites_json"

    fun addFavorite(context: Context, item: JSONObject) {
        val list = getFavorites(context).toMutableList()
        val id = item.optString("id")
        val type = item.optString("media_type").ifEmpty {
            if (item.has("first_air_date")) "tv" else "movie"
        }
        if (list.any { it.optString("id") == id && (
                it.optString("media_type").ifEmpty { if (it.has("first_air_date")) "tv" else "movie" } == type
            )
        }) return
        list.add(item)
        saveFavorites(context, list)
    }

    fun removeFavorite(context: Context, id: String, type: String) {
        val list = getFavorites(context).filterNot { o ->
            val oid = o.optString("id")
            val otype = o.optString("media_type").ifEmpty { if (o.has("first_air_date")) "tv" else "movie" }
            oid == id && otype == type
        }
        saveFavorites(context, list)

    }

    fun isFavorite(context: Context, id: String, type: String): Boolean {
        return getFavorites(context).any { o ->
            val oid = o.optString("id")
            val otype = o.optString("media_type").ifEmpty { if (o.has("first_air_date")) "tv" else "movie" }
            oid == id && otype == type
        }
    }

    fun getFavorites(context: Context): List<JSONObject> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        val arr = JSONArray(json)
        val result = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            result.add(o)
        }
        return result
    }

    private fun saveFavorites(context: Context, items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(item)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }
}



