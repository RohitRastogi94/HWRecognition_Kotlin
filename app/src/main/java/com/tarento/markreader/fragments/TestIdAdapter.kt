package com.tarento.markreader.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tarento.markreader.R
import com.tarento.markreader.data.model.FetchExamsResponse
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_testid.view.*

class TestIdAdapter : RecyclerView.Adapter<TestIdAdapter.ViewHolder>() {
    val userList: MutableList<FetchExamsResponse.Data> = mutableListOf()
    private lateinit var itemClickListener: (FetchExamsResponse.Data) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_testid, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(position, itemClickListener)
    }

    fun onItemClickListener(listener: (FetchExamsResponse.Data) -> Unit) {
        itemClickListener = listener
    }

    fun refreshListItem(itemList: List<FetchExamsResponse.Data>) {
        userList.clear()
        userList.addAll(itemList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindView(position: Int, listener: (FetchExamsResponse.Data) -> Unit) {
            with(userList[position]) {
                containerView.textViewTestId.text = this.exam_code
                itemView.setOnClickListener { listener(this) }
            }
        }
    }
}
