package com.example.onyx.Database


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import java.util.concurrent.TimeUnit

class AppDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        // 1. User table
        // 1. Users Table
        db.execSQL(
            """CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE,
        gender TEXT,
        pin TEXT,
        avatar TEXT
    )"""
        )





// 3. Favorites Movie
        db.execSQL(
            """CREATE TABLE favorites_shows (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        show_id TEXT,
        type TEXT,
        title TEXT,
        rating TEXT,
        genres TEXT,
        overview TEXT,
        runtime TEXT,
        year TEXT,
        voteCount TEXT,
        pg TEXT,
        poster TEXT,
        backdrop TEXT,
        noOfSeason INTEGER,
        lastSeason INTEGER,
        lastEpisode INTEGER,

        UNIQUE(user_id, show_id, type),
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    )"""
        )








// 5. Favorites Anime
        db.execSQL(
            """
                CREATE TABLE favorites_anime (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    anime_id TEXT,
                    name TEXT,
                    type TEXT,
                    anilistId TEXT,
                    malId TEXT,
                    description TEXT,
                    rating TEXT,
                    quality TEXT,
                    duration TEXT,
                    poster TEXT,
                    sub TEXT,
                    dub TEXT,
                    aired TEXT,
                    genre TEXT,
                    seasons TEXT,
                    UNIQUE(user_id, anime_id),
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                )

            """.trimIndent()
        )


// 6. Downloads
        db.execSQL(
            """CREATE TABLE downloads (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        item_id TEXT,
        title TEXT,
        file_path TEXT,
        size INTEGER,
        progress INTEGER,
        status TEXT,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    )"""
        )


// 7. Continue Watching
        db.execSQL(
            """CREATE TABLE continue_watching (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    item_id TEXT,                 -- movieId, episodeId, animeEpisodeId etc
                    type TEXT,                    -- movie, tv_episode, anime_episode
                    title TEXT,
                    poster TEXT,
                    backdrop TEXT,        
                    season_number TEXT,  -- for tv/anime
                    episode_number TEXT, -- for tv/anime
                    last_position INTEGER DEFAULT 0,  -- last playback position in ms
                    duration INTEGER DEFAULT 0,       -- total duration in ms
                    updated_at INTEGER,               -- last update time
                    UNIQUE(user_id, item_id, type),
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                )"""
        )

// 8. anime notification
        db.execSQL(
            """
            CREATE TABLE anime_notification (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
        
                anime_id TEXT,
                title TEXT,
                poster TEXT,
                subStored TEXT,
                dubStored TEXT,
                
                seasonsStored TEXT,
        
                notify_at INTEGER,
        
                UNIQUE(user_id, anime_id, subStored, dubStored, seasonsStored),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """
        )


// 9. tv notification
        db.execSQL(
            """
            CREATE TABLE tv_notification (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
        
                tv_id TEXT,
                title TEXT,
                poster TEXT,
                noOfSeason INTEGER,
                lastSeason INTEGER,
                lastEpisode INTEGER,
        
                notify_at INTEGER,
        
                UNIQUE(user_id, tv_id, noOfSeason, lastSeason, lastEpisode),
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """
        )


// 10. App Settings (General App Info – NOT linked to users)
        db.execSQL(
            """CREATE TABLE app_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                is_paid INTEGER DEFAULT 0,               -- 0 = false, 1 = true
                subscription_type TEXT DEFAULT 'NONE',   -- e.g., "MONTHLY", "YEARLY", "NONE"
                subscription_start INTEGER DEFAULT 0,    -- Unix timestamp when subscription started
                subscription_expiry INTEGER DEFAULT 0,   -- Unix timestamp when subscription expires
                payment_reference TEXT DEFAULT '',       -- Payment ref if any
                last_checked INTEGER DEFAULT 0           -- Unix timestamp when last checked
            )"""
        )

        // Insert default row (only one row is needed for app settings)
        db.execSQL(
            """INSERT INTO app_settings (is_paid, subscription_type, subscription_start, subscription_expiry, payment_reference, last_checked)
                VALUES (0, 'NONE', 0, 0, '', 0);"""
        )

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // If you upgrade schema later
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS watchlist")
        db.execSQL("DROP TABLE IF EXISTS favorites_movies")
        db.execSQL("DROP TABLE IF EXISTS favorites_series")
        db.execSQL("DROP TABLE IF EXISTS favorites_anime")
        db.execSQL("DROP TABLE IF EXISTS downloads")
        db.execSQL("DROP TABLE IF EXISTS continue_watching")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "app_data.db"
        private const val DATABASE_VERSION = 1
    }
    // ========== USERS TABLE OPERATIONS ==========

    fun addUser(username: String, gender: String, pin: String, avatar: String = ""): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("gender", gender)
            put("pin", pin)
            put("avatar", avatar)
        }

        // Insert and get the row ID
        val userId = db.insert("users", null, values)

        // userId = the AUTO_INCREMENT primary key (id)
        return userId   // returns -1 if insert failed
    }

    fun getUsers(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM users", null)
    }

    fun getUsernameById(id: Int): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT username FROM users WHERE id=? LIMIT 1",
            arrayOf(id.toString())
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow("username"))
            } else {
                null
            }
        }
    }

    fun deleteUser(id: Int): Boolean {
        val db = writableDatabase
        return db.delete("users", "id=?", arrayOf(id.toString())) > 0
    }

    fun updateUserAvatar(id: Int, avatar: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put("avatar", avatar) }
        return db.update("users", cv, "id=?", arrayOf(id.toString())) > 0
    }

    fun validateUser(username: String, pin: String): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM users WHERE username=? AND pin=?",
            arrayOf(username, pin)
        )
    }

    //////////////////////////////// FAVORITES ANIME FUNCTIONS ///////////////////////////////////////

    fun addFavoriteAnime(
        userId: Int,
        animeId: String,
        name: String,
        type: String,
        anilistId: String,
        malId: String,
        description: String,
        rating: String,
        quality: String,
        duration: String,
        poster: String,
        sub: String,
        dub: String,
        aired: String,
        genre: String,
        seasons: String
    ): Boolean {
        val db = writableDatabase
        val cv = ContentValues()

        cv.put("user_id", userId)
        cv.put("anime_id", animeId)
        cv.put("name", name)
        cv.put("type", type)
        cv.put("anilistId", anilistId)
        cv.put("malId", malId)
        cv.put("description", description)
        cv.put("rating", rating)
        cv.put("quality", quality)
        cv.put("duration", duration)
        cv.put("poster", poster)
        cv.put("sub", sub)
        cv.put("dub", dub)
        cv.put("aired", aired)
        cv.put("genre", genre)
        cv.put("seasons", seasons)

        return try {
            db.insertOrThrow("favorites_anime", null, cv) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateAnimeProgress(
        userId: Int,
        animeId: String,
        sub: Int,
        dub: Int,
    ): Boolean {

        val db = writableDatabase
        val cv = ContentValues()

        cv.put("sub", sub)
        cv.put("dub", dub)

        return try {
            val rows = db.update(
                "favorites_anime",
                cv,
                "user_id = ? AND anime_id = ?",
                arrayOf(userId.toString(), animeId)
            )

            if (rows > 0) {
                Log.d("Database.feedback", "Anime Fave UPDATE  SUCCESS → userId=$userId animeId=$animeId sub=$sub dub=$dub")
                true
            } else {
                Log.w("Database.feedback", "Anime DELETE FAILED (not found) → userId=$userId animeId=$animeId sub=$sub dub=$dub")
                false
            }
        } catch (e: Exception) {
            Log.d("Database.feedback", "Anime Fave UPDATE ERROR  →  userId=$userId animeId=$animeId sub=$sub dub=$dub" )
            false
        }
    }

    fun removeFavoriteAnime(userId: Int, animeId: String): Boolean {
        val db = writableDatabase
        return db.delete(
            "favorites_anime", "user_id=? AND anime_id=?",
            arrayOf(userId.toString(), animeId)
        ) > 0
    }

    fun isFavoriteAnime(userId: Int, animeId: String): Boolean {
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT 1 FROM favorites_anime WHERE user_id=? AND anime_id=? LIMIT 1",
            arrayOf(userId.toString(), animeId)
        )

        val exists = cursor.moveToFirst()
        cursor.close()

        return exists
    }

    fun getFavoriteAnime(userId: Int): ArrayList<HashMap<String, String>> {
        val db = readableDatabase
        val list = ArrayList<HashMap<String, String>>()

        val cursor = db.rawQuery(
            "SELECT * FROM favorites_anime WHERE user_id=?",
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {
            do {

                val map = HashMap<String, String>()

                map["user_id"] = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")).toString()
                map["anime_id"] = cursor.getString(cursor.getColumnIndexOrThrow("anime_id"))
                map["name"] = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                map["type"] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                map["anilistId"] = cursor.getString(cursor.getColumnIndexOrThrow("anilistId"))
                map["malId"] = cursor.getString(cursor.getColumnIndexOrThrow("malId"))
                map["description"] = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                map["rating"] = cursor.getString(cursor.getColumnIndexOrThrow("rating"))
                map["quality"] = cursor.getString(cursor.getColumnIndexOrThrow("quality"))
                map["duration"] = cursor.getString(cursor.getColumnIndexOrThrow("duration"))
                map["poster"] = cursor.getString(cursor.getColumnIndexOrThrow("poster"))
                map["sub"] = cursor.getString(cursor.getColumnIndexOrThrow("sub"))
                map["dub"] = cursor.getString(cursor.getColumnIndexOrThrow("dub"))
                map["aired"] = cursor.getString(cursor.getColumnIndexOrThrow("aired"))
                map["genre"] = cursor.getString(cursor.getColumnIndexOrThrow("genre"))

                // Convert "seasons" from comma text → array string
                val seasonsString = cursor.getString(cursor.getColumnIndexOrThrow("seasons"))
                map["seasons"] = seasonsString   // keep original string

                list.add(map)

            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }

    fun getFavoriteAnimeCount(userId: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) AS count FROM favorites_anime WHERE user_id=?",
            arrayOf(userId.toString())
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getInt(it.getColumnIndexOrThrow("count"))
            } else {
                0
            }
        }
    }


    //////////////////////////////// FAVORITES SHOWS FUNCTIONS ///////////////////////////////////////

    fun addFavoriteShow(
        userId: Int,
        showId: String,
        type: String,
        title: String,
        rating: String,
        genres: String,
        overview: String,
        runtime: String,
        year: String,
        voteCount: String,
        pg: String,
        poster: String,
        backdrop: String,
        noOfSeason: Int,
        lastSeason: Int,
        lastEpisode: Int
    ): Boolean {

        val db = writableDatabase
        val cv = ContentValues()

        cv.put("user_id", userId)
        cv.put("show_id", showId)
        cv.put("type", type)
        cv.put("title", title)
        cv.put("rating", rating)
        cv.put("genres", genres)
        cv.put("overview", overview)
        cv.put("runtime", runtime)
        cv.put("year", year)
        cv.put("voteCount", voteCount)
        cv.put("pg", pg)
        cv.put("poster", poster)
        cv.put("backdrop", backdrop)
        cv.put("noOfSeason", noOfSeason)
        cv.put("lastSeason", lastSeason)
        cv.put("lastEpisode", lastEpisode)

        return try {
            db.insertOrThrow("favorites_shows", null, cv) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateFavoriteShowProgress(
        userId: Int,
        showId: String,
        noOfSeason: Int,
        lastSeason: Int,
        lastEpisode: Int,
    ): Boolean {

        val db = writableDatabase
        val cv = ContentValues()

        cv.put("noOfSeason", noOfSeason)
        cv.put("lastSeason", lastSeason)
        cv.put("lastEpisode", lastEpisode)

        return try {
            val rows = db.update(
                "favorites_shows",
                cv,
                "user_id = ? AND show_id = ?",
                arrayOf(userId.toString(), showId)
            )

            if (rows > 0) {
                Log.d("Database.feedback", "TvShow Fave UPDATE  SUCCESS → userId=$userId animeId=$showId noOfSeason=$noOfSeason lastSeason=$lastSeason  lastEpisode=$lastEpisode" )
                true
            } else {
                Log.w("Database.feedback", "TvShow DELETE FAILED (not found) → userId=$userId animeId=$showId noOfSeason=$noOfSeason lastSeason=$lastSeason lastEpisode=$lastEpisode")
                false
            }
        } catch (e: Exception) {
            Log.d("Database.feedback", "TvShow Fave UPDATE ERROR  →  userId=$userId animeId=$showId noOfSeason=$noOfSeason lastSeason=$lastSeason lastEpisode=$lastEpisode" )
            false
        }
    }

    fun removeFavoriteShow(userId: Int, showId: String, type: String): Boolean {
        return try {
            val db = writableDatabase
            db.delete(
                "favorites_shows",
                "user_id=? AND show_id=? AND type=?",
                arrayOf(userId.toString(), showId, type)
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isFavoriteShow(userId: Int, showId: String, type: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM favorites_shows WHERE user_id=? AND show_id=? AND type=? LIMIT 1",
            arrayOf(userId.toString(), showId, type)
        )

        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getFavoriteShows(userId: Int): ArrayList<HashMap<String, String>> {
        val list = ArrayList<HashMap<String, String>>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM favorites_shows WHERE user_id=? ORDER BY id DESC",
            arrayOf(userId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"] = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString()
                map["show_id"] = cursor.getString(cursor.getColumnIndexOrThrow("show_id"))
                map["type"] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                map["title"] = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                map["rating"] = cursor.getString(cursor.getColumnIndexOrThrow("rating"))
                map["genres"] = cursor.getString(cursor.getColumnIndexOrThrow("genres"))
                map["overview"] = cursor.getString(cursor.getColumnIndexOrThrow("overview"))
                map["runtime"] = cursor.getString(cursor.getColumnIndexOrThrow("runtime"))
                map["year"] = cursor.getString(cursor.getColumnIndexOrThrow("year"))
                map["voteCount"] = cursor.getString(cursor.getColumnIndexOrThrow("voteCount"))
                map["pg"] = cursor.getString(cursor.getColumnIndexOrThrow("pg"))
                map["poster"] = cursor.getString(cursor.getColumnIndexOrThrow("poster"))
                map["backdrop"] = cursor.getString(cursor.getColumnIndexOrThrow("backdrop"))
                map["noOfSeason"] = cursor.getString(cursor.getColumnIndexOrThrow("noOfSeason"))
                map["lastSeason"] = cursor.getString(cursor.getColumnIndexOrThrow("lastSeason"))
                map["lastEpisode"] = cursor.getString(cursor.getColumnIndexOrThrow("lastEpisode"))

                list.add(map)

            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }

    fun getFavoriteShowsByType(userId: Int, type: String): ArrayList<HashMap<String, String>> {
        val list = ArrayList<HashMap<String, String>>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM favorites_shows WHERE user_id=? AND type=? ORDER BY id DESC",
            arrayOf(userId.toString(), type)
        )

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"] = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString()
                map["show_id"] = cursor.getString(cursor.getColumnIndexOrThrow("show_id"))
                map["type"] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                map["title"] = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                map["rating"] = cursor.getString(cursor.getColumnIndexOrThrow("rating"))
                map["genres"] = cursor.getString(cursor.getColumnIndexOrThrow("genres"))
                map["overview"] = cursor.getString(cursor.getColumnIndexOrThrow("overview"))
                map["runtime"] = cursor.getString(cursor.getColumnIndexOrThrow("runtime"))
                map["year"] = cursor.getString(cursor.getColumnIndexOrThrow("year"))
                map["voteCount"] = cursor.getString(cursor.getColumnIndexOrThrow("voteCount"))
                map["pg"] = cursor.getString(cursor.getColumnIndexOrThrow("pg"))
                map["poster"] = cursor.getString(cursor.getColumnIndexOrThrow("poster"))
                map["backdrop"] = cursor.getString(cursor.getColumnIndexOrThrow("backdrop"))
                map["noOfSeason"] = cursor.getString(cursor.getColumnIndexOrThrow("noOfSeason"))
                map["lastSeason"] = cursor.getString(cursor.getColumnIndexOrThrow("lastSeason"))
                map["lastEpisode"] = cursor.getString(cursor.getColumnIndexOrThrow("lastEpisode"))

                list.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }


    fun clearFavoriteShows(userId: Int) {
        val db = writableDatabase
        db.delete("favorites_shows", "user_id=?", arrayOf(userId.toString()))
    }




    ////////////////////////////////////// WATCHLIST FUNCTIONS //////////////////////////////////////

    fun addOrUpdateContinueWatching(
        userId: Int,
        itemId: String,
        type: String,
        title: String,
        poster: String,
        backdrop: String,
        seasonNumber: String,
        episodeNumber: String,
        lastPosition: Int,
        duration: Int
    ) {
        val db = writableDatabase
        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("user_id", userId)
            put("item_id", itemId)
            put("type", type)
            put("title", title)
            put("poster", poster)
            put("backdrop", backdrop)
            put("season_number", seasonNumber)
            put("episode_number", episodeNumber)
            put("last_position", lastPosition)
            put("duration", duration)
            put("updated_at", now)
        }

        db.insertWithOnConflict(
            "continue_watching",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
    //Get all continue watching items sorted by updated time
    fun getContinueWatchingAll(userId: Int, type: String): ArrayList<HashMap<String, String>> {
        val db = readableDatabase
        val list = ArrayList<HashMap<String, String>>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM continue_watching
        WHERE user_id = ? AND type = ?
        ORDER BY updated_at DESC
        """,
            arrayOf(userId.toString(),  type)
        )

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["item_id"] = cursor.getString(cursor.getColumnIndexOrThrow("item_id"))
                map["type"] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                map["title"] = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                map["poster"] = cursor.getString(cursor.getColumnIndexOrThrow("poster"))
                map["backdrop"] = cursor.getString(cursor.getColumnIndexOrThrow("backdrop"))
                map["season_number"] = cursor.getString(cursor.getColumnIndexOrThrow("season_number"))
                map["episode_number"] = cursor.getString(cursor.getColumnIndexOrThrow("episode_number"))
                map["last_position"] = cursor.getInt(cursor.getColumnIndexOrThrow("last_position")).toString()
                map["duration"] = cursor.getInt(cursor.getColumnIndexOrThrow("duration")).toString()
                map["updated_at"] = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")).toString()

                list.add(map)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }

    fun getContinueWatchingItem(userId: Int, itemId: String, type: String): HashMap<String, String>? {
        val db = readableDatabase

        val cursor = db.rawQuery(
            """
        SELECT * FROM continue_watching
        WHERE user_id = ? AND item_id = ? AND type = ?
        """,
            arrayOf(userId.toString(), itemId, type)
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        val map = HashMap<String, String>()
        map["item_id"] = cursor.getString(cursor.getColumnIndexOrThrow("item_id"))
        map["type"] = cursor.getString(cursor.getColumnIndexOrThrow("type"))
        map["title"] = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        map["poster"] = cursor.getString(cursor.getColumnIndexOrThrow("poster"))
        map["backdrop"] = cursor.getString(cursor.getColumnIndexOrThrow("backdrop"))
        map["season_number"] = cursor.getString(cursor.getColumnIndexOrThrow("season_number"))
        map["episode_number"] = cursor.getString(cursor.getColumnIndexOrThrow("episode_number"))
        map["last_position"] = cursor.getInt(cursor.getColumnIndexOrThrow("last_position")).toString()
        map["duration"] = cursor.getInt(cursor.getColumnIndexOrThrow("duration")).toString()
        map["updated_at"] = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")).toString()

        cursor.close()
        return map
    }

    fun removeContinueWatching(userId: Int, itemId: String, type: String) {
        val db = writableDatabase
        db.delete(
            "continue_watching",
            "user_id=? AND item_id=? AND type=?",
            arrayOf(userId.toString(), itemId, type)
        )
    }

    fun getResumePosition(
        userId: Int,
        itemId: String,
        type: String
    ): Int {
        val db = readableDatabase
        var lastPosition = 0

        val cursor = db.rawQuery(
            """
        SELECT last_position 
        FROM continue_watching
        WHERE user_id = ? AND item_id = ? AND type = ?
        LIMIT 1
        """,
            arrayOf(userId.toString(), itemId, type)
        )

        if (cursor.moveToFirst()) {
            lastPosition = cursor.getInt(
                cursor.getColumnIndexOrThrow("last_position")
            )
        }

        cursor.close()
        return lastPosition
    }

    fun getDurationPosition(
        userId: Int,
        itemId: String,
        type: String
    ): Int {
        val db = readableDatabase
        var durationPosition = 0

        val cursor = db.rawQuery(
            """
        SELECT duration 
        FROM continue_watching
        WHERE user_id = ? AND item_id = ? AND type = ?
        LIMIT 1
        """,
            arrayOf(userId.toString(), itemId, type)
        )

        if (cursor.moveToFirst()) {
            durationPosition = cursor.getInt(
                cursor.getColumnIndexOrThrow("duration")
            )
        }

        cursor.close()
        return durationPosition
    }

    ////////////////////////////////// NOTIFICATIONS FUNCTIONS ///////////////////////////////////////

    fun insertAnimeNotification(
        userId: Int,
        animeId: String,
        title: String,
        poster: String,
        subStored: Int,
        dubStored: Int,
        seasonsStored: Int
    ): Boolean {

        val db = writableDatabase
        val cv = ContentValues().apply {
            put("user_id", userId)
            put("anime_id", animeId)
            put("title", title)
            put("poster", poster)
            put("subStored", subStored.toString())
            put("dubStored", dubStored.toString())
            put("seasonsStored", seasonsStored.toString())
            put("notify_at", System.currentTimeMillis())
        }

        return try {
            db.insertOrThrow("anime_notification", null, cv)
            true
        } catch (e: SQLiteConstraintException) {
            // ✅ Duplicate detected → ignore
            false
        } catch (e: Exception) {
            //Log.e("Database.feedback", "Insert failed: ${e.message}")
            false
        }
    }

    fun getAnimeNotificationHistory(
        userId: Int,
        animeId: String
    ): List<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM anime_notification
        WHERE user_id = ? AND anime_id = ?
        ORDER BY notify_at DESC
        """,
            arrayOf(userId.toString(), animeId)
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    mapOf(
                        "id" to it.getInt(it.getColumnIndexOrThrow("id")).toString(),
                        "anime_id" to it.getString(it.getColumnIndexOrThrow("anime_id")),
                        "title" to it.getString(it.getColumnIndexOrThrow("title")),
                        "poster" to it.getString(it.getColumnIndexOrThrow("poster")),
                        "subStored" to it.getString(it.getColumnIndexOrThrow("subStored")),
                        "dubStored" to it.getString(it.getColumnIndexOrThrow("dubStored")),
                        "seasonsStored" to it.getString(it.getColumnIndexOrThrow("seasonsStored")),
                        "notify_at" to it.getLong(it.getColumnIndexOrThrow("notify_at")).toString()
                    )
                )
            }
        }
        return list
    }

    // Get ALL notifications for a user
    fun getAllAnimeNotifications(userId: Int): List<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM anime_notification
        WHERE user_id = ?
        ORDER BY notify_at DESC
        """,
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    mapOf(
                        "id" to it.getString(it.getColumnIndexOrThrow("id")),
                        "anime_id" to it.getString(it.getColumnIndexOrThrow("anime_id")),
                        "title" to it.getString(it.getColumnIndexOrThrow("title")),
                        "poster" to it.getString(it.getColumnIndexOrThrow("poster")),
                        "subStored" to it.getString(it.getColumnIndexOrThrow("subStored")),
                        "dubStored" to it.getString(it.getColumnIndexOrThrow("dubStored")),
                        "seasonsStored" to it.getString(it.getColumnIndexOrThrow("seasonsStored")),
                        "notify_at" to it.getLong(it.getColumnIndexOrThrow("notify_at")).toString()
                    )
                )
            }
        }
        return list
    }

    //Delete notifications for ONE anime
    fun deleteAnimeNotifications(
        userId: Int,
        animeId: String
    ): Boolean {

        val db = writableDatabase
        return try {
            db.delete(
                "anime_notification",
                "user_id = ? AND anime_id = ?",
                arrayOf(userId.toString(), animeId)
            ) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun deleteAnimeNotificationById(
        userId: Int,
        animeId: String,
        notificationId: String
    ): Boolean {

        val db = writableDatabase

        return try {
            val rowsDeleted = db.delete(
                "anime_notification",
                "user_id = ? AND anime_id = ? AND id = ?",
                arrayOf(
                    userId.toString(),
                    animeId,
                    notificationId
                )
            )

            if (rowsDeleted > 0) {
                Log.d("Database.feedback", "Anime notification DELETE SUCCESS → animeId=$animeId id=$notificationId")
                true
            } else {
                Log.w("Database.feedback", "Anime notification DELETE FAILED (not found) → animeId=$animeId id=$notificationId")
                false
            }

        } catch (e: Exception) {
            Log.e("Database.feedback", "Anime notification DELETE ERROR: ${e.message}", e)
            false
        }
    }


    fun clearAllAnimeNotifications(userId: Int): Boolean {
        val db = writableDatabase
        return try {
            val rowsDeleted = db.delete(
                "anime_notification",
                "user_id = ?",
                arrayOf(userId.toString())
            )
            if (rowsDeleted > 0) {
                Log.d("Database.feedback", "Anime notification CLEAR SUCCESS → userId=$userId id=$rowsDeleted")
                true
            } else {
                Log.w("Database.feedback", "Anime notification CLEAR FAILED (not found) → userId=$userId id=$rowsDeleted")
                false
            }
        } catch (e: Exception) {
            Log.e("Database.feedback", "clearAllAnimeNotifications failed", e)
            false
        }
    }


    fun insertTvNotification(
        userId: Int,
        tvId: String,
        title: String,
        poster: String,
        noOfSeason: Int,
        lastSeason: Int,
        lastEpisode: Int
    ): Boolean {

        val db = writableDatabase
        val cv = ContentValues().apply {
            put("user_id", userId)
            put("tv_id", tvId)
            put("title", title)
            put("poster", poster)
            put("noOfSeason", noOfSeason)
            put("lastSeason", lastSeason)
            put("lastEpisode", lastEpisode)
            put("notify_at", System.currentTimeMillis())
        }

        return try {
            db.insertOrThrow("tv_notification", null, cv)
            true
        } catch (e: SQLiteConstraintException) {
            // ✅ Duplicate (same season/episode)
            false
        } catch (e: Exception) {
            false
        }
    }


    fun getTvNotificationHistory(
        userId: Int,
        tvId: String
    ): List<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM tv_notification
        WHERE user_id = ? AND tv_id = ?
        ORDER BY notify_at DESC
        """,
            arrayOf(userId.toString(), tvId)
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    mapOf(
                        "id" to it.getInt(it.getColumnIndexOrThrow("id")).toString(),
                        "tv_id" to it.getString(it.getColumnIndexOrThrow("tv_id")),
                        "title" to it.getString(it.getColumnIndexOrThrow("title")),
                        "poster" to it.getString(it.getColumnIndexOrThrow("poster")),
                        "noOfSeason" to it.getInt(it.getColumnIndexOrThrow("noOfSeason")).toString(),
                        "lastSeason" to it.getInt(it.getColumnIndexOrThrow("lastSeason")).toString(),
                        "lastEpisode" to it.getInt(it.getColumnIndexOrThrow("lastEpisode")).toString(),
                        "notify_at" to it.getLong(it.getColumnIndexOrThrow("notify_at")).toString()
                    )
                )
            }
        }
        return list
    }

    fun getAllTvNotifications(userId: Int): List<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery(
            """
        SELECT * FROM tv_notification
        WHERE user_id = ?
        ORDER BY notify_at DESC
        """,
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    mapOf(
                        "id" to it.getString(it.getColumnIndexOrThrow("id")),
                        "tv_id" to it.getString(it.getColumnIndexOrThrow("tv_id")),
                        "title" to it.getString(it.getColumnIndexOrThrow("title")),
                        "poster" to it.getString(it.getColumnIndexOrThrow("poster")),
                        "noOfSeason" to it.getInt(it.getColumnIndexOrThrow("noOfSeason")).toString(),
                        "lastSeason" to it.getInt(it.getColumnIndexOrThrow("lastSeason")).toString(),
                        "lastEpisode" to it.getInt(it.getColumnIndexOrThrow("lastEpisode")).toString(),
                        "notify_at" to it.getLong(it.getColumnIndexOrThrow("notify_at")).toString()
                    )
                )
            }
        }
        return list
    }

    fun deleteTvNotifications(
        userId: Int,
        tvId: String
    ): Boolean {

        val db = writableDatabase
        return try {
            db.delete(
                "tv_notification",
                "user_id = ? AND tv_id = ?",
                arrayOf(userId.toString(), tvId)
            ) > 0
        } catch (e: Exception) {
            false
        }
    }


    fun deleteTvNotificationById(
        userId: Int,
        tvId: String,
        notificationId: String
    ): Boolean {

        val db = writableDatabase

        return try {
            val rowsDeleted = db.delete(
                "tv_notification",
                "user_id = ? AND tv_id = ? AND id = ?",
                arrayOf(userId.toString(), tvId, notificationId)
            )

            if (rowsDeleted > 0) {
                Log.d("Database.feedback", "TV notification DELETE SUCCESS → tvId=$tvId id=$notificationId")
                true
            } else {
                Log.w("Database.feedback", "TV notification DELETE FAILED → tvId=$tvId id=$notificationId")
                false
            }

        } catch (e: Exception) {
            Log.e("Database.feedback", "TV notification DELETE ERROR", e)
            false
        }
    }

    fun clearAllTvNotifications(userId: Int): Boolean {

        val db = writableDatabase
        return try {
            val rowsDeleted = db.delete(
                "tv_notification",
                "user_id = ?",
                arrayOf(userId.toString())
            )

            if (rowsDeleted > 0) {
                Log.d("Database.feedback", "TV notification CLEAR SUCCESS → userId=$userId rows=$rowsDeleted")
                true
            } else {
                Log.w("Database.feedback", "TV notification CLEAR FAILED → userId=$userId")
                false
            }

        } catch (e: Exception) {
            Log.e("Database.feedback", "clearAllTvNotifications failed", e)
            false
        }
    }










    ////////////////////////////////// APP SETTING FUNCTIONS ///////////////////////////////////////

    fun setSubscription(type: String, paymentRef: String = "") {
        val db = writableDatabase

        // Current time
        val now = System.currentTimeMillis()

        // Calculate expiry based on type
        val expiry = when(type.uppercase()) {
            "MONTHLY" -> now + TimeUnit.DAYS.toMillis(32)
            "3MONTH" -> now + TimeUnit.DAYS.toMillis(92)
            "YEARLY" -> now + TimeUnit.DAYS.toMillis(367)
            else -> 0L
        }

        val values = ContentValues().apply {
            put("is_paid", if (expiry > 0) 1 else 0)
            put("subscription_type", type.uppercase())
            put("subscription_start", now)
            put("subscription_expiry", expiry)
            put("payment_reference", paymentRef)
            put("last_checked", now)
        }

        db.update("app_settings", values, "id = ?", arrayOf("1"))
    }

    fun isSubscriptionActive(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT subscription_expiry FROM app_settings WHERE id = 1", null)
        var active = false

        if (cursor.moveToFirst()) {
            val expiry = cursor.getLong(cursor.getColumnIndexOrThrow("subscription_expiry"))
            active = expiry > System.currentTimeMillis()
        }
        cursor.close()
        return active
    }

    fun getSubscriptionDaysLeft(): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT subscription_expiry FROM app_settings WHERE id = 1", null)
        var daysLeft = 0L

        if (cursor.moveToFirst()) {
            val expiry = cursor.getLong(cursor.getColumnIndexOrThrow("subscription_expiry"))
            val now = System.currentTimeMillis()
            if (expiry > now) {
                daysLeft = TimeUnit.MILLISECONDS.toDays(expiry - now)
            } else {
                daysLeft = 0
            }
        }

        cursor.close()
        return daysLeft
    }

    fun getSubscriptionType(): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT subscription_type FROM app_settings WHERE id = 1", null)
        var subscriptionType = "NONE"

        if (cursor.moveToFirst()) {
            subscriptionType = cursor.getString(cursor.getColumnIndexOrThrow("subscription_type")) ?: "NONE"
        }

        cursor.close()
        return subscriptionType
    }

    fun resetExpiredSubscription() {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val cursor = db.rawQuery("SELECT subscription_expiry FROM app_settings WHERE id = 1", null)

        if (cursor.moveToFirst()) {
            val expiry = cursor.getLong(cursor.getColumnIndexOrThrow("subscription_expiry"))
            if (expiry <= now) {
                val values = ContentValues().apply {
                    put("is_paid", 0)
                    put("subscription_type", "NONE")
                    put("subscription_start", 0)
                    put("subscription_expiry", 0)
                    put("payment_reference", "")
                    put("last_checked", now)
                }
                db.update("app_settings", values, "id = ?", arrayOf("1"))
            }
        }
        cursor.close()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun resetDatabase() {
        val db = writableDatabase

        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS watchlist")
        db.execSQL("DROP TABLE IF EXISTS favorites_movies")
        db.execSQL("DROP TABLE IF EXISTS favorites_series")
        db.execSQL("DROP TABLE IF EXISTS favorites_anime")
        db.execSQL("DROP TABLE IF EXISTS downloads")
        db.execSQL("DROP TABLE IF EXISTS continue_watching")
        db.execSQL("DROP TABLE IF EXISTS app_settings")

        // Recreate database schema
        onCreate(db)
    }



}
