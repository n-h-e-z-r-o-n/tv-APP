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
                    layoutResId = R.layout.item_notification
                ) { clickedItem ->
                    // Handle Database deletion
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (clickedItem.type == "anime") {
                            db.deleteAnimeNotificationById(
                                userId = userId,
                                animeId = clickedItem.imdbCode,
                                notificationId = clickedItem.notificationId
                            )
                        } else {
                            db.deleteTvNotifications(userId, clickedItem.imdbCode)
                        }
                    }

                    // Handle Navigation
                    if (clickedItem.type == "anime") {
                        val args = android.os.Bundle().apply {
                            putString("anime_code", clickedItem.imdbCode)
                            putString("anime_poster", clickedItem.imageUrl)
                        }
                        (requireContext() as HomeActivity).navigateToFragment(WatchAnimeFragment(), args)
                    } else {
                        val intent = android.content.Intent(requireContext(), Watch_Page::class.java)
                        intent.putExtra("imdb_code", clickedItem.imdbCode)
                        intent.putExtra("type", clickedItem.type)
                        requireContext().startActivity(intent)
                    }
                }
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(allNotifications)
            }
        }
    }
}