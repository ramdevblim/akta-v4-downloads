package com.akta.luraplayersampleapp.modern.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.data.Video

class ModernGroupListAdapter(
    private var context: Context,
    private val videos: List<Pair<String, List<Video>>>,
    private val childClickListener: (name: String, videos: List<Video>, childPosition: Int) -> Unit,
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = videos.size

    override fun getChildrenCount(groupPosition: Int): Int = videos[groupPosition].second.size

    override fun getGroup(groupPosition: Int): Pair<String, List<Video>> = videos[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Video {
        return videos[groupPosition].second[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
        val cv: View = View.inflate(context, R.layout.modern_expandable_list_header, null)

        val header = cv.findViewById(R.id.header) as LinearLayout
        val title = cv.findViewById(R.id.title) as TextView
        val arrowIcon = cv.findViewById(R.id.arrow_icon) as ImageView

        title.text = getGroup(groupPosition).first

        if (isExpanded) {
            arrowIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_arrow_up
                )
            )

            header.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.header_background_selected
                    )
                )
        } else {
            arrowIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_arrow_down
                )
            )

            header.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.header_background
                    )
                )
        }

        return cv
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
        val video = getChild(groupPosition, childPosition)

        val cv: View = View.inflate(context, R.layout.modern_expandable_list_item, null)

        val item = cv.findViewById(R.id.item) as LinearLayout
        val title = cv.findViewById(R.id.title) as TextView
        val assetID = cv.findViewById(R.id.asset_id) as TextView

        if (childPosition == 0) {
            item.background = ContextCompat.getDrawable(
                context,
                R.drawable.modern_expandable_list_item_top_background
            )
        } else if (isLastChild) {
            item.background = ContextCompat.getDrawable(
                context,
                R.drawable.modern_expandable_list_item_bottom_background
            )
        }

        title.text = video.title ?: "Na"
        assetID.text = video.config.lura?.assetId ?: "Na"

        item.setOnClickListener {
            val (name, videos) = getGroup(groupPosition)
            childClickListener.invoke(name, videos, childPosition)
        }

        return cv
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

}