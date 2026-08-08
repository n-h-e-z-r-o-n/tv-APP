package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.onyx.OnyxClasses.AnimeSearchItem
import com.example.onyx.OnyxClasses.CustomKeyboardManager
import com.example.onyx.OnyxClasses.OnSearchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onyx.OnyxClasses.AnimeSearchAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URLEncoder
import androidx.fragment.app.Fragment
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.OnyxClasses.GridAdapter2
import com.example.onyx.OnyxClasses.MovieItem
import com.example.onyx.OnyxObjects.LoadingAnimation
import com.google.android.material.button.MaterialButtonToggleGroup
import org.json.JSONObject
import java.nio.charset.StandardCharsets


class SearchFragment :  Fragment(R.layout.fragment_search) {

    private lateinit var animeSearchAdapter: AnimeSearchAdapter
    private lateinit var animeSearchRecyclerView: RecyclerView

    private lateinit var showSearchAdapter: GridAdapter2
    private lateinit var showSearchRecyclerView: RecyclerView

    private var urlHome = BuildConfig.A_K
    private var speechRecognizer: android.speech.SpeechRecognizer? = null

    private lateinit var fetchAnimeAPI: AnimeApi

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening()
        } else {
            Log.e("VoiceSearch", "Permission denied")
        }
    }

    private var lastFocusedView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchAnimeAPI = AnimeApi(requireActivity())


        view.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && view.findViewById<View>(newFocus.id) != null) {
                lastFocusedView = newFocus
            }
        }

        val tvSpacing = (4 * resources.displayMetrics.density).toInt()

        //-----------------------------------------------------------------------------------------

        animeSearchRecyclerView = requireView().findViewById(R.id.SearchRecyclerAnime)
        animeSearchRecyclerView.layoutManager =  object : GridLayoutManager(requireActivity(), 4){

            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                val currentPosition = getPosition(focused)
                if (currentPosition == RecyclerView.NO_POSITION) return null

                if (direction == View.FOCUS_RIGHT) {
                    val span = spanCount
                    val isLastColumn = (currentPosition + 1) % span == 0
                    val nextRowFirstPos = currentPosition + 1

                    if (isLastColumn) {
                        if (nextRowFirstPos >= itemCount) {
                            // Block focus at end of grid
                            return focused
                        }

                        // Ensure view exists (scroll if needed)
                        val nextView = findViewByPosition(nextRowFirstPos)
                        return nextView ?: run {
                            showSearchRecyclerView.scrollToPosition(nextRowFirstPos)
                            focused
                        }
                    }
                }

                return super.onInterceptFocusSearch(focused, direction)
            }

        }
        animeSearchAdapter  = AnimeSearchAdapter(mutableListOf(), R.layout.anime_airing_item)
        animeSearchRecyclerView.adapter = animeSearchAdapter
        animeSearchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))


        //------------------------------------------------------------------------------------------


        showSearchRecyclerView = requireView().findViewById(R.id.SearchRecyclerShows)
        showSearchRecyclerView.layoutManager =  object : GridLayoutManager(requireActivity(), 4){

            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                val currentPosition = getPosition(focused)
                if (currentPosition == RecyclerView.NO_POSITION) return null

                if (direction == View.FOCUS_RIGHT) {
                    val span = spanCount
                    val isLastColumn = (currentPosition + 1) % span == 0
                    val nextRowFirstPos = currentPosition + 1

                    if (isLastColumn) {
                        if (nextRowFirstPos >= itemCount) {
                            // Block focus at end of grid
                            return focused
                        }

                        // Ensure view exists (scroll if needed)
                        val nextView = findViewByPosition(nextRowFirstPos)
                        return nextView ?: run {
                            animeSearchRecyclerView.scrollToPosition(nextRowFirstPos)
                            focused
                        }
                    }
                }

                return super.onInterceptFocusSearch(focused, direction)
            }

        }
        showSearchAdapter  = GridAdapter2(mutableListOf(), R.layout.item_search)
        showSearchRecyclerView.adapter = showSearchAdapter
        val Spacing = (10 * resources.displayMetrics.density).toInt()
        showSearchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))

        setupSearchUi()
        loadRecentSearches()

    }

    override fun onResume() {
        super.onResume()
        requireView().post {
            if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                lastFocusedView!!.requestFocus()
            } else {
                requireView().findViewById<EditText>(R.id.searchView)?.requestFocus()
            }
        }
    }


    private fun executeSearch(query: String) {
        val searchTerm = query.trim()
        if (searchTerm.isEmpty()) return

        val toggleGroup = requireView().findViewById<MaterialButtonToggleGroup>(R.id.searchCategoryToggle)
        when (toggleGroup.checkedButtonId) {
            R.id.btnAnime -> searchAnimeFetch(searchTerm)
            R.id.btnNormalShows -> searchShowsFetch(searchTerm)
        }
    }

    private fun updateSearchHint() {
        val searchInput = requireView().findViewById<EditText>(R.id.searchView)
        val toggleGroup = requireView().findViewById<MaterialButtonToggleGroup>(R.id.searchCategoryToggle)
        searchInput.hint = when (toggleGroup.checkedButtonId) {
            R.id.btnNormalShows -> "Search for shows..."
            else -> "Search for anime..."
        }
    }

    private  fun searchAnimeFetch(searchTerm:String){
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)
        val progressBar = requireView().findViewById<android.widget.ProgressBar>(R.id.searchProgressBar)
        val encodedSearchTerm = URLEncoder.encode(searchTerm.trim(), StandardCharsets.UTF_8.toString())
        
        saveRecentSearch(searchTerm)
        requireView().findViewById<LinearLayout>(R.id.recentSearchesLayout).visibility = View.GONE
        searchTextDisplay.visibility = View.VISIBLE

        animeSearchAdapter.clearItems()
        showSearchAdapter.clearItems()
        searchTextDisplay.text = "Searching..."
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(1) { attempt ->
                var connection: HttpURLConnection? = null
                try {

                    val jsonObject = fetchAnimeAPI.animeSearch(encodedSearchTerm)

                    /*

                    val url = "$urlHome/api/v2/anime/search?q=$encodedSearchTerm&page=1"
                    connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.e("ANIME_STATUS search", response.toString())

                    val jsonObject = org.json.JSONObject(response)

                     */

                    if (jsonObject == null) {
                        return@launch
                    }

                    Log.e("ANIME_STATUS search", jsonObject.toString())
                    val dataFetch = jsonObject.getJSONObject("data")
                    val  searchData = dataFetch.getJSONArray("animes")
                    Log.e("ANIME_STATUS SEARCH-R", dataFetch.toString())

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        progressBar.visibility = View.GONE
                        if(searchData.length() == 0) {
                            searchTextDisplay.text = "No Results Found for: $searchTerm"
                        }else{
                            searchTextDisplay.text = "Search results for: $searchTerm"
                        }
                    }

                    val searchDataItems = mutableListOf<AnimeSearchItem>()
                    for (i in 0 until searchData.length()) {
                        val item = searchData.getJSONObject(i)
                        val title = item.getString("name")
                        val imageUrl = item.getString("poster")
                        val id = item.getString("id")
                        val type = item.getString("type")
                        val sub = item.getJSONObject("episodes").optString("sub", "")
                        val dub = item.getJSONObject("episodes").optString("dub", "")

                        searchDataItems.add(AnimeSearchItem(
                            id,
                            title,
                            imageUrl,
                            type,
                            sub,
                            dub
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        searchDataItems.forEach { animeSearchAdapter.addItem(it) }
                        runLayoutAnimation(animeSearchRecyclerView)
                    }

                    return@launch
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded && view != null) {
                            progressBar.visibility = View.GONE
                            searchTextDisplay.text = "Error occurred during search"
                        }
                    }
                    Log.e("ANIME_STATUS S-Error", "Error fetching data", e)
                    return@launch
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }


    private fun searchShowsFetch(searchTerm: String){
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)
        val progressBar = requireView().findViewById<android.widget.ProgressBar>(R.id.searchProgressBar)
        val encodedSearchTerm = URLEncoder.encode(searchTerm.trim(), StandardCharsets.UTF_8.toString())
        
        saveRecentSearch(searchTerm)
        requireView().findViewById<LinearLayout>(R.id.recentSearchesLayout).visibility = View.GONE
        searchTextDisplay.visibility = View.VISIBLE

        animeSearchAdapter.clearItems()
        showSearchAdapter.clearItems()
        searchTextDisplay.text = "Searching..."
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                var connection: HttpURLConnection? = null
                try {
                    Log.e("SEARCH RESULTS", searchTerm)

                    // --- Background work (network request) ---
                    val url =
                        "https://api.themoviedb.org/3/search/multi?include_adult=false&query=$encodedSearchTerm"
                    connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    Log.e("SEARCH RESULTS", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    withContext(Dispatchers.Main)  {
                        if (!isAdded || view == null) return@withContext
                        progressBar.visibility = View.GONE
                        if (moviesArray.length() == 0) {
                            searchTextDisplay.text = "No Results Found for: $searchTerm"
                        } else {
                            searchTextDisplay.text = "Search Results for: $searchTerm (${moviesArray.length()})"
                        }
                    }

                    val showDataItems = mutableListOf<MovieItem>()
                    for (i in 0 until moviesArray.length()) {
                        val current = moviesArray.getJSONObject(i)
                        current.remove("overview")
                        current.remove("genre_ids")
                        current.remove("popularity")
                        current.remove("video")

                        val mediaType = current.optString("media_type", "movie")

                        var title = "Unknown"
                        var info = ""
                        var date = ""
                        var voteAverage = ""
                        var imgUrl = ""
                        var poster = ""

                        Log.e("SEARCH RESULTS $i", current.toString())
                        when (mediaType) {
                            "person" -> {
                                title = current.optString(
                                    "name",
                                    current.optString("original_name", "Unknown")
                                )
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "profile_path",
                                    ""
                                )
                                poster = current.optString("profile_path", "null")
                                info = current.optString("known_for_department", "")
                                voteAverage = ""
                            }

                            "tv" -> {
                                title = current.optString(
                                    "name",
                                    current.optString("original_name", "Unknown")
                                )
                                date =
                                    current.optString("first_air_date")
                                        .takeIf { it.isNotEmpty() }
                                        ?.substring(0, 4) ?: ""
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "poster_path",
                                    ""
                                )
                                poster = current.optString("poster_path", "null")
                                info = ""
                                voteAverage =
                                    current.optDouble("vote_average", 0.0).toInt()
                                        .toString() + " ★"
                            }

                            "movie" -> {
                                title = current.optString("original_title", "Unknown")
                                date =
                                    current.optString("release_date").takeIf { it.isNotEmpty() }
                                        ?.substring(0, 4) ?: ""
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "poster_path",
                                    ""
                                )
                                poster = current.optString("poster_path", "null")
                                info = "" // TODO: runtime if available
                                voteAverage = current.optDouble("vote_average", 0.0).toInt()
                                    .toString() + " ★"
                            }
                        }

                        if (poster.isBlank() || poster.endsWith("null")) continue

                        val id = current.getString("id")

                        showDataItems.add(MovieItem(
                            title = title,
                            imageUrl = imgUrl,
                            imdbCode = id,
                            type = mediaType,
                            year = date,
                            rating = voteAverage,
                            runtime = info
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        showDataItems.forEach { showSearchAdapter.addItem(it) }
                        runLayoutAnimation(showSearchRecyclerView)
                    }

                    return@launch
                } catch (e: Exception) {
                    Log.e("SEARCH RESULTS ERROR", "S ERROR", e)
                    if (attempt == 2) { // Last attempt
                        withContext(Dispatchers.Main) {
                            if (isAdded && view != null) {
                                progressBar.visibility = View.GONE
                                searchTextDisplay.text = "Error occurred during search"
                            }
                        }
                    }
                    delay(3000)
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupSearchUi() {

        val searchInput = requireView().findViewById<EditText>(R.id.searchView)
        val keyboardLayout = requireView().findViewById<LinearLayout>(R.id.keyboard_layout)
        val toggleGroup = requireView().findViewById<MaterialButtonToggleGroup>(R.id.searchCategoryToggle)

        val prefs = requireActivity().getSharedPreferences("SearchPrefs", android.content.Context.MODE_PRIVATE)
        val savedCategoryId = prefs.getInt("category", R.id.btnAnime)
        toggleGroup.check(savedCategoryId)
        updateSearchHint()

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                prefs.edit().putInt("category", checkedId).apply()
                updateSearchHint()
            }
        }

        // You can comment out this function call to disable voice search
        setupVoiceSearch()

        val keyboardManager = CustomKeyboardManager(
            requireActivity(),
            searchInput,
            keyboardLayout,
            object : OnSearchListener {
                override fun EnterActionTrigger(query: String) {
                    executeSearch(query)
                }
            }
        )
        keyboardManager.showKeyboard()
        //keyboardManager.hideKeyboard()
        keyboardManager.isKeyboardVisible()
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_slide_up)
        recyclerView.layoutAnimation = controller
        recyclerView.adapter?.notifyDataSetChanged()
        recyclerView.scheduleLayoutAnimation()
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////


    private fun setupVoiceSearch() {
        val voiceSearchBtn = requireView().findViewById<android.widget.ImageButton>(R.id.btnVoiceSearch)
        voiceSearchBtn.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }


    private fun startListening() {
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Log.e("VoiceSearch", "Speech recognition not available")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(requireContext())
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                val searchInput = requireView().findViewById<EditText>(R.id.searchView)
                searchInput.hint = "Listening..."
                requireView().findViewById<android.widget.ImageButton>(R.id.btnVoiceSearch).setColorFilter(android.graphics.Color.RED)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                updateSearchHint()
                requireView().findViewById<android.widget.ImageButton>(R.id.btnVoiceSearch).clearColorFilter()
            }
            override fun onError(error: Int) {
                updateSearchHint()
                requireView().findViewById<android.widget.ImageButton>(R.id.btnVoiceSearch).clearColorFilter()
                Log.e("VoiceSearch", "Error code: $error")
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val spokenText = data[0]
                    val searchInput = requireView().findViewById<EditText>(R.id.searchView)
                    searchInput.setText(spokenText)

                    executeSearch(spokenText)
                }
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val data = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val searchInput = requireView().findViewById<EditText>(R.id.searchView)
                    searchInput.setText(data[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun saveRecentSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return

        val prefs = requireActivity().getSharedPreferences("SearchPrefs", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "") ?: ""
        val historyList = historyString.split(",").filter { it.isNotBlank() }.toMutableList()

        historyList.removeAll { it.equals(normalizedQuery, ignoreCase = true) }
        historyList.add(0, normalizedQuery)

        if (historyList.size > 10) {
            historyList.removeAt(historyList.size - 1)
        }

        prefs.edit().putString("history", historyList.joinToString(",")).apply()
    }

    private fun loadRecentSearches() {
        val prefs = requireActivity().getSharedPreferences("SearchPrefs", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "") ?: ""
        val historyList = historyString.split(",").filter { it.isNotBlank() }
        
        val recentLayout = requireView().findViewById<LinearLayout>(R.id.recentSearchesLayout)
        val container = requireView().findViewById<LinearLayout>(R.id.recentSearchesContainer)
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)
        
        if (historyList.isEmpty()) {
            recentLayout.visibility = View.GONE
            searchTextDisplay.visibility = View.GONE
            return
        }

        recentLayout.visibility = View.VISIBLE
        searchTextDisplay.visibility = View.GONE
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        val rootView = requireView()

        for (term in historyList) {
            val recentSearchView = inflater.inflate(R.layout.item_recent_search, container, false)
            val titleView = recentSearchView.findViewById<TextView>(R.id.searchTitle)

            titleView.text = term

            
            recentSearchView.setOnClickListener {
                val searchInput = rootView.findViewById<EditText>(R.id.searchView)
                searchInput.setText(term)

                val toggleGroup = rootView.findViewById<MaterialButtonToggleGroup>(R.id.searchCategoryToggle)
                when (toggleGroup.checkedButtonId) {
                    R.id.btnAnime -> searchAnimeFetch(term)
                    R.id.btnNormalShows -> searchShowsFetch(term)
                    else -> {}
                }
            }
            container.addView(recentSearchView)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevent memory leaks by clearing view references
        if (::animeSearchRecyclerView.isInitialized) {
            animeSearchRecyclerView.adapter = null
        }
        if (::showSearchRecyclerView.isInitialized) {
            showSearchRecyclerView.adapter = null
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

}
