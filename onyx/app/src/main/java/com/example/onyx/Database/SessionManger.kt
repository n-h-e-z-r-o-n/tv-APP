package com.example.onyx.Database

import android.content.Context


class SessionManger(context: Context) {
    private val pref = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

    fun saveUserId(userId: Int) {
        pref.edit().putInt("LOGGED_USER_ID", userId).apply()
    }

    fun getUserId(): Int {
        return pref.getInt("LOGGED_USER_ID", -1) // -1 means no user logged in
    }

    fun saveAvatar(avatar: String) {
        pref.edit().putString("LOGGED_AVATAR", avatar).apply()
    }

    fun getUserAvatar(): String? {
        return pref.getString("LOGGED_AVATAR", "-") // -1 means no user logged in
    }

    fun clearSession() {
        pref.edit().clear().apply()
    }

    /* ---------- DYNAMIC COLOR ---------- */
    fun isDynamicColorEnabled(): Boolean {
        return pref.getBoolean("DYNAMIC_COLOR", true) // Default is true
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        pref.edit().putBoolean("DYNAMIC_COLOR", enabled).apply()
    }

    /* ---------- CONTINUE WATCHING (SESSION) ---------- */

    // ✅ Save last playback position
    fun saveLastPosition(itemId: String, positionMs: Long) {
        pref.edit()
            .putLong("LAST_POS_$itemId", positionMs)
            .apply()
    }

    // ✅ Get last playback position
    fun getLastPosition(itemId: String): Long {
        return pref.getLong("LAST_POS_$itemId", 0L)
    }

    // ✅ Clear position (when episode/movie finishes)
    fun clearLastPosition(itemId: String) {
        pref.edit()
            .remove("LAST_POS_$itemId")
            .apply()
    }

}