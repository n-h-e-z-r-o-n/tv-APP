package com.example.onyx.OnyxClasses

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.text.format.DateUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import com.bumptech.glide.request.target.Target
import com.example.onyx.Actor_Page
import com.example.onyx.Anime_Video_Player
import com.example.onyx.Category_Page
import com.example.onyx.FetchData.TMDBapi

import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.Instraction
import com.example.onyx.Login_Page
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.Play
import com.example.onyx.R
import com.example.onyx.Shows_Page
import com.example.onyx.Watch_Anime_Page
import com.example.onyx.Watch_Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.text.toLongOrNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.* // Make sure this is imported

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class GridAdapter(
    private val items: MutableList<MovieItemOne>,
    private val layoutResId: Int,

) : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 150L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null
    var onItemFocused: ((View, MovieItemOne) -> Unit)? = null
    var onItemFocusLost: (() -> Unit)? = null
    var isLoadingMore = false
        set(value) {
            field = value
            notifyItemChanged(items.size) // refresh the "Add More" item only
        }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView? = view.findViewById(R.id.itemImage)
        val showYear: TextView? = view.findViewById(R.id.itemText)
        val showTitle: TextView? = view.findViewById(R.id.showTitle)
        val showRating: TextView? = view.findViewById(R.id.showRating)
        val showRS: TextView? = view.findViewById(R.id.showRS)
        val showType: TextView? = view.findViewById(R.id.showType)

        val Logo_image: ImageView? = view.findViewById(R.id.itemLogo)



        init {

        }
    }

    override fun getItemViewType(position: Int): Int {
        // The last item is the "Add More" button
        return if (position == items.size) VIEW_TYPE_ADD_BUTTON else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_ADD_BUTTON) {
            R.layout.item_add_more // 👈 Create this layout separately
        } else {
            layoutResId
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD_BUTTON) {

            val content = holder.itemView.findViewById<View>(R.id.addMoreContent)
            val loading = holder.itemView.findViewById<View>(R.id.addMoreLoading)

            if (isLoadingMore) {
                content.visibility = View.GONE
                loading.visibility = View.VISIBLE
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
            } else {
                content.visibility = View.VISIBLE
                loading.visibility = View.GONE
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
            }
            // Handle the Add More button
            holder.itemView.setOnClickListener {
                onAddMoreClicked?.invoke()
            }
            return
        }

        val currentItem = items[position]
        val title = currentItem.title
        val imageUrl = currentItem.backdropUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val year = currentItem.year
        val rating = currentItem.rating
        val runtime = currentItem.runtime

        holder.showYear?.text = year.substring(0, 4)
        holder.showTitle?.text = title
        holder.showRating?.text = rating
        holder.showRS?.text = runtime
        holder.showType?.text = type


        val fetch = TMDBapi(holder.itemView.context)

        fetch.fetchLogos(type, imdbCode, holder.Logo_image as ImageView, holder.showTitle as TextView)





        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image!!)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                onItemFocused?.invoke(v, currentItem)
            }
            else {
                onItemFocusLost?.invoke()   // hide popup
            }
        }



        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val now = System.currentTimeMillis()
            if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
            lastKeyTime = now

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size) return@setOnKeyListener true
                }
            }

            false
        }


    }

    override fun getItemCount(): Int {
        // Total items = movies + the add button
        return items.size + 1
    }

    fun addItem(item: MovieItemOne) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<MovieItemOne>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }
}


data class MovieItemOne(
    val title: String = "",
    val backdropUrl: String= "",
    val posterUlr: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = ""
)
////////////////////////////////////////////////////////////////////////////////////////////////////

class FilterAdapter(
    private val items: MutableList<filterItemOne>,
    private val layoutResId: Int
) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 170L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null
    var onItemFocused: ((View, filterItemOne) -> Unit)? = null
    var onItemFocusLost: (() -> Unit)? = null
    var isLoadingMore = false
        set(value) {
            field = value
            notifyItemChanged(items.size) // refresh the "Add More" item only
        }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView? = view.findViewById(R.id.itemImage)
        val showYear: TextView? = view.findViewById(R.id.itemText)
        val showTitle: TextView? = view.findViewById(R.id.showTitle)
        val showRating: TextView? = view.findViewById(R.id.showRating)
        val showRS: TextView? = view.findViewById(R.id.showRS)
        val showType: TextView? = view.findViewById(R.id.showType)

        init {

        }
    }

    override fun getItemViewType(position: Int): Int {
        // The last item is the "Add More" button
        return if (position == items.size) VIEW_TYPE_ADD_BUTTON else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_ADD_BUTTON) {
            R.layout.item_add_more // 👈 Create this layout separately
        } else {
            layoutResId
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD_BUTTON) {

            val content = holder.itemView.findViewById<View>(R.id.addMoreContent)
            val loading = holder.itemView.findViewById<View>(R.id.addMoreLoading)

            if (isLoadingMore) {
                content.visibility = View.GONE
                loading.visibility = View.VISIBLE
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
            } else {
                content.visibility = View.VISIBLE
                loading.visibility = View.GONE
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
            }
            // Handle the Add More button
            holder.itemView.setOnClickListener {
                onAddMoreClicked?.invoke()
            }
            return
        }

        val currentItem = items[position]
        val title = currentItem.title
        val imageUrl = currentItem.backdropUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val year = currentItem.year
        val rating = currentItem.rating
        val runtime = currentItem.runtime

        holder.showYear?.text = year
        holder.showTitle?.text = title
        holder.showRating?.text = rating
        holder.showRS?.text = runtime
        holder.showType?.text = type

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image!!)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }

        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                onItemFocused?.invoke(v, currentItem)
            }
            else {
                onItemFocusLost?.invoke()   // hide popup
            }
        }



        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val now = System.currentTimeMillis()
            if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
            lastKeyTime = now

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size) return@setOnKeyListener true
                }
            }

            false
        }
    }

    override fun getItemCount(): Int {
        // Total items = movies + the add button
        return items.size + 1
    }

    fun addItem(item: filterItemOne) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<filterItemOne>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }
}


data class filterItemOne(
    val title: String = "",
    val backdropUrl: String= "",
    val posterUlr: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = ""
)
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class OtherAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<OtherAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val CardViewSquare: CardView = view.findViewById(R.id.CardViewSquare)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)



        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerCrop()
            .into(holder.Movie_image)


        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", imdbCode)
                putExtra("type", type)
            }
            context.startActivity(intent)
            Log.e("OtherAdapter", "clicked ${intent.toString()}")
        }

    }

    override fun getItemCount() = items.size

    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


class CategoryAdapter(
    private val  items: MutableList<categoryItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource

) :  RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    companion object {
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 150L // ms
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val CardViewSquare: CardView = view.findViewById(R.id.categoryView)
        val category_image: ImageView = view.findViewById(R.id.categoryImage)




        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]
        val imageUrl = currentItem.cImg
        val imdbCode = currentItem.cCode
        val companyName = currentItem.cName

        // ✅ wait until ImageView is measured
        holder.category_image.post {
            val currentHeight = holder.category_image.height
            val finalHeight = if (currentHeight > 200) 200 else currentHeight

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.category_image)
        }





        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Category_Page::class.java).apply {
                putExtra("company_id", imdbCode)
                putExtra("company_name", companyName)
            }
            context.startActivity(intent)
        }


        holder.CardViewSquare.setOnKeyListener { v, keyCode, event ->

            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val now = System.currentTimeMillis()
            if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
            lastKeyTime = now

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    //if (position == 0) return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size-1) return@setOnKeyListener true
                }
            }

            false
        }
    }

    override fun getItemCount() = items.size

    fun addItem(item: categoryItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

data class categoryItem(
    val cCode: String = "",
    val cImg: String= "",
    val cName: String = ""
)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


class GridAdapter2(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<GridAdapter2.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val showYear: TextView = view.findViewById(R.id.itemText)


        val showTitle: TextView = view.findViewById(R.id.showTitle)
        val showRating: TextView = view.findViewById(R.id.showRating)
        val showRS: TextView = view.findViewById(R.id.showRS)
        val showType: TextView = view.findViewById(R.id.showType)





        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

                try {
                    // Overlay fade
                    val overlay: View = itemView.findViewById(R.id.focusOverlay)

                    if (hasFocus) {
                        overlay.apply {
                            alpha = 0f
                            visibility = View.VISIBLE
                            animate().alpha(1f).setDuration(150).start()
                        }
                    } else {
                        overlay.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction { overlay.visibility = View.GONE }
                            .start()
                    }
                } catch (e : Exception){}
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val year = currentItem.year
        val rating = currentItem.rating
        val runtime = currentItem.runtime





        holder.showYear.text = year
        holder.showTitle.text = title
        holder.showRating.text = rating
        holder.showRS.text = runtime
        holder.showType.text = type




        /*
        Picasso.get()
            .load(imageUrl)
            .fit()
            .centerInside()
            .into(holder.Movie_image)

         */

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", currentItem.imdbCode)
            intent.putExtra("type", currentItem.type)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



class RecommendAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<RecommendAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val CardViewSquare: CardView = view.findViewById(R.id.CardViewSquare)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)



        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type

        holder.itemText.text = title


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerCrop()
            .into(holder.Movie_image)


        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", imdbCode)
                putExtra("type", type)
            }
            context.startActivity(intent)
            Log.e("OtherAdapter", "clicked ${intent.toString()}")
        }

    }

    override fun getItemCount() = items.size

    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



class CastAdapter(
    private val  items: MutableList<CastItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<CastAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val Actor_Name: TextView = view.findViewById(R.id.itemText)



        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type



        holder.Actor_Name.text = title


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Actor_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }

        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }


    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: CastItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }

}


data class CastItem(
    val title: String = "",
    val imageUrl: String= "",
    val imdbCode: String= "",
    val type: String = "",

)

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ProfileAdapter (
    private val  items: MutableList<profileItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<ProfileAdapter .ViewHolder>() {

    var onProfileSelected: ((profileItem) -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.profileCardContiner)
        val profileImageWidget: ImageView = view.findViewById(R.id.itemUserAvatar)
        val usernameWidget: TextView = view.findViewById(R.id.itemUsername)



        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val username = currentItem.username
        val userid = currentItem.userid
        val avatarImg = currentItem.avatar


        holder.usernameWidget.text = username

        // Handle "Create Profile" button appearance
        if (userid == "CREATE") {
            holder.profileImageWidget.setImageResource(R.drawable.ic_account)
            holder.profileImageWidget.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.profileImageWidget.setBackgroundColor(Color.parseColor("#00000000"))


        } else {
            // Handle avatar loading - if empty, use placeholder
            if (avatarImg.isNotEmpty()) {

                val assetPath = "file:///android_asset/$avatarImg"
                Glide.with(holder.itemView.context)
                    .load(assetPath)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.profileImageWidget)
            } else {
                holder.profileImageWidget.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        holder.CardViewcontiner.setOnClickListener {
            onProfileSelected?.invoke(currentItem) ?: run {

            }
        }

        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }


    }

    override fun getItemCount() = items.size

    fun addItem(item: profileItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
    
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

}


data class profileItem(
    val username: String = "",
    val avatar: String= "",
    val userid: String= "",
    )

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// AvatarAdapter - for selecting profile avatars from assets
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class AvatarAdapter(
    private val avatarPaths: List<String>,
    private val layoutResId: Int
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    var onAvatarSelected: ((String) -> Unit)? = null
    private var selectedPosition: Int = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImage: ImageView = view.findViewById(R.id.avatarImage)
        val avatarCardView: CardView = view.findViewById(R.id.avatarCardView)

        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation on focus
                v.animate()
                    .scaleX(if (hasFocus) 1.05f else 1f)
                    .scaleY(if (hasFocus) 1.05f else 1f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatarPath = avatarPaths[position]
        val context = holder.itemView.context

        // Load image from assets using Glide
        try {
            val assetPath = "file:///android_asset/$avatarPath"
            Glide.with(context)
                .load(assetPath)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.avatarImage)
        } catch (e: Exception) {
            Log.e("AvatarAdapter", "Error loading avatar: ${e.message}", e)
            holder.avatarImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Highlight selected avatar
        if (position == selectedPosition) {
            holder.avatarCardView.setCardBackgroundColor(
                Color.parseColor("#4CAF50")
            )
        } else {
            holder.avatarCardView.setCardBackgroundColor(
                Color.TRANSPARENT
            )
        }

        // Handle click
        holder.avatarCardView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // Notify changes for selection highlight
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            
            // Invoke callback
            onAvatarSelected?.invoke(avatarPath)
        }

        // Handle focus for TV/remote control
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = false
    }

    override fun getItemCount() = avatarPaths.size
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class FavAdapter(
    private val  items: MutableList<FavItem>,
    private val layoutResId: Int ,
    private val backdropView: ImageView,
    private val favTitleView: TextView,
    private val favGenreView: TextView,
    private val favTypeView: TextView,
    private val favRatingView: TextView,
    private val favYearView: TextView,
    private val favOverviewView: TextView,
    private val RemoveFaveItemBtn: LinearLayout

) :  RecyclerView.Adapter<FavAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)


        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->

                //Scale animation
                itemView.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()



                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = items[pos]

                        // Backdrop image
                        Glide.with(backdropView.context)
                            .load(item.backdropUrl)
                            //.override(Target.SIZE_ORIGINAL, backdropView.height) // scale height to container
                            .into(backdropView)

                        // ✅ Set text properties correctly
                        favTitleView.text    = item.title
                        favGenreView.text    = item.genres
                        favTypeView.text     = item.showType          // ensure you have a `type` field
                        favRatingView.text   = "${item.voteAverage}"
                        favYearView.text     = item.releaseDate
                        favOverviewView.text = item.overview


                        RemoveFaveItemBtn.setOnClickListener {
                            //.removeFavorite( RemoveFaveItemBtn.context, item.imdbCode, item.showType)
                            val pos = bindingAdapterPosition
                            if (pos != RecyclerView.NO_POSITION) {
                                items.removeAt(pos)
                                notifyItemRemoved(pos)
                            }
                        }
                    }
                }
            }


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val posterUrl = currentItem.posterUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.showType



        Glide.with(holder.itemView.context)
            .load(posterUrl)
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if(type == "anime"){
                val intent = Intent(context, Watch_Anime_Page::class.java)
                intent.putExtra("anime_code", imdbCode)
                intent.putExtra("anime_poster", posterUrl)
                context.startActivity(intent)
            }else {
                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", imdbCode)
                intent.putExtra("type", type)
                context.startActivity(intent)
            }
        }


    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: FavItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateItems(newItems: List<FavItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()  // Notify RecyclerView that data is cleared
    }

}


data class FavItem(
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val releaseDate: String,
    val runtime: String,
    val overview: String,
    val voteAverage: String,
    val genres: String,
    val production: String,
    val parentalGuide: String,
    val imdbCode: String,
    val showType : String,
)
//////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class EqualSxpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1

        // Apply equal spacing on all sides
        outRect.left = space / 2
        outRect.right = space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        // Optional: extra space for first/last rows & columns so edges are even
        if (position < spanCount) {
            outRect.top = space // first row
        }
        if (position % spanCount == 0) {
            outRect.left = space // first column
        }
    }
}


class EqualSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1

        // Apply equal spacing on all sides
        outRect.left = space / 2
        outRect.right = space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        // Optional: extra space for first/last rows & columns so edges are even
        if (position < spanCount) {
            outRect.top = space // first row
        }
        if (position % spanCount == 0) {
            outRect.left = space // first column
        }
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


class EpisodesAdapter(
    private val episodes: MutableList<EpisodeItem>,
    private val db: AppDatabase,
    private val userId: Int,
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    inner class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val epNoView: TextView = view.findViewById(R.id.episode_Number)
        val titleView: TextView = view.findViewById(R.id.episode_title)
        val durationView: TextView = view.findViewById(R.id.episode_duration)
        val ratingView: TextView = view.findViewById(R.id.episode_Rating)
        val descView: TextView = view.findViewById(R.id.episode_description)
        val epsImg: ImageView = view.findViewById(R.id.Ep_IMG)

        var cWatchSeek_bar: SeekBar = view.findViewById(R.id.cWatchSeek_bar)

        var job: Job? = null  // Track the coroutine for this ViewHolder

        var lastClickTime = 0L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val ep = episodes[position]

        holder.job?.cancel()  // cancel previous coroutine if any
        holder.job = adapterScope.launch {
            val itemId = "${ep.seriesId}_S${ep.seasonNumber}_E${ep.episodesNumber}"
            //val lastPos = db.getResumePosition(userId, itemId, "tv").toLong()
            //val durationPos = db.getDurationPosition(userId, itemId, "tv").toLong()

            /*
            val lastPos = withContext(Dispatchers.IO) {
                db.getResumePosition(userId, itemId, "tv").toLong()
            }
            val durationPos = withContext(Dispatchers.IO) {
                db.getDurationPosition(userId, itemId, "tv").toLong()
            }
            */

            // Run both DB queries concurrently
            val lastPosDeferred = async(Dispatchers.IO) { db.getResumePosition(userId, itemId, "tv").toLong() }
            val durationPosDeferred = async(Dispatchers.IO) { db.getDurationPosition(userId, itemId, "tv").toLong() }

            // Await both results
            val lastPos = lastPosDeferred.await()
            val durationPos = durationPosDeferred.await()

            val safeDuration = if (durationPos == 0L) 1L else durationPos
            val progress = ((lastPos.toDouble() / safeDuration.toDouble()) * 1000).toInt()
            holder.cWatchSeek_bar.progress = progress.coerceIn(0, 1000)
        }


        holder.epNoView.text = "S${ep.seasonNumber}-E${ep.episodesNumber}"
        holder.titleView.text = ep.episodesName
        holder.durationView.text = "⏱ ${ep.episodesRuntime} min"
        holder.ratingView.text = "★ ${ep.episodesRating}"
        holder.descView.text = ep.episodesDescription

        GlobalUtils.enableFullViewOnDescendantFocus(ep.parentView, holder.itemView)


        val url = "https://image.tmdb.org/t/p/original${ep.episodesImage}"
        Glide.with(holder.itemView.context)
            .load(url)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .into(holder.epsImg)

        //var lastClickTime = 0L
        holder.itemView.setOnClickListener {view ->
            val now = System.currentTimeMillis()
            if (now - holder.lastClickTime < 1000) return@setOnClickListener
            holder.lastClickTime = now

            val context = holder.itemView.context
            val intent = Intent(context, Play::class.java).apply {
                Log.e("DEBUG_Each EpisodeWatch", "Eps ${ep.episodesNumber} Season ${ep.seasonNumber}")
                putExtra("imdb_code", ep.seriesId)
                putExtra("type", "tv")
                putExtra("seasonNo", ep.seasonNumber)
                putExtra("episodeNo", ep.episodesNumber)
                putExtra("poster", ep.showPoster)
                putExtra("backdrop", ep.showBackdrop)
                putExtra("title", ep.showTitle)
            }
            //context.startActivity(intent)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("EpisodesAdapter", "Play activity not found", e)
                Toast.makeText(context, "Cannot open player", Toast.LENGTH_SHORT).show()
            }
        }



        // ✅ Attach the KeyListener here
        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == episodes.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }


    }



    override fun getItemCount(): Int = episodes.size



    fun updateData(newList: List<EpisodeItem>) {
        episodes.clear()
        episodes.addAll(newList)
        notifyDataSetChanged()
    }
}



data class EpisodeItem(
    val showTitle: String = "",
    val showPoster: String = "",
    val showBackdrop: String = "",
    val episodesName: String = "",
    val episodesImage: String= "",
    val episodesNumber: String= "",
    val episodesRating: String = "",
    val episodesRuntime: String = "",
    val episodesDescription: String = "",
    val seriesId: String = "",
    val seasonNumber: String = "",
    val parentView: ViewGroup

)

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


data class MovieItem(
    val title: String = "",
    val imageUrl: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = ""
)


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////





class NotificationAdapter(
    private val  items: MutableList<NotificationItem>,   // ✅ mutable now,
    private val layoutResId: Int,  // 👈 pass in the layout resource
    private val db: AppDatabase,
    private val userId: Int
) :  RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {


    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val image: ImageView = view.findViewById(R.id.notification_title)
        val showTitle: TextView = view.findViewById(R.id.notification_title)
        val message: TextView = view.findViewById(R.id.notification_message)
        val imageContainer: ImageView = view.findViewById(R.id.notification_icon)
        val timeContainer: TextView = view.findViewById(R.id.timestamp_text)


        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val info = currentItem.info
        val updateSeason  = currentItem.newSeason
        val updateEpisode  = currentItem.newEpisode
        val lastPos = currentItem.time?.toLongOrNull() ?: 0L

        holder.timeContainer.text = formatTimeFromString(currentItem.time)


        holder.showTitle.text =  title
        holder.message.text =  info


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.imageContainer)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context

            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
            items.removeAt(adapterPos)
            notifyItemRemoved(adapterPos)


                if(type == "anime"){

                    adapterScope.launch(Dispatchers.IO) {
                        db.deleteAnimeNotificationById(
                            userId = userId,
                            animeId = imdbCode,
                            notificationId = currentItem.notificationId
                        )
                    }


                    val intent = Intent(context, Watch_Anime_Page::class.java)

                    intent.putExtra("anime_code", imdbCode)
                    intent.putExtra("anime_poster", imageUrl)
                    context.startActivity(intent)

                }else {
                    val intent = Intent(context, Watch_Page::class.java)
                    adapterScope.launch(Dispatchers.IO) { db.deleteTvNotifications(userId, imdbCode)}
                    intent.putExtra("imdb_code", imdbCode)
                    intent.putExtra("type", type)
                    context.startActivity(intent)
                }

            //NotificationHelper.updateNotification(context, imdbCode, updateSeason, updateEpisode)
            //call updateNotification
        }

    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: NotificationItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
    
    // 👇 helper to refresh all items
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    // 👇 helper to remove specific item by imdbCode
    fun removeItem(imdbCode: String) {
        val index = items.indexOfFirst { it.imdbCode == imdbCode }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    private fun formatTimeFromString(timeString: String?): String {

        val timeMillis = timeString
            ?.trim()
            ?.toLongOrNull()
            ?: return "Just now"

        return DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}


data class NotificationItem(
    val notificationId: String,
    val imdbCode: String,
    val title: String,
    val imageUrl: String?,
    val info: String,
    val type: String = "tv",
    val newSeason: String,
    val newEpisode: String,
    val time: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class cWatchingAdapter(
    private val items: MutableList<HashMap<String, String>>,
    private val layoutResId: Int
) : RecyclerView.Adapter<cWatchingAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val rootCard: CardView = view.findViewById(R.id.rootCard)
        val poster: ImageView = view.findViewById(R.id.itemImage)
        val title: TextView = view.findViewById(R.id.watchItemTitle)
        val episode: TextView = view.findViewById(R.id.watchItemEpisode)
        val lastPosition: TextView = view.findViewById(R.id.watchItemLastPosition)
        val duration: TextView = view.findViewById(R.id.watchItemDuration)
        val seekBar: SeekBar = view.findViewById(R.id.cWatchSeek_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        val itemId = item["item_id"] ?: ""
        val type = item["type"] ?: ""
        val title = item["title"] ?: ""
        val posterUrl = item["poster"] ?: ""
        val backdropUrl = item["backdrop"] ?: ""
        val episodeNumber = item["episode_number"] ?: ""
        val seasonNumber = item["season_number"] ?: ""
        val lastPos = item["last_position"]?.toLongOrNull() ?: 0L
        val duration = item["duration"]?.toLongOrNull() ?: 1L

        // ---------- UI ----------
        holder.title.text = title


        holder.lastPosition.text = formatTimeMillis(lastPos)
        holder.duration.text = formatTimeMillis(duration)


        val progress = ((lastPos.toDouble() / duration.toDouble()) * 1000).toInt()
        holder.seekBar.progress = progress.coerceIn(0, 1000)

        Glide.with(holder.itemView.context)
            .load(posterUrl)
            .centerCrop()
            .into(holder.poster)

        // ---------- CLICK → RESUME ----------
        val context = holder.itemView.context
        if(type=="anime"){
            holder.episode.text = "E$episodeNumber"



            holder.rootCard.setOnClickListener {
                val context = holder.itemView.context

                Anime_Video_Player.Companion.playVideoExternally(context, itemId, episodeNumber, seasonNumber)
            }

        }else if(type=="movie"){
            holder.episode.text = ""

            holder.rootCard.setOnClickListener {

                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", itemId)
                intent.putExtra("type", type)
                intent.putExtra("title", title)
                intent.putExtra("poster", posterUrl)
                intent.putExtra("backdrop", backdropUrl)
                intent.putExtra("seasonNo", seasonNumber)
                intent.putExtra("EpisodeNo", episodeNumber)
                intent.putExtra("continue_play", true)

                context.startActivity(intent)
            }

        }else if(type=="tv"){
            holder.episode.text = "S$seasonNumber-E$episodeNumber"
            val seriesId = itemId.substringBefore("_")

            holder.rootCard.setOnClickListener {

                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", seriesId)
                intent.putExtra("type", type)
                intent.putExtra("title", title)
                intent.putExtra("poster", posterUrl)
                intent.putExtra("backdrop", backdropUrl)
                intent.putExtra("seasonNo", seasonNumber)
                intent.putExtra("EpisodeNo", episodeNumber)
                intent.putExtra("continue_play", true)

                context.startActivity(intent)
            }
        }

    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<HashMap<String, String>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    // ---------- UTIL ----------
    private fun formatTimeMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0)
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }
}

