package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Notification_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_page)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LoadingAnimation.setup(this@Notification_Page)
        NavAction.setupSidebar(this)



        val recyclerView = findViewById<RecyclerView>(R.id.notification_widget)
        recyclerView.layoutManager = LinearLayoutManager(this)



        CoroutineScope(Dispatchers.IO).launch {
            val notifications = NavAction.loadNotifications(this@Notification_Page)
            //val notifications = NotificationHelper.getNotifications(this@Notification_Page)
            Log.e("NotificationHelper", "notifications ${notifications}")


            withContext(Dispatchers.Main) {

                val adapter = NotificationAdapter(
                    items = notifications.toMutableList(),
                    layoutResId = R.layout.item_notification
                )
                recyclerView.adapter = adapter
            }

        }




    }
}