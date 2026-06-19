package com.example.onyx

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxClasses.AiringAnimeItem
import com.example.onyx.OnyxClasses.AnimeAiringAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import kotlinx.coroutines.cancelChildren
import kotlin.collections.mutableListOf


import androidx.fragment.app.Fragment
import com.example.onyx.databinding.FragmentWatchAnimePageBinding
import com.google.android.material.card.MaterialCardView

class WatchAnimeFragment : Fragment(R.layout.fragment_watch_anime_page) {
    private var urlHome = BuildConfig.A_K
    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger
    private lateinit var  fetchAnime: AnimeApi
    private lateinit var  fetchTMDB: TMDBapi

    private  var userId = -1


    private lateinit var poster :String
    private lateinit var backdrop :String

    private lateinit var animeId :String

    private lateinit var mainSection: FrameLayout

    private lateinit var SeasonIMGArray: MutableList<String>

    private  var screenHeight = 0

    private var selectedSeasonView: FrameLayout? = null



    private var _binding: FragmentWatchAnimePageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 2. Inflate the layout using the generated binding class
        _binding = FragmentWatchAnimePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GlobalUtils.applyTheme(requireActivity())

        setupBackPressedCallback()

        LoadingAnimation.setup(requireContext(), view, R.raw.line_loading)
        LoadingAnimation.show(view)

        db = AppDatabase(requireContext())
        sm = SessionManger(requireContext())
        fetchAnime = AnimeApi(requireContext())
        fetchTMDB = TMDBapi(requireContext())

        userId = sm.getUserId()

        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels

        //------------------------------------------------------------------------------------------

        mainSection = requireView().findViewById(R.id.mainSection)

        //------------------------------------------------------------------------------------------

        val params = mainSection.layoutParams
        params.height = (screenHeight * 1).toInt()
        mainSection.layoutParams = params



        SeasonIMGArray = mutableListOf<String>()
        //------------------------------------------------------------------------------------------
        animeId = arguments?.getString("anime_code")?: ""

        /*
        Log.e("ANIME_Watch id", animeId)
        if (animeId.isNotEmpty()){
            getInfo(animeId)
        }else{
            getInfo("one-punch-man-season-3-19932")
        }
         */

        viewLifecycleOwner.lifecycleScope.launch {

            Log.e("ANIME_Watch id", animeId)
            if (animeId.isNotEmpty()){
                getInfo(animeId)
            }else{
                getInfo("one-punch-man-season-3-19932")
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()

        // Remove all handler callbacks
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }




    private suspend fun getInfo(Id: String){


        val jsonObject = fetchAnime.animeInfo(Id)

        Log.e("ANIME_Watch Data", jsonObject.toString())
        if (jsonObject==null){
            return
        }

        val data = jsonObject.getJSONObject("data")

        val id = data.getString("id")
        poster = data.getString("poster")
        backdrop = data.getString("backdrop")
        val logo = data.optString("tmdbLogoUrl", "")

        val anilistId = data.getString("anilistId")
        val malId = data.getString("malId")

        val name = data.getString("name")

        val japaneseName = data.getString("jname")
        val description = data.getString("description")
        val rating = data.getJSONObject("stats").getString("rating")
        val quality = data.getJSONObject("stats").getString("quality")
        val type = data.getJSONObject("stats").getString("type")
        val duration = data.getJSONObject("stats").getString("duration")
        val sub = data.getJSONObject("stats").getJSONObject("episodes").optString("sub", "")
        val dub = data.getJSONObject("stats").getJSONObject("episodes").optString("dub", "")
        val aired = data.getString("aired")

        val genresArray = data.getJSONArray("genres")
        var genre = ""
        for (i in 0 until genresArray.length()) {
            genre = genre +" ~ " +genresArray.getString(i)
        }
        val studio = data.getString("studios")


        val  seasons = data.getJSONArray("seasons")?: JSONArray()
        val  relatedAnimes = data.getJSONArray("relatedAnimes")?: JSONArray()
        val  recommendedAnime = data.getJSONArray("recommendedAnimes")?: JSONArray()

        val saveData  = jsonObject.getJSONObject("data")
        saveData.remove("recommendedAnimes")
        saveData.remove("relatedAnimes")
        saveData.remove("mostPopularAnimes")


        binding.watchTitle.text = name
        binding.watchRating.text = rating
        binding.watchRuntime.text = duration
        binding.watchType.text = type
        binding.watchQuality.text = quality
        binding.watchSub.text = sub
        binding.watchDub.text = dub
        binding.watchYear.text = GlobalUtils.formatDateString(aired)
        binding.watchOverview.text = description
        binding.watchGenres.text = genre

        val posterWidget = requireView().findViewById<ImageView>(R.id.WatchImage)
        Glide.with(posterWidget.context)
            .load(poster)
            .fitCenter()
            .into(posterWidget)




        val backdrop_Widget = requireView().findViewById<ImageView>(R.id.backdropWidget)
        requireView().findViewById<ImageView>(R.id.WatchImage).visibility = View.GONE
        Glide.with(requireContext())
            .load(backdrop)
            .centerInside()
            .into(backdrop_Widget)


        LoadingAnimation.hide(requireView())





        try {
            if (!logo.isEmpty()){
                val cShowLogo = requireView().findViewById<ImageView>(R.id.cShowLogo)

                Glide.with(requireContext())
                    .load(logo)
                    .centerInside()
                    .into(cShowLogo)
                binding.watchTitle.visibility = View.GONE
            }

        }catch (e: Exception){
            binding.watchTitle.text = name
        }


        if (seasons.length() == 1){
            binding.SeasonHeadline.visibility = View.GONE
            getEpisodes(id)
        }else if (seasons.length() > 1){
            binding.SeasonHeadline.visibility = View.VISIBLE
            createSeasonButtons(seasons.length(), seasons )
        }else{
            binding.SeasonHeadline.text = "Unavailable"
        }


        setupFavoriteButton(
            animeId = animeId,
            name = name,
            type = type,
            anilistId = anilistId,
            malId = malId,
            description = description,
            rating = rating,
            quality = quality,
            duration = duration,
            backdrop = backdrop,
            sub=sub,
            dub=dub,
            aired=aired,
            genre=genre,
            seasons = seasons.toString()
        )


        //showRecommendation(relatedAnimes, recommendedAnime)



    }


    private fun createSeasonButtons(
        noOfSeasons: Int,
        seasonData: JSONArray
    ) {
        val container = requireView().findViewById<LinearLayout>(R.id.anime_season_selector_container)
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        for (i in 0 until noOfSeasons) {
            val season = seasonData.getJSONObject(i)

            val seasonBtn = inflater.inflate(R.layout.anime_season_item2, container, false) as FrameLayout
            val seasonTitle = seasonBtn.findViewById<TextView>(R.id.SeasonTitle)

            val title = season.optString("title", "Season ${i + 1}")
            val imageUrl = season.optString("poster", "")
            val season_id = season.optString("id", "")

            seasonTitle.text = title

            SeasonIMGArray.add(imageUrl)

            seasonBtn.setOnClickListener {

                selectedSeasonView?.isSelected = false
                seasonBtn.isSelected = true

                selectedSeasonView = seasonBtn

                requireView().findViewById<TextView>(R.id.selected_seasonShow).text = "List of episodes ($title)"

                getEpisodes(season_id)
            }

            container.addView(seasonBtn)
            if (i == 0) seasonBtn.performClick()
        }
    }
    @OptIn(UnstableApi::class)
    private fun getEpisodes(seasonId: String){
        val container = requireView().findViewById<LinearLayout>(R.id.anime_episodes_selector_container)
        container.removeAllViews()

        val jsonObject = fetchAnime.animeEpisodes(seasonId)

        Log.e("ANIME_Watch EPISODES", jsonObject.toString())
        if (jsonObject==null){
            return
        }

        val data = jsonObject.getJSONObject("data")
        val  episodes = data.getJSONArray("episodes")


        val inflater = LayoutInflater.from(requireContext())

        for (i in 0 until episodes.length()) {
            val episode = episodes.getJSONObject(i)

            val cardView = inflater.inflate(R.layout.anime_item_episode, container, false) as CardView
            val epTitle = cardView.findViewById<TextView>(R.id.episode_name)
            val epNumber = cardView.findViewById<TextView>(R.id.episode_Number)
            val epImg = cardView.findViewById<ImageView>(R.id.episode_image)
            val cWatchSeek_bar = cardView.findViewById<SeekBar>(R.id.cWatchSeek_bar)


            val eTitle = episode.optString("title", "${i + 1}")
            val eNumber = episode.optString("number", "")
            val episodeId = episode.optString("episodeId", "")
            val eImageUrl = episode.optString("image", "")

            try{
                Glide.with(this)
                    .load(eImageUrl)
                    .centerCrop()
                    .into(epImg)
            }catch (e:Exception){}


            epTitle.text = eTitle
            epNumber.text = "E$eNumber "

            val lastPos = db.getResumePosition(userId, episodeId, "anime").toLong()
            val durationPos = db.getDurationPosition(userId, episodeId, "anime").toLong()
            val progress = ((lastPos.toDouble() / durationPos.toDouble()) * 1000).toInt()
            cWatchSeek_bar.progress = progress.coerceIn(0, 1000)


            cardView.setOnClickListener {
                Log.e("ANIME_episodeId ", "episodeId: $episodeId")

                //Anime_Video_Player.playVideoExternally(requireContext(), episodeId, eNumber, seasonId, eTitle)
                Anime_Video_Player.playVideoExternally(requireContext(), episodeId, eNumber, seasonId)
            }
            container.addView(cardView)
        }


    }


    private fun showRecommendation(data: JSONArray, data2: JSONArray) {

        // ✅ Merge the two JSONArrays
        val Airing = JSONArray()
        for (i in 0 until data.length()) {
            Airing.put(data.getJSONObject(i))
        }
        for (i in 0 until data2.length()) {
            Airing.put(data2.getJSONObject(i))
        }

        var RecommendationItems = mutableListOf<AiringAnimeItem>()

        for (i in 0 until Airing.length()) {


            val item = Airing.getJSONObject(i)

            val title = item.getString("name")

            val imageUrl = item.getString("poster")

            val id = item.getString("id")

            val type = item.getString("type")

            val sub = item.getJSONObject("episodes").optString("sub", "")
            val dub = item.getJSONObject("episodes").optString("dub", "")


            RecommendationItems.add(
                AiringAnimeItem(
                    id,
                    title,
                    imageUrl,
                    type,
                    sub,
                    dub
                )
            )

        }


        val recyclerView = requireView().findViewById<RecyclerView>(R.id.animeWatchRecommendation)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), GlobalUtils.calculateSpanCount(requireContext(), 170))
        recyclerView.adapter = AnimeAiringAdapter(RecommendationItems, R.layout.anime_airing_item)

        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

    }


    private fun setupFavoriteButton(
        animeId :String,
        name:String,
        type :String,
        anilistId :String,
        malId :String,
        description:String,
        rating :String,
        quality :String,
        duration :String,
        backdrop :String?,
        sub:String,
        dub:String,
        aired:String,
        genre:String,
        seasons:String,


    ) {
        val userId = sm.getUserId()
        val favoriteButton =  binding.favoriteButton
        val favoriteButtonImg =  binding.favoriteButtonImg
        val favoriteButtonText =  binding.favoriteButtonText



        favoriteButton.requestFocus()

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyIcon() {
            val isFav = db.isFavoriteAnime(userId, animeId)
            if (isFav) {
                favoriteButtonImg.setImageResource(R.drawable.ic_tickfave)
                favoriteButtonText.text = "Remove-Fav "
            } else {
                favoriteButtonImg.setImageResource(R.drawable.ic_addfave)
                favoriteButtonText.text= "Add-to-Fav "
            }
        }

        applyIcon()

        favoriteButton.setOnClickListener {
            val isFav = db.isFavoriteAnime(userId, animeId)
            if (isFav) {
                db.removeFavoriteAnime(userId, animeId)
            } else {
                db.addFavoriteAnime(
                    userId,
                    animeId,
                    name,
                    type,
                    anilistId,
                    malId,
                    description,
                    rating,
                    quality,
                    duration,
                    backdrop?:poster,
                    sub,
                    dub,
                    aired,
                    genre,
                    seasons
                )
            }
            applyIcon()
        }
    }


    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {


                // Cancel any running coroutines
                lifecycleScope.coroutineContext.cancelChildren()

                // Remove all handler callbacks
                Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)


                // If controls are hidden, exit the video player
                requireActivity().supportFragmentManager.popBackStack()

            }
        })
    }



}