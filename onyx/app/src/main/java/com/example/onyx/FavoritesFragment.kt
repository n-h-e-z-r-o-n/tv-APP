package com.example.onyx

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.databinding.FragmentFavoritesBinding
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
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
    private var favoritesLoaded = false

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

        binding.favoritesRecycler.layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
            alignItems = AlignItems.FLEX_START
        }
        binding.favoritesRecycler.isNestedScrollingEnabled = false

        loadFavorites()
    }

    override fun onResume() {
        super.onResume()

        if (GlobalUtils.favoritesStateHasChanged && favoritesLoaded) {
            GlobalUtils.favoritesStateHasChanged = false
            loadFavorites()
        } else {
            restoreFavoriteFocus()
        }
    }

    private fun loadFavorites() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val items = withContext(Dispatchers.IO) {
                val animeFavData = db.getFavoriteAnime(userId)
                val showFavData = db.getFavoriteShows(userId)

                val combinedItems = mutableListOf<FavItem>()

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
                binding.favoritesCount.text = items.size.toString()

                if (!::favoritesAdapter.isInitialized) {
                    favoritesAdapter = FavAdapter(items.toMutableList(), R.layout.square_card)
                    binding.favoritesRecycler.adapter = favoritesAdapter
                } else {
                    favoritesAdapter.updateItems(items)
                }
            }

            favoritesLoaded = true
            restoreFavoriteFocus()
        }
    }

    private fun restoreFavoriteFocus() {
        val previousFocus = lastFocusedView

        if (
            previousFocus != null &&
            previousFocus.isAttachedToWindow &&
            previousFocus.isShown &&
            previousFocus.isFocusable
        ) {
            previousFocus.requestFocus()
            return
        }

        val firstChild = binding.favoritesRecycler.layoutManager?.findViewByPosition(0)
        if (firstChild != null && firstChild.isFocusable) {
            firstChild.requestFocus()
        } else {
            binding.favoritesRecycler.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
