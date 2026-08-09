package com.example.onyx

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.databinding.FragmentFavoritesBinding
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private var userId: Int = -1

    private var lastFocusedFavoriteKey: String? = null
    private var favoritesLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        userId = sm.getUserId()

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
                renderFavorites(items)
            }

            favoritesLoaded = true
            restoreFavoriteFocus()
        }
    }

    private fun renderFavorites(items: List<FavItem>) {
        binding.favoritesFlexbox.removeAllViews()

        items.forEach { item ->
            val cardView = layoutInflater.inflate(
                R.layout.square_card,
                binding.favoritesFlexbox,
                false
            )

            val imageView = cardView.findViewById<ImageView>(R.id.itemImage)
            val titleView = cardView.findViewById<TextView>(R.id.itemText)
            val favoriteKey = "${item.showType}:${item.imdbCode}"

            titleView.text = item.title
            cardView.tag = favoriteKey

            Glide.with(cardView.context)
                .load(
                    GlobalUtils.getOptimizedBackdropUrl(item.backdropUrl).ifBlank {
                        GlobalUtils.getOptimizedPosterUrl(item.posterUrl)
                    }
                )
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .into(imageView)

            cardView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    lastFocusedFavoriteKey = favoriteKey
                }
            }

            cardView.setOnClickListener {
                openFavorite(item)
            }

            val layoutParams = cardView.layoutParams
            if (layoutParams is FlexboxLayout.LayoutParams) {
                layoutParams.flexShrink = 1f
                layoutParams.flexGrow = 0f
                cardView.layoutParams = layoutParams
            } else {
                cardView.layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    flexShrink = 1f
                    flexGrow = 0f
                }
            }

            binding.favoritesFlexbox.addView(cardView)
        }
    }

    private fun openFavorite(item: FavItem) {
        val context = requireActivity()
        if (item.showType == "anime") {
            val args = Bundle().apply {
                putString("anime_code", item.imdbCode)
                putString("anime_poster", item.posterUrl)
            }
            (context as HomeActivity).navigateToFragment(WatchAnimeFragment(), args)
        } else {
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", item.imdbCode)
                putExtra("type", item.showType)
            }
            context.startActivity(intent)
        }
    }

    private fun restoreFavoriteFocus() {
        val favoriteKey = lastFocusedFavoriteKey
        if (favoriteKey != null) {
            for (index in 0 until binding.favoritesFlexbox.childCount) {
                val child = binding.favoritesFlexbox.getChildAt(index)
                if (favoriteKey == child.tag && child.isFocusable) {
                    child.requestFocus()
                    return
                }
            }
        }

        val firstChild = binding.favoritesFlexbox.getChildAt(0)
        if (firstChild != null && firstChild.isFocusable) {
            firstChild.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
