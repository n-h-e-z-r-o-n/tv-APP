package com.example.onyx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.net.HttpURLConnection
import java.net.URL
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlin.concurrent.thread
import android.os.Handler
import android.widget.AdapterView
import java.util.concurrent.Executor

class PayWall : AppCompatActivity() {


    private var isProcessing: Boolean = false
    private var slideshowJob: Job? = null
    private lateinit var btnMpesaPayment: Button
    private lateinit var etMpesaPhone: EditText
    private lateinit var progressBar: ProgressBar

    private lateinit var mpesaFeedbackBox: TextView
    private val INTASEND_SECRET_KEY = "Bearer ISSecretKey_live_e9d3162e-95cb-42a4-b64b-ee378525ca5a"

    private enum class Plan { MONTHLY, QUARTERLY, YEARLY }
    private var selectedPlan: Plan = Plan.MONTHLY







    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pay_wall)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        loadTrendingMovies()

        ////////////////////////////////////////////////////////////////////////////////////////////
        progressBar =  findViewById<ProgressBar>(R.id.MpesaProgressBar)
        mpesaFeedbackBox = findViewById<TextView>(R.id.mpesaFeedbackBox)
        ////////////////////////////////////////////////////////////////////////////////////////////

        val PaymentContainer = findViewById<CardView>(R.id.PaymentContainer)
        val btnPurchase = findViewById<Button>(R.id.btnPurchase)
        val btnClosePayment = findViewById<TextView>(R.id.btnClosePayment)
        val payInfo = findViewById<LinearLayout>(R.id.payInfo)
        val termsText = findViewById<TextView>(R.id.termsText)
        val planMonthly = findViewById<LinearLayout>(R.id.planMonthly)
        val planQuarterly = findViewById<LinearLayout>(R.id.planQuarterly)
        val planYearly = findViewById<LinearLayout>(R.id.planYearly)
        val priceCurrencyText = findViewById<TextView>(R.id.priceCurrencyText)
        val priceAmountText = findViewById<TextView>(R.id.priceAmountText)
        val pricePeriodText = findViewById<TextView>(R.id.pricePeriodText)

        // Initialize payment method views and country spinner early (used below)
        val rbMpesa = findViewById<LinearLayout>(R.id.rbMpesa)
        val rbCard = findViewById<LinearLayout>(R.id.rbCard)
        val rbGooglePay = findViewById<LinearLayout>(R.id.rbGooglePay)
        val mpesaSection = findViewById<LinearLayout>(R.id.mpesaSection)
        val cardSection = findViewById<LinearLayout>(R.id.cardSection)
        btnMpesaPayment = findViewById<Button>(R.id.btnMpesaPayment)
        etMpesaPhone  = findViewById<EditText>(R.id.etMpesaPhone)
        val spCountry  = findViewById<Spinner>(R.id.spCountry)



        btnPurchase.setOnClickListener {
            PaymentContainer.visibility = View.VISIBLE
            payInfo.visibility = View.GONE
        }

        fun updateDisplayedPrice(country: String) {
            // Currency label
            val currencyLabel = when {
                country.contains("Uganda", true) -> "UGX"
                country.contains("Tanzania", true) -> "TZS"
                else -> "KSh"
            }
            priceCurrencyText.text = currencyLabel

            // Amount and period by plan
            priceAmountText.text = getPriceAmount(country, selectedPlan)
            pricePeriodText.text = when (selectedPlan) {
                Plan.MONTHLY -> "/ month"
                Plan.QUARTERLY -> "/ 3 months"
                Plan.YEARLY -> "/ year"
            }
        }

        // Reflect price when country changes
        spCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val country = spCountry.selectedItem?.toString() ?: return
                updateDisplayedPrice(country)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        btnClosePayment.setOnClickListener {
            PaymentContainer.visibility = View.GONE
            payInfo.visibility = View.VISIBLE

        }
        termsText.setOnClickListener {
            try {
                startActivity(Intent(this, TermsAndConditionsActivity::class.java))
            } catch (_: Exception) { }
        }

        fun updatePlanSelectionUI() {
            // Visual selection: stronger outline for selected, normal selector for others
            fun applySelected(v: LinearLayout, selected: Boolean) {
                v.isSelected = selected
                if (selected) {
                    v.background = ContextCompat.getDrawable(this, R.drawable.episode_selected)
                    v.elevation = 4f
                    v.alpha = 1f
                } else {
                    v.background = ContextCompat.getDrawable(this, R.drawable.tv_button_selector)
                    v.elevation = 0f
                    v.alpha = 0.95f
                }
            }

            applySelected(planMonthly, selectedPlan == Plan.MONTHLY)
            applySelected(planQuarterly, selectedPlan == Plan.QUARTERLY)
            applySelected(planYearly, selectedPlan == Plan.YEARLY)

            // Reflect price on main card based on current country selection
            val country = spCountry.selectedItem?.toString() ?: "Kenya"
            updateDisplayedPrice(country)
        }

        planMonthly.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            selectedPlan = Plan.MONTHLY
            updatePlanSelectionUI()
        }
        planQuarterly.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            selectedPlan = Plan.QUARTERLY
            updatePlanSelectionUI()
        }
        planYearly.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            selectedPlan = Plan.YEARLY
            updatePlanSelectionUI()
        }
        // Initialize selection visuals
        updatePlanSelectionUI()
        ////////////////////////////////////////////////////////////////////////////////////////////






        


        rbMpesa.setOnClickListener() {
                mpesaSection.visibility = View.VISIBLE
                cardSection.visibility = View.GONE

        }

        rbCard.setOnClickListener() {
                cardSection.visibility = View.VISIBLE
                mpesaSection.visibility = View.GONE

        }

        rbGooglePay.setOnClickListener() {
                mpesaSection.visibility = View.GONE
                cardSection.visibility = View.GONE

        }



        

        btnMpesaPayment.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            showLoading(true)
            val phone = etMpesaPhone.text.toString().trim()
            val country = spCountry.selectedItem.toString()
            val amount = getPriceAmount(country, selectedPlan)

            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }

            when {
                country.contains("Kenya", ignoreCase = true) -> {
                    sendStkPush(amount, phone)
                }
                country.contains("Uganda", ignoreCase = true) -> {
                    sendIntaSendXbPush(amount, phone, "UGX")
                }
                country.contains("Tanzania", ignoreCase = true) -> {
                    sendIntaSendXbPush(amount, phone, "TZS")
                }
                else -> {
                    Toast.makeText(this, "Please select a valid country", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
        }

    }

    



    private fun getPriceAmount(country: String, plan: Plan): String {
        val isKenya = country.contains("Kenya", true)
        val isUganda = country.contains("Uganda", true)
        val isTanzania = country.contains("Tanzania", true)

        return when {
            isKenya -> when (plan) { Plan.MONTHLY -> "20"; Plan.QUARTERLY -> "50"; Plan.YEARLY -> "180" }
            isUganda -> when (plan) { Plan.MONTHLY -> "5000"; Plan.QUARTERLY -> "12000"; Plan.YEARLY -> "42000" }
            isTanzania -> when (plan) { Plan.MONTHLY -> "5000"; Plan.QUARTERLY -> "12000"; Plan.YEARLY -> "42000" }
            else -> when (plan) { Plan.MONTHLY -> "20"; Plan.QUARTERLY -> "50"; Plan.YEARLY -> "180" }
        }
    }




    private fun   loadTrendingMovies() {
        LoadingAnimation.show(this@PayWall)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/trending/all/day"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
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
                slideshowJob = CoroutineScope(Dispatchers.Main).launch {
                    while (true) {
                        try {
                                for (imgUrl in outputList) {
                                    Glide.with(this@PayWall)
                                        .load(imgUrl)
                                        .centerCrop()
                                        .transition(DrawableTransitionOptions.withCrossFade(500))
                                        .into(displaySection)

                                    delay(20500)
                                }
                        } catch (e: Exception ){
                            break
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("PayWall", "Error loading trending movies", e)
            }
        }
    }




    private fun sendStkPush(amount: String, phone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prepare the URL and connection
                val url = URL("https://api.intasend.com/api/v1/payment/mpesa-stk-push/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty( "Authorization", INTASEND_SECRET_KEY  )
                connection.doOutput = true

                // Create JSON body
                val jsonBody = """
                {
                    "amount": "$amount",
                    "phone_number": "$phone"
                }
                  """.trimIndent()

                // Send request body
                connection.outputStream.use { os ->
                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // Read response
                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error"
                }

                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        Log.d("MPESA_STK_PUSH 1", "Response: $response")
                        mpesaFeedbackBox.text =  "STK Push sent! Check your phone."

                        val json = JSONObject(response)
                        val invoiceId = json.getJSONObject("invoice").getString("invoice_id")
                        checkPaymentStatus(invoiceId)
                    } else {
                        //Toast.makeText(this@PayWall, "Failed: $response", Toast.LENGTH_LONG).show()
                        mpesaFeedbackBox.text = response.toString()
                        showLoading(false)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PayWall, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
        }
    }

    private fun sendIntaSendXbPush(amount: String, phone: String, currency: String = "UGX") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.intasend.com/api/v1/payment/intasend-xb-push/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", INTASEND_SECRET_KEY)
                connection.doOutput = true

                val jsonBody = """
                {
                    "amount": "$amount",
                    "phone_number": "$phone",
                    "currency": "$currency"
                }
                """.trimIndent()

                connection.outputStream.use { os ->
                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error"
                }

                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        Log.d("INTASEND_XB_PUSH", "Response: $response")
                        mpesaFeedbackBox.text = "STK Push sent! Check your phone."

                        val json = JSONObject(response)
                        val invoiceId = json.getJSONObject("invoice").getString("invoice_id")
                        checkPaymentStatus(invoiceId)
                    } else {
                        mpesaFeedbackBox.text = response
                        showLoading(false)

                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mpesaFeedbackBox.text = "Error: ${e.message}"
                    showLoading(false)
                }
            }
        }
    }


    private fun checkPaymentStatus(invoiceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var attempts = 0
            val maxAttempts = 20
            while (attempts < maxAttempts) {
                try {
                    val url = URL("https://api.intasend.com/api/v1/payment/status/")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", INTASEND_SECRET_KEY)
                    connection.doOutput = true

                    val jsonBody = """{ "invoice_id": "$invoiceId" }"""

                    connection.outputStream.use { os ->
                        os.write(jsonBody.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    val response = if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error"
                    }
                    Log.d("MPESA_STK_PUSH 2", "Response: $response")

                    if (responseCode == 200) {
                        val json = JSONObject(response)
                        val state = json.getJSONObject("invoice").getString("state")

                        withContext(Dispatchers.Main) {

                            if (state == "COMPLETE") {
                                mpesaFeedbackBox.text = "✅ Payment successful!"
                                navigateToHome()
                            } else if (state == "FAILED") {
                                mpesaFeedbackBox.text =
                                    "❌ Payment failed or cancelled. Restart again"
                                showLoading(false)

                            } else if (state == "PENDING") {
                                Log.d("INTASEND_STATUS", "Payment still pending...")
                                mpesaFeedbackBox.text = "Payment still pending..."
                            } else {
                                Log.d("INTASEND_STATUS", "Still pending...")
                                mpesaFeedbackBox.text = "Payment still pending..."
                            }
                        }
                        if (state == "COMPLETE" || state == "FAILED") break
                    }


                    delay(15000)
                    attempts++
                } catch (e: Exception) {
                    mpesaFeedbackBox.text = e.message
                    showLoading(false)
                    break
                }
            }
            if (attempts >= maxAttempts) {
                withContext(Dispatchers.Main) {
                    mpesaFeedbackBox.text = "Payment status timeout. Try again."
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            isProcessing = show
            btnMpesaPayment.isEnabled = !show
            btnMpesaPayment.text = if (show) "Processing..." else "Initiate payment"
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            // Disable plan changes while processing
            findViewById<LinearLayout>(R.id.planMonthly)?.isEnabled = !show
            findViewById<LinearLayout>(R.id.planQuarterly)?.isEnabled = !show
            findViewById<LinearLayout>(R.id.planYearly)?.isEnabled = !show
        } else {
            // Switch to main thread
            Handler(Looper.getMainLooper()).post {
                showLoading(show)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowJob?.cancel()
    }


    private fun navigateToHome() {
        showLoading(false)
        // compute expiry based on selected plan
        val days = when (selectedPlan) {
            Plan.MONTHLY -> 30
            Plan.QUARTERLY -> 90
            Plan.YEARLY -> 365
        }
        saveSubscriptionTime(days)
        val intent = Intent(this, Home_Page::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveSubscriptionTime(planDays: Int) {
        val now = System.currentTimeMillis()
        val expiry = now + planDays * 24L * 60L * 60L * 1000L
        val prefs = getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("lastPaymentTime", now)
            .putLong("expiryTime", expiry)
            .putString("plan", selectedPlan.name)
            .apply()
    }


}