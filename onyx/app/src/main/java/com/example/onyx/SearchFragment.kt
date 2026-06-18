package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.example.onyx.databinding.FragmentSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import androidx.fragment.app.Fragment
import com.example.onyx.OnyxClasses.GridAdapter2
import com.example.onyx.OnyxClasses.MovieItem
import com.google.android.material.button.MaterialButtonToggleGroup
import org.json.JSONObject


class SearchFragment :  Fragment(R.layout.fragment_search) {

    private lateinit var animeSearchAdapter: AnimeSearchAdapter
    private lateinit var animeSearchRecyclerView: RecyclerView

    private lateinit var showSearchAdapter: GridAdapter2
    private lateinit var showSearchRecyclerView: RecyclerView

    private var urlHome = BuildConfig.A_K

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




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
                            animeSearchRecyclerView.scrollToPosition(nextRowFirstPos)
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

    }



    private  fun searchAnimeFetch(searchTerm:String){
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)

        animeSearchAdapter.clearItems()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(1) { attempt ->
                try {

                    val url = "$urlHome/api/v2/anime/search?q=$searchTerm&page=1"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.e("ANIME_STATUS search", response.toString())

                    val jsonObject = org.json.JSONObject(response)
                    Log.e("ANIME_STATUS search", jsonObject.toString())
                    val dataFetch = jsonObject.getJSONObject("data")
                    val  searchData = dataFetch.getJSONArray("animes")
                    Log.e("ANIME_STATUS SEARCH-R", dataFetch.toString())
                    var searchDataItmes = mutableListOf<AnimeSearchItem>()


                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        if(searchData.length() == 0) {
                            searchTextDisplay.text = "No Results Found"
                        }else{
                            searchTextDisplay.text = "Search results for: $searchTerm"
                        }
                    }



                    for (i in 0 until searchData.length()) {
                        val item = searchData.getJSONObject(i)
                        val title = item.getString("name")
                        val imageUrl = item.getString("poster")
                        val id = item.getString("id")
                        val type = item.getString("type")
                        val sub = item.getJSONObject("episodes").optString("sub", "")
                        val dub = item.getJSONObject("episodes").optString("dub", "")



                        val searchItem = AnimeSearchItem(
                            id,
                            title,
                            imageUrl,
                            type,
                            sub,
                            dub,
                        )

                        withContext(Dispatchers.Main) {
                            if (!isAdded || view == null) return@withContext
                            animeSearchAdapter.addItem(searchItem)
                        }

                    }


                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS S-Error", "Error fetching data", e)
                    return@launch
                }
            }
        }
    }


    private fun searchShowsFetch(searchTerm:String) {
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {

                    Log.e("SEARCH RESULTS", searchTerm)

                    // --- Background work (network request) ---
                    val url =
                        "https://api.themoviedb.org/3/search/multi?include_adult=false&query=$searchTerm"
                    val connection = URL(url).openConnection() as HttpURLConnection
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

                    //Log.e("SEARCH RESULTS", moviesArray.toString())


                    withContext(Dispatchers.Main)  {
                        searchTextDisplay.text = "Search Results for: $searchTerm (${moviesArray.length()})"
                        showSearchAdapter.clearItems()
                    }

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


                        //movies.add(MovieItem(title, imgUrl, id, type))

                        val movieItem = MovieItem(
                            title = title,
                            imageUrl = imgUrl,
                            imdbCode = id,
                            type = mediaType,
                            year = date,
                            rating = voteAverage,
                            runtime = info
                        )

                        withContext(Dispatchers.Main) {
                            showSearchAdapter.addItem(movieItem)
                        }

                    }


                    return@launch
                } catch (e: Exception) {
                    Log.e("SEARCH RESULTS ERROR", "S ERROR", e)
                    delay(10_000)
                }
            }
        }
    }



    private fun setupSearchUi() {

        val searchInput = requireView().findViewById<EditText>(R.id.searchView)
        val keyboardLayout = requireView().findViewById<LinearLayout>(R.id.keyboard_layout)
        val toggleGroup = requireView().findViewById<MaterialButtonToggleGroup>(R.id.searchCategoryToggle)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnAnime -> searchInput.hint = "Search for anime..."
                    R.id.btnNormalShows -> searchInput.hint = "Search for shows..."

                }
            }
        }

        val keyboardManager = CustomKeyboardManager(
            requireActivity(),
            searchInput,
            keyboardLayout,
            object : OnSearchListener {
                override fun EnterActionTrigger(query: String) {
                    val searchTerm = query.trim()
                    if (searchTerm.isNotEmpty()) {
                        when (toggleGroup.checkedButtonId) {
                            R.id.btnAnime -> {
                                searchAnimeFetch(searchTerm)
                            }
                            R.id.btnNormalShows -> {
                                searchShowsFetch(searchTerm)
                            }
                            else -> {
                                // Fallback just in case (defaults to Anime)
                            }
                        }
                    }
                }
            }
        )
        keyboardManager.showKeyboard()
        //keyboardManager.hideKeyboard()
        keyboardManager.isKeyboardVisible()
    }

}