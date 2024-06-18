package com.akta.luraplayercontrols.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayercontrols.R

class LuraMenuView(context: Context) : LinearLayout(context) {

    internal val topContainer: LinearLayout
    private val backButton: ImageView
    internal val titleView: TextView
    private val closeButton: ImageView
    internal val recyclerView: RecyclerView
    internal var adapters: ArrayDeque<RecyclerView.Adapter<*>> = ArrayDeque()

    init {
        View.inflate(context, R.layout.lura_sub_menu_view, this)
        topContainer = findViewById(R.id.lura_sub_menu_top_container)
        backButton = findViewById(R.id.lura_sub_menu_back)
        closeButton = findViewById(R.id.lura_sub_menu_close)
        recyclerView = findViewById(R.id.lura_sub_menu)
        titleView = findViewById(R.id.lura_sub_menu_title)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    fun addAdapter(adapter: RecyclerView.Adapter<*>) = adapters.add(adapter)

    fun updateAdapter(adapter: RecyclerView.Adapter<*>) {
        if (adapters.isEmpty()) {
            backButton.visibility = INVISIBLE
            titleView.visibility = INVISIBLE
            closeButton.visibility = VISIBLE
        } else {
            backButton.visibility = VISIBLE
            titleView.visibility = VISIBLE
            closeButton.visibility = INVISIBLE
        }
        recyclerView.adapter = adapter
    }
}