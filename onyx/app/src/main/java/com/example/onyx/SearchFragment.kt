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


class SearchFragment :  Fragment(R.layout.fragment_search) {

    private lateinit var searchAdapter: AnimeSearchAdapter
    private lateinit var searchRecyclerView: RecyclerView

    private var urlHome = BuildConfig.A_K

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        val tvSpacing = (10 * resources.displayMetrics.density).toInt()



        //-----------------------------------------------------------------------------------------

        searchRecyclerView = requireView().findViewById(R.id.SearchRecycler)
        searchRecyclerView.layoutManager =  object : GridLayoutManager(requireActivity(), 3){

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
                            searchRecyclerView.scrollToPosition(nextRowFirstPos)
                            focused
                        }
                    }
                }

                return super.onInterceptFocusSearch(focused, direction)
            }

        }
        searchAdapter  = AnimeSearchAdapter(mutableListOf(), R.layout.anime_airing_item)
        searchRecyclerView.adapter = searchAdapter
        searchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))


        //------------------------------------------------------------------------------------------

        setupSearchUi()

    }



    private  fun searchAnimeFetch(searchTerm:String){
        val searchTextDisplay = requireView().findViewById<TextView>(R.id.searchTextDisplay)

        searchAdapter.clearItems()
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
                            searchAdapter.addItem(searchItem)
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



    private fun setupSearchUi() {

        val searchInput = requireView().findViewById<EditText>(R.id.AnimeSearchInput)
        val keyboardLayout = requireView().findViewById<LinearLayout>(R.id.keyboard_layout)
        val keyboardManager = CustomKeyboardManager(
            requireActivity(),
            searchInput,
            keyboardLayout,
            object : OnSearchListener {
                override fun EnterActionTrigger(query: String) {
                    val searchTerm = query.trim()
                    if (searchTerm.isNotEmpty()) {
                        searchAnimeFetch(searchTerm)
                    }
                }
            }
        )
        keyboardManager.showKeyboard()
        //keyboardManager.hideKeyboard()
        keyboardManager.isKeyboardVisible()
    }

}