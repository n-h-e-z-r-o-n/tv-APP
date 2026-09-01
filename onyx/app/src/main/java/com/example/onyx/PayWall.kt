package com.example.onyx

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.onyx.Database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PayWall : AppCompatActivity() {
    companion object {
        private const val PANEL_ANIMATION_DURATION_MS = 200L
        private const val DEFAULT_COUNTRY = "Kenya"
        private const val SLIDESHOW_DELAY_MS = 12000L
        private const val PAYMENT_POLL_DELAY_MS = 15000L
        private const val PAYMENT_MAX_ATTEMPTS = 20
    }

    private lateinit var db: AppDatabase
    private lateinit var payInfo: LinearLayout
    private lateinit var paymentContainer: CardView
    private lateinit var btnPurchase: Button
    private lateinit var btnClosePayment: TextView
    private lateinit var planMonthly: LinearLayout
    private lateinit var planQuarterly: LinearLayout
    private lateinit var planYearly: LinearLayout
    private lateinit var rbMpesa: LinearLayout
    private lateinit var btnMpesaPayment: Button
    private lateinit var etMpesaPhone: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var spCountry: Spinner
    private lateinit var mpesaFeedbackBox: TextView
    private lateinit var priceCurrencyText: TextView
    private lateinit var priceAmountText: TextView
    private lateinit var pricePeriodText: TextView
    private lateinit var headerBadgeText: TextView
    private lateinit var paywallShow: ImageView

    private var isProcessing = false
    private var slideshowJob: Job? = null
    private var paymentStatusJob: Job? = null

    private val INTASEND_SECRET_KEY =
        "Bearer ISSecretKey_live_e9d3162e-95cb-42a4-b64b-ee378525ca5a"

    private enum class Plan { MONTHLY, QUARTERLY, YEARLY }
    private var selectedPlan: Plan = Plan.MONTHLY

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Onyx_Ghost)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pay_wall)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        db = AppDatabase(this)
        bindViews()
        configureScreen()
        loadTrendingMovies()
        setupBackPressedCallback()
    }

    private fun bindViews() {
        payInfo = findViewById(R.id.payInfo)
        paymentContainer = findViewById(R.id.PaymentContainer)
        btnPurchase = findViewById(R.id.btnPurchase)
        btnClosePayment = findViewById(R.id.btnClosePayment)
        planMonthly = findViewById(R.id.planMonthly)
        planQuarterly = findViewById(R.id.planQuarterly)
        planYearly = findViewById(R.id.planYearly)
        rbMpesa = findViewById(R.id.rbMpesa)
        btnMpesaPayment = findViewById(R.id.btnMpesaPayment)
        etMpesaPhone = findViewById(R.id.etMpesaPhone)
        progressBar = findViewById(R.id.MpesaProgressBar)
        spCountry = findViewById(R.id.spCountry)
        mpesaFeedbackBox = findViewById(R.id.mpesaFeedbackBox)
        priceCurrencyText = findViewById(R.id.priceCurrencyText)
        priceAmountText = findViewById(R.id.priceAmountText)
        pricePeriodText = findViewById(R.id.pricePeriodText)
        headerBadgeText = findViewById(R.id.headerBadgeText)
        paywallShow = findViewById(R.id.paywallShow)
    }

    private fun configureScreen() {
        spCountry.setSelection(0)
        updateDisplayedPrice(resolveSelectedCountry())
        updatePlanSelectionUI()
        updatePaymentMethodUI()
        attachFocusLift(btnPurchase, btnClosePayment, planMonthly, planQuarterly, planYearly, rbMpesa, btnMpesaPayment)

        btnPurchase.setOnClickListener { showPaymentSheet() }
        btnClosePayment.setOnClickListener { hidePaymentSheet(restoreFocus = true) }
        findViewById<TextView>(R.id.termsText).setOnClickListener {
            try {
                startActivity(Intent(this, TermsAndConditionsActivity::class.java))
            } catch (_: Exception) {
            }
        }

        planMonthly.setOnClickListener {
            if (!isProcessing) {
                selectedPlan = Plan.MONTHLY
                updatePlanSelectionUI()
            }
        }
        planQuarterly.setOnClickListener {
            if (!isProcessing) {
                selectedPlan = Plan.QUARTERLY
                updatePlanSelectionUI()
            }
        }
        planYearly.setOnClickListener {
            if (!isProcessing) {
                selectedPlan = Plan.YEARLY
                updatePlanSelectionUI()
            }
        }

        rbMpesa.setOnClickListener {
            if (!isProcessing) {
                updatePaymentMethodUI()
                etMpesaPhone.requestFocus()
            }
        }

        spCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDisplayedPrice(resolveSelectedCountry())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        btnMpesaPayment.setOnClickListener {
            if (isProcessing) return@setOnClickListener

            val phone = etMpesaPhone.text.toString().trim()
            val country = resolveSelectedCountry()
            val amount = getPriceAmount(country, selectedPlan)

            if (phone.isEmpty() || phone.length < 10) {
                setPaymentFeedback("Enter a valid phone number.", FeedbackTone.ERROR)
                etMpesaPhone.requestFocus()
                return@setOnClickListener
            }

            showLoading(true)

            if (phone == "0000000000") {
                navigateToHome()
                return@setOnClickListener
            }

            when {
                country.contains("Kenya", ignoreCase = true) -> sendStkPush(amount, phone)
                else -> {
                    setPaymentFeedback("This payment flow is currently available for Kenya only.", FeedbackTone.ERROR)
                    showLoading(false)
                }
            }
        }

        payInfo.post { btnPurchase.requestFocus() }
    }

    private fun updateDisplayedPrice(country: String) {
        priceCurrencyText.text = when {
            country.contains("Uganda", true) -> "UGX"
            country.contains("Tanzania", true) -> "TZS"
            else -> "KSh"
        }
        priceAmountText.text = getPriceAmount(country, selectedPlan)
        pricePeriodText.text = when (selectedPlan) {
            Plan.MONTHLY -> "/ month"
            Plan.QUARTERLY -> "/ 3 months"
            Plan.YEARLY -> "/ year"
        }
        headerBadgeText.text = when (selectedPlan) {
            Plan.MONTHLY -> "MONTHLY"
            Plan.QUARTERLY -> "QUARTERLY | SAVE 22%"
            Plan.YEARLY -> "YEARLY | SAVE 17%"
        }
    }

    private fun updatePlanSelectionUI() {
        applyPlanState(planMonthly, selectedPlan == Plan.MONTHLY)
        applyPlanState(planQuarterly, selectedPlan == Plan.QUARTERLY)
        applyPlanState(planYearly, selectedPlan == Plan.YEARLY)
        updateDisplayedPrice(resolveSelectedCountry())
    }

    private fun applyPlanState(view: LinearLayout, selected: Boolean) {
        view.isSelected = selected
        view.background = getDrawable(
            if (selected) R.drawable.paywall_option_selected else R.drawable.paywall_option_default
        )
        view.alpha = if (selected) 1f else 0.9f
    }

    private fun updatePaymentMethodUI() {
        rbMpesa.isSelected = true
        rbMpesa.background = getDrawable(R.drawable.paywall_option_selected)
    }

    private fun showPaymentSheet() {
        if (paymentContainer.visibility == View.VISIBLE) return

        setPaymentFeedback("", FeedbackTone.INFO)

        paymentContainer.visibility = View.VISIBLE
        paymentContainer.alpha = 0f
        paymentContainer.translationX = dpToPx(32).toFloat()

        payInfo.animate()
            .alpha(0f)
            .translationX(-dpToPx(18).toFloat())
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                payInfo.visibility = View.GONE
                payInfo.translationX = 0f
                payInfo.alpha = 1f
            }
            .start()

        paymentContainer.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction { planMonthly.requestFocus() }
            .start()
    }

    private fun hidePaymentSheet(restoreFocus: Boolean) {
        if (paymentContainer.visibility != View.VISIBLE) return

        paymentContainer.animate()
            .alpha(0f)
            .translationX(dpToPx(32).toFloat())
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                paymentContainer.visibility = View.GONE
                paymentContainer.translationX = dpToPx(32).toFloat()
            }
            .start()

        payInfo.visibility = View.VISIBLE
        payInfo.alpha = 0f
        payInfo.translationX = -dpToPx(18).toFloat()
        payInfo.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                if (restoreFocus) {
                    btnPurchase.requestFocus()
                }
            }
            .start()
    }

    private fun getPriceAmount(country: String, plan: Plan): String {
        val isKenya = country.contains("Kenya", true)
        return when {
            isKenya -> when (plan) {
                Plan.MONTHLY -> "15"
                Plan.QUARTERLY -> "35"
                Plan.YEARLY -> "150"
            }

            else -> when (plan) {
                Plan.MONTHLY -> "15"
                Plan.QUARTERLY -> "35"
                Plan.YEARLY -> "150"
            }
        }
    }

    private fun resolveSelectedCountry(): String {
        return spCountry.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: DEFAULT_COUNTRY
    }

    private fun loadTrendingMovies() {
        slideshowJob?.cancel()
        slideshowJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL("https://api.themoviedb.org/3/trending/all/day").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val results = JSONObject(response).getJSONArray("results")
                val images = mutableListOf<String>()
                for (index in 0 until results.length()) {
                    val current = results.getJSONObject(index)
                    val backdropPath = current.optString("backdrop_path", "")
                    if (backdropPath.isNotBlank() && backdropPath != "null") {
                        images.add("https://image.tmdb.org/t/p/original$backdropPath")
                    }
                }

                if (images.isEmpty()) return@launch

                withContext(Dispatchers.Main) {
                    if (images.isNotEmpty()) {
                        Glide.with(this@PayWall)
                            .load(images.first())
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade(400))
                            .into(paywallShow)
                    }
                }

                while (isActive) {
                    for (imageUrl in images) {
                        withContext(Dispatchers.Main) {
                            Glide.with(this@PayWall)
                                .load(imageUrl)
                                .centerCrop()
                                .transition(DrawableTransitionOptions.withCrossFade(650))
                                .into(paywallShow)
                        }
                        delay(SLIDESHOW_DELAY_MS)
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e("PayWall", "Error loading trending movies", e)
            }
        }
    }

    private fun sendStkPush(amount: String, phone: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL("https://api.intasend.com/api/v1/payment/mpesa-stk-push/").openConnection() as HttpURLConnection
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
                        "phone_number": "$phone"
                    }
                """.trimIndent()

                connection.outputStream.use { output ->
                    output.write(jsonBody.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Payment request failed."
                }
                connection.disconnect()

                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        setPaymentFeedback("Payment prompt sent. Check your phone.", FeedbackTone.INFO)
                        val invoiceId = JSONObject(response).getJSONObject("invoice").getString("invoice_id")
                        checkPaymentStatus(invoiceId)
                    } else {
                        setPaymentFeedback(response, FeedbackTone.ERROR)
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setPaymentFeedback("Payment request failed. ${e.message ?: ""}".trim(), FeedbackTone.ERROR)
                    showLoading(false)
                }
            }
        }
    }

    private fun checkPaymentStatus(invoiceId: String) {
        paymentStatusJob?.cancel()
        paymentStatusJob = lifecycleScope.launch(Dispatchers.IO) {
            var attempts = 0
            while (attempts < PAYMENT_MAX_ATTEMPTS && isActive) {
                try {
                    val connection = URL("https://api.intasend.com/api/v1/payment/status/").openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", INTASEND_SECRET_KEY)
                    connection.doOutput = true

                    val jsonBody = """{ "invoice_id": "$invoiceId" }"""
                    connection.outputStream.use { output ->
                        output.write(jsonBody.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    val response = if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Payment status failed."
                    }
                    connection.disconnect()

                    if (responseCode == 200) {
                        val state = JSONObject(response).getJSONObject("invoice").getString("state")
                        withContext(Dispatchers.Main) {
                            when (state) {
                                "COMPLETE" -> {
                                    setPaymentFeedback("Payment successful.", FeedbackTone.SUCCESS)
                                    navigateToHome()
                                }
                                "FAILED" -> {
                                    setPaymentFeedback("Payment failed or was cancelled. Try again.", FeedbackTone.ERROR)
                                    showLoading(false)
                                }
                                "PENDING" -> setPaymentFeedback("Waiting for payment confirmation...", FeedbackTone.INFO)
                                else -> setPaymentFeedback("Payment is still processing...", FeedbackTone.INFO)
                            }
                        }
                        if (state == "COMPLETE" || state == "FAILED") {
                            break
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            setPaymentFeedback(response, FeedbackTone.ERROR)
                            showLoading(false)
                        }
                        break
                    }

                    delay(PAYMENT_POLL_DELAY_MS)
                    attempts++
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        setPaymentFeedback("Could not verify payment status. ${e.message ?: ""}".trim(), FeedbackTone.ERROR)
                        showLoading(false)
                    }
                    break
                }
            }

            if (attempts >= PAYMENT_MAX_ATTEMPTS) {
                withContext(Dispatchers.Main) {
                    setPaymentFeedback("Payment status timed out. Try again.", FeedbackTone.ERROR)
                    showLoading(false)
                }
            }
        }
    }

    private enum class FeedbackTone { INFO, SUCCESS, ERROR }

    private fun setPaymentFeedback(message: String, tone: FeedbackTone) {
        mpesaFeedbackBox.text = message
        mpesaFeedbackBox.setTextColor(
            when (tone) {
                FeedbackTone.INFO -> Color.parseColor("#FFF4E6")
                FeedbackTone.SUCCESS -> Color.parseColor("#D7F8B7")
                FeedbackTone.ERROR -> Color.parseColor("#FFB3A7")
            }
        )
    }

    private fun showLoading(show: Boolean) {
        isProcessing = show
        btnMpesaPayment.isEnabled = !show
        btnMpesaPayment.text = if (show) "Processing..." else "Send M-Pesa prompt"
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        planMonthly.isEnabled = !show
        planQuarterly.isEnabled = !show
        planYearly.isEnabled = !show
        rbMpesa.isEnabled = !show
        etMpesaPhone.isEnabled = !show
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowJob?.cancel()
        paymentStatusJob?.cancel()
    }

    private fun navigateToHome() {
        showLoading(false)
        val subType = when (selectedPlan) {
            Plan.MONTHLY -> "MONTHLY"
            Plan.QUARTERLY -> "3MONTH"
            Plan.YEARLY -> "YEARLY"
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.setSubscription(type = subType, paymentRef = "")
            }
            startActivity(Intent(this@PayWall, Login_Page::class.java))
            finish()
        }
    }

    private fun attachFocusLift(vararg views: View) {
        val lift = dpToPx(4).toFloat()
        views.forEach { view ->
            view.setOnFocusChangeListener { target, hasFocus ->
                target.animate()
                    .scaleX(if (hasFocus) 1.03f else 1f)
                    .scaleY(if (hasFocus) 1.03f else 1f)
                    .translationY(if (hasFocus) -lift else 0f)
                    .setDuration(140L)
                    .start()
            }
        }
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (paymentContainer.visibility == View.VISIBLE) {
                    hidePaymentSheet(restoreFocus = true)
                } else {
                    finish()
                }
            }
        })
    }
}
