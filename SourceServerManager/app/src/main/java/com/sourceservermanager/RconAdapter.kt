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

class RconAdapter(
        private val longListener: OnItemLongClickListener
) : ListAdapter<Rcon, RconAdapter.RconHolder>(DIFF_CALLBACK) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RconHolder {
        val itemView: View = LayoutInflater.from((parent.context)).inflate(R.layout.card_view_rcon, parent, false)
        return RconHolder(itemView)
    }

    override fun onBindViewHolder(holder: RconHolder, position: Int) {
        val currentRcon: Rcon = getItem(position)
        holder.bind(currentRcon, longListener)
    }

    inner class RconHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var rconViewTimeStamp: TextView = itemView.rcon_card_time
        private var rconViewMessage: TextView = itemView.rcon_card_message

        fun bind(rcon: Rcon, longListener: OnItemLongClickListener) {
            rconViewMessage.text = rcon.rconMessage
            rconViewTimeStamp.text = rcon.rconTimestamp

            itemView.setOnLongClickListener {
                longListener.onItemLongClick(rcon)
                true
            }
        }
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(rcon: Rcon)
    }
}