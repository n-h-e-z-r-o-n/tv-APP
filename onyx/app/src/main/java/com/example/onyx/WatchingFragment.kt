
package com.example.onyx
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.cWatchingAdapter
import com.example.onyx.OnyxObjects.GlobalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WatchingFragment : Fragment(R.layout.fragment_watching) {
    private lateinit var showsWatchingRecycler: RecyclerView
    private lateinit var animeWatchingRecycler: RecyclerView
    private lateinit var showsWatchingSection: LinearLayout
    private lateinit var animeWatchingSection: LinearLayout
    private lateinit var showsWatchAdapter: cWatchingAdapter
    private lateinit var animeWatchAdapter: cWatchingAdapter
    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private var lastFocusedView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase(requireContext())
        sm = SessionManger(requireActivity())
        
        view.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && view.findViewById<View>(newFocus.id) != null) {
                lastFocusedView = newFocus
            }
        }
        val fragmentScrollView = requireView().findViewById<ScrollView>(R.id.fragmentScrollView)

        showsWatchingSection = view.findViewById(R.id.showsWatchingSection)
        animeWatchingSection = view.findViewById(R.id.animeWatchingSection)

        GlobalUtils.centerParentOnFocus(fragmentScrollView, showsWatchingSection)
        GlobalUtils.centerParentOnFocus(fragmentScrollView, animeWatchingSection)


        showsWatchingRecycler = view.findViewById(R.id.showsWatchingRecycler)
        showsWatchingRecycler.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        animeWatchingRecycler = view.findViewById(R.id.animeWatchingRecycler)
        animeWatchingRecycler.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        showsWatchedList()
        animeWatchedList()
    }

    override fun onResume() {
        super.onResume()
        requireView().post {
            if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                lastFocusedView!!.requestFocus()
            } else {
                showsWatchingRecycler.requestFocus()
            }
        }
    }

    private fun showsWatchedList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = sm.getUserId()
            // Fetch both lists in parallel on IO
            val (movies, tvShows) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val mv = async { db.getContinueWatchingAll(userId, "movie") }
                    val tv = async { db.getContinueWatchingAll(userId, "tv") }
                    mv.await() to tv.await()
                }
            }
            // Combine + sort
            val combinedList = (movies + tvShows)
                .sortedByDescending { it["updated_at"]?.toLongOrNull() ?: 0L }
            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                if (combinedList.isNotEmpty()) {
                    if (!::showsWatchAdapter.isInitialized) {
                        showsWatchAdapter = cWatchingAdapter(
                            combinedList.toMutableList(),
                            R.layout.item_watched
                        )
                        showsWatchingRecycler.adapter = showsWatchAdapter
                    } else {
                        showsWatchAdapter.updateItems(combinedList)
                    }
                    showsWatchingSection.visibility = View.VISIBLE
                } else {
                    showsWatchingSection.visibility = View.GONE
                }
            }
        }
    }



    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            // The fragment is now hidden, so bring back the bottom nav for the other fragments
        } else {
            if (this::showsWatchAdapter.isInitialized) {
                showsWatchAdapter.clearItems()
            }

            if (this::animeWatchAdapter.isInitialized) {
                animeWatchAdapter.clearItems()
            }

            animeWatchedList()
            showsWatchedList()
        }
    }

    private fun animeWatchedList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = sm.getUserId()
            val cWatching = withContext(Dispatchers.IO) {
                val mv = async { db.getContinueWatchingAll(userId, "anime") }
                mv.await()
            }
            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                if (cWatching.isNotEmpty()) {
                    animeWatchingSection.visibility = View.VISIBLE
                    if (!::animeWatchAdapter.isInitialized) {
                        animeWatchAdapter = cWatchingAdapter(
                            cWatching,
                            R.layout.item_watched
                        )
                        animeWatchingRecycler.adapter = animeWatchAdapter
                    } else {
                        animeWatchAdapter.updateItems(cWatching)
                    }
                } else {
                    animeWatchingSection.visibility = View.GONE
                }
            }
        }
    }



}