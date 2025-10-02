package com.example.onyx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PayWall : AppCompatActivity() {
    
    private lateinit var btnPurchase: Button
    private lateinit var btnContinueFree: TextView
    private lateinit var btnRestorePurchase: TextView
    private lateinit var trendingMoviesRecyclerView: RecyclerView
    private lateinit var adapter: GridAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pay_wall)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //setupClickListeners()
        loadTrendingMovies()
    }
    

    
    private fun setupClickListeners() {
        btnPurchase.setOnClickListener {
            // TODO: Implement payment processing
            handlePurchase()
        }
        
        btnContinueFree.setOnClickListener {
            navigateToHome()
        }
        
        btnRestorePurchase.setOnClickListener {
            // TODO: Implement restore purchase functionality
            handleRestorePurchase()
        }
    }
    


    private fun loadTrendingMovies() {
        loadingAnimation.show(this@PayWall)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/trending/all/day"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = org.json.JSONObject(response)
                val moviesArray = jsonObject.getJSONArray("results")

                val outputList = mutableListOf<String>()



                for (i in 0 until moviesArray.length()) {
                    val current = moviesArray.getJSONObject(i)
                    val poster = current.optString("poster_path", "")
                    val backdrop_path = current.optString("backdrop_path", "")

                    if (poster.isNotBlank() && !poster.endsWith("null")) {
                        val imgUrl = "https://image.tmdb.org/t/p/w780$poster"
                        val imgUrls = "https://image.tmdb.org/t/p/w1280$backdrop_path"
                        outputList.add(imgUrls)
                    }
                }

                val displaySection = findViewById<ImageView>(R.id.paywallShow)

                // Loop posters like a slideshow
                CoroutineScope(Dispatchers.Main).launch {
                    while (true) {
                        for (imgUrl in outputList) {
                            Glide.with(this@PayWall)
                                .load(imgUrl)
                                .centerCrop()
                                .into(displaySection)

                            delay(10500) // 1.5 seconds per image
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("PayWall", "Error loading trending movies", e)
            }
        }
    }


    private fun handlePurchase() {
        // TODO: Implement actual payment processing
        // For now, just simulate successful purchase and navigate to home
        Log.d("PayWall", "Purchase button clicked")
        
        // Simulate payment success
        navigateToHome()
    }
    
    private fun handleRestorePurchase() {
        // TODO: Implement restore purchase functionality
        Log.d("PayWall", "Restore purchase clicked")
        
        // For now, just navigate to home
        navigateToHome()
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, Home_Page::class.java)
        startActivity(intent)
        finish()
    }
}