package com.example.onyx

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import android.view.View



class PayWall : AppCompatActivity() {
    
    private lateinit var btnPurchase: Button
    private lateinit var btnContinueFree: TextView
    private lateinit var PaymentContainer: CardView
    private lateinit var btnRestorePurchase: TextView
    
    // Payment form elements
    private lateinit var btnClosePayment: TextView
    private lateinit var btnCompletePayment: Button
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiryDate: EditText
    private lateinit var etCVC: EditText
    private lateinit var etCardholderName: EditText
    private lateinit var etCountry: TextView

    
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pay_wall)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        loadTrendingMovies()

        btnPurchase = findViewById(R.id.btnPurchase)
        PaymentContainer = findViewById(R.id.PaymentContainer)
        
        // Initialize payment form elements
        btnClosePayment = findViewById(R.id.btnClosePayment)
        btnCompletePayment = findViewById(R.id.btnCompletePayment)
        etCardNumber = findViewById(R.id.etCardNumber)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        etCVC = findViewById(R.id.etCVC)
        etCardholderName = findViewById(R.id.etCardholderName)
        etCountry = findViewById(R.id.etCountry)
        
        // Set up click listeners
        btnPurchase.setOnClickListener {
            PaymentContainer.visibility = View.VISIBLE
        }
        
        btnClosePayment.setOnClickListener {
            PaymentContainer.visibility = View.GONE
        }
        
        btnCompletePayment.setOnClickListener {
            // Handle payment completion
            processPayment()
        }
        
        // Add card number formatting
        etCardNumber.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().replace(" ", "")
                val formatted = text.chunked(4).joinToString(" ")
                if (formatted != s.toString()) {
                    etCardNumber.setText(formatted)
                    etCardNumber.setSelection(formatted.length)
                }
            }
        })
        
        // Add expiry date formatting
        etExpiryDate.addTextChangedListener(object : android.text.TextWatcher {
            private var isDeleting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Track if the user is deleting
                isDeleting = count > after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString()

                // ✅ Auto insert "/" after typing 2 digits (if not deleting)
                if (!isDeleting && text.length == 2 && !text.contains("/")) {
                    etExpiryDate.setText("$text/")
                    etExpiryDate.setSelection(etExpiryDate.text.length)
                }
            }
        })

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

                            delay(20500) // 1.5 seconds per image
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("PayWall", "Error loading trending movies", e)
            }
        }
    }
    
    private fun processPayment() {
        // Validate form fields
        val cardNumber = etCardNumber.text.toString().replace(" ", "")
        val expiryDate = etExpiryDate.text.toString()
        val cvc = etCVC.text.toString()
        val cardholderName = etCardholderName.text.toString()
        val countryHolderName = etCardholderName.text.toString()

        // Basic validation
        if (cardNumber.length < 16) {
            etCardNumber.error = "Invalid card number"
            return
        }
        
        /*
        if (expiryDate.length < 7) {
            etExpiryDate.error = "Invalid expiry date"
            return
        }
         */

        
        if (cvc.length < 3) {
            etCVC.error = "Invalid CVC"
            return
        }
        
        if (cardholderName.isEmpty()) {
            etCardholderName.error = "Cardholder name required"
            return
        }

        if (countryHolderName.isEmpty()) {
            etCountry.error = "Cardholder name required"
            return
        }
        
        // Simulate payment processing
        btnCompletePayment.text = "Processing..."
        btnCompletePayment.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // Simulate processing time
            
            // Hide payment container and navigate to home
            //PaymentContainer.visibility = View.GONE
            //navigateToHome()
        }
    }

    
    private fun navigateToHome() {
        val intent = Intent(this, Home_Page::class.java)
        startActivity(intent)
        finish()
    }
}