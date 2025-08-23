package com.example.onyx

class SidbarAction {

    protected fun setupSidebar() {
        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)

        btnHome?.setOnClickListener {
            if (this !is MainActivity) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        btnMovies?.setOnClickListener {
            if (this !is Movie_Page) {
                startActivity(Intent(this, Movie_Page::class.java))
            }
        }

        btnTvShows?.setOnClickListener {
            if (this !is Tv_Page) {
                startActivity(Intent(this, Tv_Page::class.java))
            }
        }

        btnSearch?.setOnClickListener {
            if (this !is Search_Page) {
                startActivity(Intent(this, Search_Page::class.java))
            }
        }

        btnProfile?.setOnClickListener {
            if (this !is Profile_Page) {
                startActivity(Intent(this, Profile_Page::class.java))
            }
        }
    }

}