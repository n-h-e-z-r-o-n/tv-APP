package com.example.onyx

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.AvatarAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.ProfileAdapter
import com.example.onyx.OnyxClasses.profileItem
import com.example.onyx.OnyxObjects.GlobalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login_Page : AppCompatActivity() {

    companion object {
        private const val PANEL_ANIMATION_DURATION_MS = 180L
    }

    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var avatarRecycler: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var avatarAdapter: AvatarAdapter
    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private var activeSub = false
    private val profiles = mutableListOf<profileItem>()
    private var selectedAvatar: String = ""
    private lateinit var profileContainer: FrameLayout
    private lateinit var settingButton: TextView
    private lateinit var settingUi: View
    private lateinit var gDriveBackup: TextView
    private lateinit var exitApp: TextView
    private lateinit var exitSetting: TextView
    private lateinit var CreateProfileContainer: FrameLayout
    private lateinit var createProfileCard: View
    private lateinit var selectedAvatarPreview: ImageView
    private lateinit var selectedAvatarLabel: TextView
    private lateinit var usernameInput: EditText
    private lateinit var usernamePinInput: EditText
    private lateinit var createProfileBtn: TextView
    private lateinit var cancelProfileBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_login_page)

        val loadingImageView = findViewById<ImageView>(R.id.backgroundImgContainer)
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.themeImage, typedValue, true)

        Glide.with(this)
            .asGif()
            .load(typedValue.resourceId)
            .into(loadingImageView)

        bindChrome()

        db = AppDatabase(this)
        sm = SessionManger(this)
        activeSub = db.isSubscriptionActive()

        val userId = sm.getUserId()
        if (userId == -1) {
            initializeWidgets()
            loadProfiles()
            setupBackPressedCallback()
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun bindChrome() {
        settingUi = findViewById(R.id.settingUi)
        settingButton = findViewById(R.id.settingButton)
        gDriveBackup = findViewById(R.id.gDriveBackup)
        exitApp = findViewById(R.id.exitApp)
        exitSetting = findViewById(R.id.exitSetting)
        CreateProfileContainer = findViewById(R.id.CreateProfileContainer)
        createProfileCard = findViewById(R.id.createProfileCard)

        settingButton.setOnClickListener { showSettingsPanel() }
        exitSetting.setOnClickListener { hideSettingsPanel(restoreFocus = true) }
        exitApp.setOnClickListener { GlobalUtils.exitApp(this) }
        gDriveBackup.setOnClickListener { backupLibrary() }

        attachFocusLift(settingButton, gDriveBackup, exitApp, exitSetting)
    }

    private fun initializeWidgets() {
        profileContainer = findViewById(R.id.profileContainer)
        selectedAvatarPreview = findViewById(R.id.selectedAvatarPreview)
        selectedAvatarLabel = findViewById(R.id.selectedAvatarLabel)
        usernameInput = findViewById(R.id.usernameInput)
        usernamePinInput = findViewById(R.id.usernamePinInput)
        createProfileBtn = findViewById(R.id.createProfileBtn)
        cancelProfileBtn = findViewById(R.id.cancelProfileBtn)

        val profileSpacing = dpToPx(12)
        val avatarSpacing = dpToPx(6)

        profileRecyclerView = findViewById(R.id.profileRecycler)
        profileRecyclerView.addItemDecoration(EqualSpaceItemDecoration(profileSpacing))
        profileRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        profileAdapter = ProfileAdapter(profiles, R.layout.item_account)
        profileRecyclerView.adapter = profileAdapter

        profileAdapter.onProfileSelected = { profile ->
            Log.d("Login_Page", "Selected profile: $profile")
            if (profile.userid == "CREATE") {
                showCreateProfilePanel()
            } else {
                routeToProfile(profile)
            }
        }

        avatarRecycler = findViewById(R.id.avatarRecycler)
        val avatarPaths = mutableListOf<String>()
        try {
            val avatarFiles = assets.list("profile_avatars") ?: emptyArray()
            for (file in avatarFiles.sorted()) {
                avatarPaths.add("profile_avatars/$file")
            }
        } catch (e: Exception) {
            Log.e("Login_Page avatar", "Error loading avatar files: ${e.message}", e)
        }

        avatarRecycler.addItemDecoration(EqualSpaceItemDecoration(avatarSpacing))
        avatarRecycler.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        avatarAdapter = AvatarAdapter(avatarPaths, R.layout.item_avatar)
        avatarRecycler.adapter = avatarAdapter
        avatarAdapter.onAvatarSelected = { avatarPath ->
            selectedAvatar = avatarPath
            updateSelectedAvatarPreview()
            updateCreateButtonState()
        }

        setupProfileUI()
        attachFocusLift(createProfileBtn, cancelProfileBtn)
        resetCreateProfileForm()
    }

    private fun loadProfiles() {
        lifecycleScope.launch {
            try {
                val users = withContext(Dispatchers.IO) { db.getUsers() }
                if (users.count == 0) {
                    withContext(Dispatchers.IO) {
                        db.setSubscription("MONTHLY", "")
                    }
                    activeSub = db.isSubscriptionActive()
                }

                profiles.clear()
                while (users.moveToNext()) {
                    val id = users.getInt(users.getColumnIndexOrThrow("id"))
                    val username = users.getString(users.getColumnIndexOrThrow("username"))
                    val avatar = users.getString(users.getColumnIndexOrThrow("avatar")) ?: ""
                    profiles.add(
                        profileItem(
                            username = username,
                            avatar = avatar,
                            userid = id.toString()
                        )
                    )
                }
                users.close()

                profiles.add(
                    profileItem(
                        username = "Create Profile",
                        avatar = "",
                        userid = "CREATE"
                    )
                )

                profileAdapter.notifyDataSetChanged()
                requestProfileRailFocusIfAppropriate()
            } catch (e: Exception) {
                Log.e("Login_Page", "Error loading profiles: ${e.message}", e)
                Toast.makeText(this@Login_Page, "Error loading profiles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfiles()
    }

    private fun setupProfileUI() {
        createProfileBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val pin = usernamePinInput.text.toString().trim()

            if (username.isEmpty()) {
                usernameInput.error = "Enter a profile name"
                usernameInput.requestFocus()
                return@setOnClickListener
            }
            if (pin.isEmpty()) {
                usernamePinInput.error = "Enter a PIN"
                usernamePinInput.requestFocus()
                return@setOnClickListener
            }
            if (selectedAvatar.isEmpty()) {
                Toast.makeText(this, "Choose an avatar first", Toast.LENGTH_SHORT).show()
                avatarRecycler.requestFocus()
                return@setOnClickListener
            }

            createProfileBtn.isEnabled = false

            try {
                val newUserId = db.addUser(username, "NAN", pin, selectedAvatar)
                if (newUserId != -1L) {
                    Toast.makeText(this, "Profile created", Toast.LENGTH_SHORT).show()
                    resetCreateProfileForm()
                    hideCreateProfilePanel(restoreFocus = false)
                    loadProfiles()
                } else {
                    updateCreateButtonState()
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                updateCreateButtonState()
                Log.e("Login_Page", "Error creating profile: ${e.message}", e)
                Toast.makeText(this, "Error creating profile", Toast.LENGTH_SHORT).show()
            }
        }

        cancelProfileBtn.setOnClickListener {
            resetCreateProfileForm()
            hideCreateProfilePanel(restoreFocus = true)
        }

        usernameInput.doAfterTextChanged { updateCreateButtonState() }
        usernamePinInput.doAfterTextChanged { updateCreateButtonState() }
    }

    private fun routeToProfile(profile: profileItem) {
        val userId = profile.userid
        val userAvatar = profile.avatar

        if (activeSub) {
            sm.saveUserId(userId.toInt())
            sm.saveAvatar(userAvatar)
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            return
        }

        lifecycleScope.launch {
            val ipState = GlobalUtils.getSavedCountryCode(this@Login_Page)
            val destination = if (ipState.equals("KE", ignoreCase = true)) {
                PayWall::class.java
            } else {
                HomeActivity::class.java
            }
            startActivity(Intent(this@Login_Page, destination))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showSettingsPanel() {
        if (settingUi.visibility == View.VISIBLE) return
        if (CreateProfileContainer.visibility == View.VISIBLE) return

        settingUi.visibility = View.VISIBLE
        settingUi.alpha = 0f
        settingUi.translationY = dpToPx(12).toFloat()
        settingUi.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction { gDriveBackup.requestFocus() }
            .start()
    }

    private fun hideSettingsPanel(restoreFocus: Boolean) {
        if (settingUi.visibility != View.VISIBLE) return

        settingUi.animate()
            .alpha(0f)
            .translationY(dpToPx(12).toFloat())
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                settingUi.visibility = View.GONE
                settingUi.translationY = dpToPx(12).toFloat()
                if (restoreFocus) {
                    settingButton.requestFocus()
                }
            }
            .start()
    }

    private fun showCreateProfilePanel() {
        if (CreateProfileContainer.visibility == View.VISIBLE) return

        hideSettingsPanel(restoreFocus = false)
        CreateProfileContainer.visibility = View.VISIBLE
        CreateProfileContainer.alpha = 0f
        createProfileCard.alpha = 0f
        createProfileCard.scaleX = 0.96f
        createProfileCard.scaleY = 0.96f

        CreateProfileContainer.animate()
            .alpha(1f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .start()

        createProfileCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction { usernameInput.requestFocus() }
            .start()
    }

    private fun hideCreateProfilePanel(restoreFocus: Boolean) {
        if (CreateProfileContainer.visibility != View.VISIBLE) return

        CreateProfileContainer.animate()
            .alpha(0f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .start()

        createProfileCard.animate()
            .alpha(0f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(PANEL_ANIMATION_DURATION_MS)
            .withEndAction {
                CreateProfileContainer.visibility = View.GONE
                createProfileCard.alpha = 1f
                createProfileCard.scaleX = 1f
                createProfileCard.scaleY = 1f
                if (restoreFocus) {
                    requestProfileRailFocusIfAppropriate()
                }
            }
            .start()
    }

    private fun backupLibrary() {
        lifecycleScope.launch {
            gDriveBackup.isEnabled = false
            gDriveBackup.alpha = 0.55f
            Toast.makeText(this@Login_Page, "Backing up library...", Toast.LENGTH_SHORT).show()

            try {
                withContext(Dispatchers.IO) {
                    GlobalUtils.autoBackupDatabase(this@Login_Page)
                }
                Toast.makeText(this@Login_Page, "Backup finished", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Login_Page", "Backup failed: ${e.message}", e)
                Toast.makeText(this@Login_Page, "Backup failed", Toast.LENGTH_SHORT).show()
            } finally {
                gDriveBackup.isEnabled = true
                gDriveBackup.alpha = 1f
            }
        }
    }

    private fun resetCreateProfileForm() {
        usernameInput.text.clear()
        usernamePinInput.text.clear()
        selectedAvatar = ""
        avatarAdapter.clearSelection()
        updateSelectedAvatarPreview()
        updateCreateButtonState()
    }

    private fun updateSelectedAvatarPreview() {
        if (selectedAvatar.isBlank()) {
            selectedAvatarPreview.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18))
            selectedAvatarPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
            selectedAvatarPreview.setImageResource(R.drawable.ic_person)
            selectedAvatarPreview.setColorFilter(Color.parseColor("#FFF8EE"))
            selectedAvatarLabel.text = "Choose an avatar"
            return
        }

        selectedAvatarPreview.setPadding(0, 0, 0, 0)
        selectedAvatarPreview.scaleType = ImageView.ScaleType.CENTER_CROP
        selectedAvatarPreview.clearColorFilter()
        Glide.with(this)
            .load("file:///android_asset/$selectedAvatar")
            .centerCrop()
            .into(selectedAvatarPreview)
        selectedAvatarLabel.text = "Avatar selected"
    }

    private fun updateCreateButtonState() {
        val enabled = usernameInput.text.toString().trim().isNotEmpty() &&
            usernamePinInput.text.toString().trim().isNotEmpty() &&
            selectedAvatar.isNotBlank()

        createProfileBtn.isEnabled = enabled
        createProfileBtn.alpha = if (enabled) 1f else 0.6f
    }

    private fun requestProfileRailFocusIfAppropriate() {
        if (CreateProfileContainer.visibility == View.VISIBLE || settingUi.visibility == View.VISIBLE) {
            return
        }

        profileRecyclerView.post {
            val currentFocus = currentFocus
            if (currentFocus != null && currentFocus.isShown) {
                return@post
            }
            val firstItem = profileRecyclerView.findViewHolderForAdapterPosition(0)?.itemView
            firstItem?.requestFocus()
        }
    }

    private fun attachFocusLift(vararg views: View) {
        val lift = dpToPx(4).toFloat()
        views.forEach { target ->
            target.setOnFocusChangeListener { view, hasFocus ->
                view.animate()
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
                when {
                    CreateProfileContainer.visibility == View.VISIBLE -> {
                        resetCreateProfileForm()
                        hideCreateProfilePanel(restoreFocus = true)
                    }
                    settingUi.visibility == View.VISIBLE -> hideSettingsPanel(restoreFocus = true)
                }
            }
        })
    }
}
