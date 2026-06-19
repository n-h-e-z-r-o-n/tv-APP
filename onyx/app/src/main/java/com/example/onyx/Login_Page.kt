package com.example.onyx

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL



class Login_Page : AppCompatActivity() {

    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var avatarRecycler: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger
    private var activeSub = false
    private val profiles = mutableListOf<profileItem>()
    private var selectedAvatar: String = ""
    private lateinit var profileContainer: FrameLayout
    private lateinit var settingButton: ImageView
    private lateinit var settingUi: FrameLayout
    private lateinit var gDriveBackup: TextView
    private lateinit var exitApp: TextView
    private lateinit var exitSetting: TextView
    private lateinit var CreateProfileContainer: FrameLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_login_page)

        ////////////////////////////////////////////////////////////////////////////////////////////
        val loadingImageView = findViewById<ImageView>(R.id.backgroundImgContainer)

        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.themeImage, typedValue, true)

        Glide.with(this)
            .asGif()
            .load( typedValue.resourceId)
            .into(loadingImageView)
        ///////////////////////////////////////////////////////////////////////////////////////////

        settingUi = findViewById(R.id.settingUi)
        settingButton = findViewById(R.id.settingButton)
        gDriveBackup = findViewById(R.id.gDriveBackup)
        exitApp = findViewById(R.id.exitApp)
        exitSetting = findViewById(R.id.exitSetting)

        settingButton.setOnClickListener {
            settingUi.visibility = View.VISIBLE
            gDriveBackup.requestFocus()
        }

        exitApp.setOnClickListener {
            GlobalUtils.exitApp(this)
        }

        gDriveBackup.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                GlobalUtils.autoBackupDatabase(this@Login_Page)
            }
        }

        exitSetting.setOnClickListener {
            settingUi.visibility = View.GONE
            settingButton.requestFocus()
        }






        db = AppDatabase(this)         // Initialize database
        sm = SessionManger(this)         // Initialize session manager
        //db.resetDatabase()
        activeSub = db.isSubscriptionActive()



        val userId = sm.getUserId()
        if (userId == -1) {
            InitializeWindgets()         // Setup
            loadProfiles()         // Load existing profiles
            setupBackPressedCallback()
        }else{
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun InitializeWindgets() {

        CreateProfileContainer = findViewById(R.id.CreateProfileContainer)
        profileContainer =  findViewById(R.id.profileContainer)

        val Spacing = (4 * resources.displayMetrics.density).toInt()

        ////////////////////////////////////////////////////////////////////////////////////////////

        profileRecyclerView = findViewById(R.id.profileRecycler)
        profileRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        val profileLayoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        profileRecyclerView.layoutManager = profileLayoutManager
        profileAdapter = ProfileAdapter(profiles, R.layout.item_account)
        profileRecyclerView.adapter = profileAdapter

        // Handle profile selection
        profileAdapter.onProfileSelected = { profile ->
            Log.e("ProfileAdapter", "Selected profile: $profile")
            var userId = profile.userid
            var userAvatar = profile.avatar
            if (userId == "CREATE") {
                CreateProfileContainer.visibility = LinearLayout.VISIBLE
                profileContainer.visibility = LinearLayout.GONE
            }else{
                if (activeSub) {
                    sm.saveUserId(userId.toInt())
                    sm.saveAvatar(userAvatar.toString())
                    startActivity(Intent(this, HomeActivity::class.java))

                } else {
                    lifecycleScope.launch {
                        val ipState =  GlobalUtils.getSavedCountryCode(this@Login_Page)
                        if (ipState.equals("KE", ignoreCase = true)) {
                            startActivity(Intent(this@Login_Page, PayWall::class.java))
                        } else {
                            startActivity(Intent(this@Login_Page, HomeActivity::class.java))
                        }
                    }
                }
            }

        }

        ////////////////////////////////////////////////////////////////////////////////////////////

        avatarRecycler = findViewById<RecyclerView>(R.id.avatarRecycler)
        val avatarPaths = mutableListOf<String>()         // Load avatar paths from assets
        try {
            val avatarFiles = assets.list("profile_avatars") ?: emptyArray()
            for (file in avatarFiles.sorted()) {
                avatarPaths.add("profile_avatars/$file")
            }
        } catch (e: Exception) {
            Log.e("Login_Page avatar", "Error loading avatar files: ${e.message}", e)
        }
        avatarRecycler.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        val avatarLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        avatarRecycler.layoutManager = avatarLayoutManager
        val avatarAdapter = AvatarAdapter(avatarPaths, R.layout.item_avatar)
        avatarRecycler.adapter = avatarAdapter
        avatarAdapter.onAvatarSelected = { avatarPath ->         // Handle avatar selection

            selectedAvatar = avatarPath
            Log.d("Login_Page avatar", "Selected avatar: $avatarPath")
        }
        setupProfileUI()
    }


    private fun loadProfiles() {

        lifecycleScope.launch {
        try {
            val users = withContext(Dispatchers.IO) {
                db.getUsers()
            }

            Log.e("Login_Page cursor", "cursor t-no: ${users.count}")

            if (users.count == 0) {
                withContext(Dispatchers.IO) {
                    db.setSubscription("MONTHLY", "")
                }
                activeSub = db.isSubscriptionActive()
            }


            profiles.clear() // Clear profiles first to avoid duplicates

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

            // Add "Create Profile" button as last item
            profiles.add(
                profileItem(
                    username = "Create +",
                    avatar = "",
                    userid = "CREATE"
                )
            )

            profileAdapter.notifyDataSetChanged()

        } catch (e: Exception) {
            Log.e("Login_Page", "Error loading profiles: ${e.message}", e)
            Toast.makeText(this@Login_Page, "Error loading profiles", Toast.LENGTH_SHORT).show()
        }
    }
    }


    override fun onResume() {
        super.onResume()
        // Reload profiles in case they were modified elsewhere
        loadProfiles()
    }

    private fun setupProfileUI() {

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val usernameGenderInput = findViewById<EditText>(R.id.usernameGenderInput)
        val usernamePinInput = findViewById<EditText>(R.id.usernamePinInput)
        val createProfileBtn = findViewById<TextView>(R.id.createProfileBtn)
        val cancelProfileBtn = findViewById<TextView>(R.id.cancelProfileBtn)

        createProfileBtn.setOnClickListener {
            CreateProfileContainer.visibility = LinearLayout.VISIBLE
            profileContainer.visibility = LinearLayout.GONE


            val username = usernameInput.text.toString().trim()
            val gender = "NAN"//usernameGenderInput.text.toString().trim()
            val pin = usernamePinInput.text.toString().trim()

            if (username.isEmpty()) {
                usernameInput.error = "Enter username"
                return@setOnClickListener
            }
            if (gender.isEmpty()) {
                usernameGenderInput.error = "Enter gender"
                return@setOnClickListener
            }
            if (pin.isEmpty()) {
                usernamePinInput.error = "Enter PIN"
                return@setOnClickListener
            }
            if (selectedAvatar.isEmpty()) {
                Toast.makeText(this, "Please select an avatar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val newUserId = db.addUser(username, gender, pin, selectedAvatar)

                if (newUserId != -1L) {
                    Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show()
                    Log.e("Login_Page", "New user ID: $newUserId")
                    
                    // Reset fields
                    usernameInput.text.clear()
                    usernameGenderInput.text.clear()
                    usernamePinInput.text.clear()
                    selectedAvatar = ""
                    
                    // containerS
                    CreateProfileContainer.visibility = View.GONE
                    profileContainer.visibility = View.VISIBLE
                    
                    // Reload profiles
                    loadProfiles()
                } else {
                    Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Login_Page", "Error creating profile: ${e.message}", e)
                Toast.makeText(this, "Error creating profile", Toast.LENGTH_SHORT).show()
            }
        }

        cancelProfileBtn.setOnClickListener {

            // Reset fields
            usernameInput.text.clear()
            usernameGenderInput.text.clear()
            usernamePinInput.text.clear()
            selectedAvatar = ""

            // Hide container
            profileContainer.visibility = LinearLayout.VISIBLE
            CreateProfileContainer.visibility = View.GONE
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
    }



}