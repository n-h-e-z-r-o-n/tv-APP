package com.example.onyx

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.databinding.FragmentFavoritesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoritesAdapter: FavAdapter
    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private var userId: Int = -1

    private var lastFocusedView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        view.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && view.findViewById<View>(newFocus.id) != null) {
                lastFocusedView = newFocus
            }
        }

        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        userId = sm.getUserId()

        //binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)

        val metrics = requireContext().resources.displayMetrics
        val screenWidthPx = metrics.widthPixels.toFloat()
        val itemHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                180f,
                metrics
            ) // R.layout.square_card height
        val itemWidthPx = itemHeightPx * (16f / 9f) // TMDB backdrop ≈ 16:9
        val spanCount=  maxOf(1, (screenWidthPx / itemWidthPx).toInt())

        binding.favoritesRecycler.layoutManager =
            StaggeredGridLayoutManager(
                spanCount,
                StaggeredGridLayoutManager.VERTICAL
            )

        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        binding.root.post {
            if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                lastFocusedView!!.requestFocus()
            } else {
                binding.favoritesRecycler.requestFocus()
            }
        }
    }

    private fun loadFavorites() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val items = withContext(Dispatchers.IO) {
                val animeFavData = db.getFavoriteAnime(userId)
                val showFavData = db.getFavoriteShows(userId)

                val combinedItems = mutableListOf<FavItem>()

                // Map Anime
                combinedItems.addAll(animeFavData.map { anime ->
                    FavItem(
                        title = anime["name"] ?: "",
                        posterUrl = anime["poster"] ?: "",
                        backdropUrl = anime["poster"] ?: "",
                        releaseDate = anime["aired"] ?: "",
                        runtime = anime["duration"] ?: "",
                        overview = anime["description"] ?: "",
                        voteAverage = anime["rating"] ?: "",
                        genres = anime["genre"] ?: "",
                        production = "",
                        parentalGuide = anime["rating"] ?: "",
                        imdbCode = anime["anime_id"] ?: "",
                        showType = "anime"
                    )
                })

                // Map TV/Movies
                combinedItems.addAll(showFavData.map { show ->
                    FavItem(
                        title = show["title"] ?: "",
                        posterUrl = show["poster"] ?: "",
                        backdropUrl = show["backdrop"] ?: "",
                        releaseDate = show["year"] ?: "",
                        runtime = show["runtime"] ?: "",
                        overview = show["overview"] ?: "",
                        voteAverage = show["rating"] ?: "",
                        genres = show["genres"] ?: "",
                        production = "",
                        parentalGuide = show["pg"] ?: "",
                        imdbCode = show["show_id"] ?: "",
                        showType = "tv"
                    )
                })

                combinedItems
            }

            if (items.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.favoritesSection.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.favoritesSection.visibility = View.VISIBLE

                if (!::favoritesAdapter.isInitialized) {
                    favoritesAdapter = FavAdapter(items.toMutableList(), R.layout.square_card)
                    binding.favoritesRecycler.adapter = favoritesAdapter
                } else {
                    favoritesAdapter.updateItems(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}