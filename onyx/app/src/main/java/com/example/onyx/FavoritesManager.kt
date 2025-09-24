package com.example.onyx

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FavoritesManager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorites_json"

    data class FavoriteItem(
        val id: String,
        val title: String,
        val imageUrl: String,
        val type: String
    )

    fun addFavorite(context: Context, item: FavoriteItem) {
        val list = getFavorites(context).toMutableList()
        if (list.any { it.id == item.id && it.type == item.type }) return
        list.add(item)
        saveFavorites(context, list)
    }

    fun removeFavorite(context: Context, id: String, type: String) {
        val list = getFavorites(context).filterNot { it.id == id && it.type == type }
        saveFavorites(context, list)
    }

    fun isFavorite(context: Context, id: String, type: String): Boolean {
        return getFavorites(context).any { it.id == id && it.type == type }
    }

    fun getFavorites(context: Context): List<FavoriteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        val arr = JSONArray(json)
        val result = mutableListOf<FavoriteItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                FavoriteItem(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    imageUrl = o.optString("imageUrl"),
                    type = o.optString("type")
                )
            )
        }
        return result
    }

    private fun saveFavorites(context: Context, items: List<FavoriteItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("title", item.title)
            o.put("imageUrl", item.imageUrl)
            o.put("type", item.type)
            arr.put(o)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }
}



