package com.sourceservermanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sourceservermanager.data.Rcon
import kotlinx.android.synthetic.main.card_view_rcon.view.*

class RconAdapter: ListAdapter<Rcon, RconAdapter.RconHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Rcon>() {
            override fun areItemsTheSame(oldItem: Rcon, newItem: Rcon): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Rcon, newItem: Rcon): Boolean {
                return oldItem.rconMessage == newItem.rconMessage &&
                        oldItem.rconTimestamp == newItem.rconTimestamp &&
                        oldItem.rconTitle == newItem.rconTitle &&
                        oldItem.rconIP == newItem.rconIP
            }
        }
    }

    private var listener: OnItemLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RconHolder {
        val itemView: View = LayoutInflater.from((parent.context)).inflate(R.layout.card_view_rcon, parent, false)
        return RconHolder(itemView)
    }

    override fun onBindViewHolder(holder: RconHolder, position: Int) {
        val currentRcon: Rcon = getItem(position)

        holder.rconViewTimeStamp.text = currentRcon.rconTimestamp
        holder.rconViewMessage.text = currentRcon.rconMessage

    }

    inner class RconHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemLongClick(getItem(position))
                }
                true
            }
        }

        val rconViewTimeStamp: TextView = itemView.rcon_card_time
        val rconViewMessage: TextView = itemView.rcon_card_message
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(rcon: Rcon)
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        this.listener = listener
    }
}