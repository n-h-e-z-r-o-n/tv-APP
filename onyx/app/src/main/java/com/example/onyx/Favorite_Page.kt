package com.example.onyx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView


class Favorite_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorite_page)
        NavAction.setupSidebar(this@Favorite_Page)

        val recyclerView = findViewById<RecyclerView>(R.id.favoritesRecycler)
        val emptyState = findViewById<TextView>(R.id.emptyState)

        val spacingPx = (16 * resources.displayMetrics.density).toInt()
        val itemMinWidthDp = 160
        val itemMinWidthPx = (itemMinWidthDp * resources.displayMetrics.density).toInt()
        val screenWidthPx = resources.displayMetrics.widthPixels
        val spanCount = maxOf(1, (screenWidthPx - (40 * resources.displayMetrics.density).toInt()) / itemMinWidthPx)

        recyclerView.layoutManager = GridLayoutManager(this, spanCount)

        val favorites = FavoritesManager.getFavorites(this)
        if (favorites.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE

            val items = favorites.map {
                MovieItem(
                    title = it.title,
                    imageUrl = it.imageUrl,
                    imdbCode = it.id,
                    type = it.type
                )
            }.toMutableList()

            recyclerView.adapter = OtherAdapter(items, R.layout.square_card)
        }
    }
}