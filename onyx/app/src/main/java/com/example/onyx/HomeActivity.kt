package com.example.onyx

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.NavAction

class HomeActivity : AppCompatActivity() {

    var showsFragment: ShowsFragment? = null
    var animeFragment: AnimeFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var watchingFragment: WatchingFragment? = null
    private var notificationFragment: notificationFragment? = null
    private var searchFragment: SearchFragment? = null

    private var activeFragment: Fragment? = null
    private var currentCoreTag: String = TAG_SHOWS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_home)

        val loadingImageView = findViewById<ImageView>(R.id.AnimationBG)
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.themeImage, typedValue, true)

        Glide.with(this)
            .asGif()
            .load(typedValue.resourceId)
            .into(loadingImageView)

        NavAction.setupSidebar(this)
        setupSidebarForFragments()
        restoreFragmentReferences()

        if (savedInstanceState == null) {
            val initialTag = getPersistedCoreTag()
            selectSidebarTag(initialTag)
            switchFragment(initialTag)
        } else {
            currentCoreTag = findVisibleCoreTag() ?: getPersistedCoreTag()
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible }
            selectSidebarTag(currentCoreTag)
        }
    }

    private fun setupSidebarForFragments() {
        requireSidebarView(TAG_SHOWS).setOnClickListener { navigateToCoreTag(TAG_SHOWS) }
        requireSidebarView(TAG_ANIME).setOnClickListener { navigateToCoreTag(TAG_ANIME) }
        requireSidebarView(TAG_SEARCH).setOnClickListener { navigateToCoreTag(TAG_SEARCH) }
        requireSidebarView(TAG_WATCHING).setOnClickListener { navigateToCoreTag(TAG_WATCHING) }
        requireSidebarView(TAG_FAVORITES).setOnClickListener { navigateToCoreTag(TAG_FAVORITES) }
        requireSidebarView(TAG_NOTIFICATIONS).setOnClickListener { navigateToCoreTag(TAG_NOTIFICATIONS) }
        requireSidebarView(TAG_PROFILE).setOnClickListener { navigateToCoreTag(TAG_PROFILE) }
    }

    private fun navigateToCoreTag(tag: String) {
        val normalizedTag = normalizeCoreTag(tag)
        selectSidebarTag(normalizedTag)
        switchFragment(normalizedTag)
    }

    private fun selectSidebarTag(tag: String) {
        val views = sidebarSelectionViews()
        val selectedView = views[normalizeCoreTag(tag)] ?: views.getValue(TAG_SHOWS)
        views.values.forEach { view ->
            view.isSelected = view === selectedView
        }
    }

    private fun sidebarSelectionViews(): Map<String, View> {
        return mapOf(
            TAG_SHOWS to requireSidebarView(TAG_SHOWS),
            TAG_ANIME to requireSidebarView(TAG_ANIME),
            TAG_SEARCH to requireSidebarView(TAG_SEARCH),
            TAG_WATCHING to requireSidebarView(TAG_WATCHING),
            TAG_FAVORITES to requireSidebarView(TAG_FAVORITES),
            TAG_NOTIFICATIONS to requireSidebarView(TAG_NOTIFICATIONS),
            TAG_PROFILE to requireSidebarView(TAG_PROFILE)
        )
    }

    private fun requireSidebarView(tag: String): View {
        val viewId = when (normalizeCoreTag(tag)) {
            TAG_SHOWS -> R.id.sidebarBtnShows
            TAG_ANIME -> R.id.sidebarBtnAnime
            TAG_SEARCH -> R.id.sidebarSearchBtn
            TAG_WATCHING -> R.id.sidebarWatchListBtn
            TAG_FAVORITES -> R.id.sidebarFavoritesBtn
            TAG_NOTIFICATIONS -> R.id.sidebarNotificationBtn
            TAG_PROFILE -> R.id.sidebarBtnProfile
            else -> R.id.sidebarBtnShows
        }
        return findViewById(viewId)
    }

    private fun switchFragment(tag: String) {
        val normalizedTag = normalizeCoreTag(tag)
        val fm = supportFragmentManager

        if (fm.isStateSaved) {
            return
        }

        val visibleOverlay = fm.fragments.firstOrNull {
            it.isAdded && it.isVisible && !isCoreNavigationTag(it.tag)
        }

        if (activeFragment?.tag == normalizedTag && visibleOverlay == null) {
            currentCoreTag = normalizedTag
            persistCoreTag(normalizedTag)
            return
        }

        val transaction = fm.beginTransaction().setReorderingAllowed(true)
        var targetFragment = fm.findFragmentByTag(normalizedTag)

        fm.fragments.filter { it.isAdded }.forEach { fragment ->
            when {
                fragment === targetFragment -> Unit
                isCoreNavigationTag(fragment.tag) -> {
                    transaction.hide(fragment)
                    transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                }
                else -> transaction.remove(fragment)
            }
        }

        if (targetFragment == null) {
            targetFragment = createCoreFragment(normalizedTag)
            transaction.add(R.id.fragmentContainer, targetFragment!!, normalizedTag)
        } else {
            transaction.show(targetFragment!!)
        }

        transaction.setMaxLifecycle(targetFragment!!, Lifecycle.State.RESUMED)

        activeFragment = targetFragment
        currentCoreTag = normalizedTag
        persistCoreTag(normalizedTag)
        transaction.commit()
    }

    fun navigateToFragment(fragment: Fragment, args: Bundle? = null) {
        val coreTag = coreTagForFragment(fragment)
        if (coreTag != null) {
            navigateToCoreTag(coreTag)
            return
        }

        args?.let { fragment.arguments = Bundle(it) }

        val fm = supportFragmentManager
        if (fm.isStateSaved) {
            return
        }

        val transaction = fm.beginTransaction().setReorderingAllowed(true)

        fm.fragments.filter { it.isAdded }.forEach { existingFragment ->
            when {
                isCoreNavigationTag(existingFragment.tag) -> {
                    transaction.hide(existingFragment)
                    transaction.setMaxLifecycle(existingFragment, Lifecycle.State.STARTED)
                }
                else -> transaction.remove(existingFragment)
            }
        }

        transaction.add(R.id.fragmentContainer, fragment, buildOverlayTag(fragment))
        activeFragment = fragment
        transaction.commit()
    }

    fun navigateToExistingAndDestroyCurrent(existingFragment: Fragment, fragmentToDestroy: Fragment) {
        val coreTag = coreTagForFragment(existingFragment)
        if (coreTag != null) {
            val fm = supportFragmentManager
            if (!fm.isStateSaved) {
                fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .remove(fragmentToDestroy)
                    .commit()
            }
            navigateToCoreTag(coreTag)
            return
        }

        val fm = supportFragmentManager
        if (fm.isStateSaved) {
            return
        }

        val transaction = fm.beginTransaction().setReorderingAllowed(true)
        transaction.remove(fragmentToDestroy)

        if (existingFragment.isAdded) {
            transaction.show(existingFragment)
            transaction.setMaxLifecycle(existingFragment, Lifecycle.State.RESUMED)
        } else {
            transaction.add(
                R.id.fragmentContainer,
                existingFragment,
                existingFragment.tag ?: buildOverlayTag(existingFragment)
            )
        }

        activeFragment = existingFragment
        transaction.commit()
    }

    fun returnToCoreNavigation(fragmentToDestroy: Fragment) {
        val fm = supportFragmentManager
        if (fm.isStateSaved) {
            return
        }

        if (fragmentToDestroy.isAdded) {
            fm.beginTransaction()
                .setReorderingAllowed(true)
                .remove(fragmentToDestroy)
                .commit()
        }

        navigateToCoreTag(currentCoreTag)
    }

    private fun restoreFragmentReferences() {
        val fm = supportFragmentManager
        showsFragment = fm.findFragmentByTag(TAG_SHOWS) as? ShowsFragment
        animeFragment = fm.findFragmentByTag(TAG_ANIME) as? AnimeFragment
        profileFragment = fm.findFragmentByTag(TAG_PROFILE) as? ProfileFragment
        watchingFragment = fm.findFragmentByTag(TAG_WATCHING) as? WatchingFragment
        notificationFragment = fm.findFragmentByTag(TAG_NOTIFICATIONS) as? notificationFragment
        searchFragment = fm.findFragmentByTag(TAG_SEARCH) as? SearchFragment
    }

    private fun createCoreFragment(tag: String): Fragment {
        val fragment = when (tag) {
            TAG_SHOWS -> ShowsFragment()
            TAG_ANIME -> AnimeFragment()
            TAG_PROFILE -> ProfileFragment()
            TAG_WATCHING -> WatchingFragment()
            TAG_FAVORITES -> FavoritesFragment()
            TAG_NOTIFICATIONS -> notificationFragment()
            TAG_SEARCH -> SearchFragment()
            else -> ShowsFragment()
        }

        when (tag) {
            TAG_SHOWS -> showsFragment = fragment as ShowsFragment
            TAG_ANIME -> animeFragment = fragment as AnimeFragment
            TAG_PROFILE -> profileFragment = fragment as ProfileFragment
            TAG_WATCHING -> watchingFragment = fragment as WatchingFragment
            TAG_NOTIFICATIONS -> notificationFragment = fragment as notificationFragment
            TAG_SEARCH -> searchFragment = fragment as SearchFragment
        }

        return fragment
    }

    private fun buildOverlayTag(fragment: Fragment): String {
        return fragment::class.java.name
    }

    private fun coreTagForFragment(fragment: Fragment): String? {
        return when (fragment) {
            is ShowsFragment -> TAG_SHOWS
            is AnimeFragment -> TAG_ANIME
            is ProfileFragment -> TAG_PROFILE
            is WatchingFragment -> TAG_WATCHING
            is FavoritesFragment -> TAG_FAVORITES
            is notificationFragment -> TAG_NOTIFICATIONS
            is SearchFragment -> TAG_SEARCH
            else -> null
        }
    }

    private fun normalizeCoreTag(tag: String?): String {
        return if (tag != null && CORE_NAVIGATION_TAGS.contains(tag)) tag else TAG_SHOWS
    }

    private fun isCoreNavigationTag(tag: String?): Boolean {
        return tag != null && CORE_NAVIGATION_TAGS.contains(tag)
    }

    private fun persistCoreTag(tag: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(PREF_LAST_FRAGMENT, tag)
            .apply()
    }

    private fun getPersistedCoreTag(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return normalizeCoreTag(prefs.getString(PREF_LAST_FRAGMENT, TAG_SHOWS))
    }

    private fun findVisibleCoreTag(): String? {
        return supportFragmentManager.fragments.firstOrNull {
            it.isVisible && isCoreNavigationTag(it.tag)
        }?.tag
    }

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val PREF_LAST_FRAGMENT = "last_fragment"

        private const val TAG_SHOWS = "shows"
        private const val TAG_ANIME = "anime"
        private const val TAG_PROFILE = "profile"
        private const val TAG_WATCHING = "watching"
        private const val TAG_FAVORITES = "favorites"
        private const val TAG_NOTIFICATIONS = "notifications"
        private const val TAG_SEARCH = "search"

        private val CORE_NAVIGATION_TAGS = setOf(
            TAG_SHOWS,
            TAG_ANIME,
            TAG_PROFILE,
            TAG_WATCHING,
            TAG_FAVORITES,
            TAG_NOTIFICATIONS,
            TAG_SEARCH
        )
    }
}
