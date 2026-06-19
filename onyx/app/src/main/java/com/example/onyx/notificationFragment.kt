package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.NotificationAdapter
import com.example.onyx.OnyxClasses.NotificationItem
import com.example.onyx.OnyxClasses.cWatchingAdapter
import com.example.onyx.databinding.FragmentNotificationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class notificationFragment :  Fragment(R.layout.fragment_notification) {


    private lateinit var notificationRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter

    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger

    private var userId: Int = -1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        userId = sm.getUserId()


        notificationRecyclerView = requireView().findViewById<RecyclerView>(R.id.notificationRecycler)
        notificationRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
        val clearBtn = requireView().findViewById<TextView>(R.id.clearNotBtn)

        clearBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.clearAllAnimeNotifications(userId)
            }
            notificationAdapter.clearItems()
        }

        loadNotifications()
    }





    private fun animeNotificationS() {
        lifecycleScope.launch {

            // DB call on IO
            val notificationsFromDb = withContext(Dispatchers.IO) {
                db.getAllAnimeNotifications(userId)
            }

            Log.e("anime_notifications", " userId: $userId , notificationsFromDb: $notificationsFromDb  ")

            // Headline
            requireView().findViewById<TextView>(R.id.notificationHeadline).text =  "notifications"

            //if (notificationsFromDb.size > 0) requireActivity().findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.VISIBLE

            val notifications = notificationsFromDb.map { item ->
                //Log.e("anime_Not_fetched", "xyz:" + item["subStored"]?.let { it::class } + item["seasons"] )

                val subcount = (item["subStored"]?.toString()?.toIntOrNull()) ?: 0
                val dubcount = (item["dubStored"]?.toString()?.toIntOrNull()) ?: 0

                Log.e("anime_Not_fetched", "xyz:" + "subcount: " +subcount + " | " + item["subStored"] + " dubcount: " + dubcount+ " | " + item["dubStored"] )

                var info = ""

                if(subcount> 0){
                    info = info + "Episode $subcount [SUB] available NOW \n\n"
                }
                if(dubcount> 0){
                    info = info + "Episode $dubcount [DUB] available NOW"
                }

                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["anime_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"],
                    info =  info, //"sub: ${item["subStored"]} dub: ${item["dubStored"]}",
                    type = "anime",
                    newSeason = item["subStored"]?.toString().orEmpty(),
                    newEpisode = item["dubStored"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // Adapter setup / update
            if (!::notificationAdapter.isInitialized) {
                notificationAdapter = NotificationAdapter(
                    items = notifications.toMutableList(),
                    layoutResId = R.layout.item_notification,
                    db,
                    userId
                )
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(notifications)
            }
        }
    }

    private fun showNotificationS() {
        lifecycleScope.launch {
            // Fetch from DB on IO thread
            val dbNotifications = withContext(Dispatchers.IO) {
                db.getAllTvNotifications(userId)
            }

            if (dbNotifications.size > 0) requireActivity().findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.VISIBLE

            // Update UI on Main thread
            val notificationItems = dbNotifications.map { item ->

                Log.d(
                    "Not_tv",
                    """
                notificationId: ${item["id"]}
                anime_id: ${item["anime_id"]}
                title: ${item["title"]}
                poster: ${item["poster"]}
                noOfSeason: ${item["noOfSeason"]}
                lastSeason: ${item["lastSeason"]}
                lastEpisode: ${item["lastEpisode"]}
                notify_at: ${item["notify_at"]}
                """.trimIndent()
                )

                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["tv_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"],
                    info = "Season ${item["lastSeason"]} - Episode ${item["lastEpisode"]}",
                    type = "tv",
                    newSeason = item["lastSeason"]?.toString().orEmpty(),
                    newEpisode = item["lastEpisode"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // UI updates
            requireView().findViewById<TextView>(R.id.notificationHeadline).text =
                "notifications (${notificationItems.size})"

            if (!::notificationAdapter.isInitialized) {
                notificationAdapter = NotificationAdapter(
                    items = notificationItems.toMutableList(),
                    layoutResId = R.layout.item_notification,
                    db,
                    userId
                )
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(notificationItems)
            }
        }
    }


    private fun loadNotifications() {
        lifecycleScope.launch {
            // 1. Fetch concurrently using async instead of withContext
            val animeNotificationsDeferred = async(Dispatchers.IO) {
                db.getAllAnimeNotifications(userId)
            }
            val tvNotificationsDeferred = async(Dispatchers.IO) {
                db.getAllTvNotifications(userId)
            }

            // 2. Await the results of the Deferred objects
            val notificationsFromDb = animeNotificationsDeferred.await()
            val dbTvNotifications = tvNotificationsDeferred.await()

            // Map anime notifications
            val animeItems = notificationsFromDb.map { item ->
                val subcount = item["subStored"]?.toString()?.toIntOrNull() ?: 0
                val dubcount = item["dubStored"]?.toString()?.toIntOrNull() ?: 0

                // 3. Cleaner string building to prevent trailing newlines
                val infoText = buildString {
                    if (subcount > 0) append("Episode $subcount [SUB] available NOW\n\n")
                    if (dubcount > 0) append("Episode $dubcount [DUB] available NOW")
                }.trimEnd()

                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["anime_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"]?.toString(),
                    info = infoText,
                    type = "anime",
                    newSeason = item["subStored"]?.toString().orEmpty(),
                    newEpisode = item["dubStored"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // Map TV notifications
            val tvItems = dbTvNotifications.map { item ->
                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["tv_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"]?.toString(),
                    info = "Season ${item["lastSeason"]} - Episode ${item["lastEpisode"]}",
                    type = "tv",
                    newSeason = item["lastSeason"]?.toString().orEmpty(),
                    newEpisode = item["lastEpisode"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // 4. More idiomatic list concatenation
            val allNotifications = (animeItems + tvItems).toMutableList()

            // Uncomment if you decide to sort them later
            // allNotifications.sortByDescending { it.time }

            // UI Updates
            if (allNotifications.isNotEmpty()) {
                requireActivity().findViewById<CardView>(R.id.cNotificationAnimeIcon)?.visibility = View.VISIBLE
            }

            // Headline
            requireView().findViewById<TextView>(R.id.notificationHeadline).text = "notifications (${allNotifications.size})"

            // Adapter setup / update
            if (!::notificationAdapter.isInitialized) {
                notificationAdapter = NotificationAdapter(
                    items = allNotifications,
                    layoutResId = R.layout.item_notification,
                    db,
                    userId
                )
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(allNotifications)
            }
        }
    }
}