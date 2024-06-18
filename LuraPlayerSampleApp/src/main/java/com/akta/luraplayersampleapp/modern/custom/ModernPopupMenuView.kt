package com.akta.luraplayersampleapp.modern.custom

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayersampleapp.R

class ModernPopupMenuView(context: Context) : LinearLayout(context) {

    private val recyclerView: RecyclerView

    init {
        View.inflate(context, R.layout.modern_popup_menu_view, this)
        recyclerView = findViewById(R.id.options_recyclerview)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    fun updateAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

}