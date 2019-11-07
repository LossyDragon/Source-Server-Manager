package com.sourceservermanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sourceservermanager.data.Server
import kotlinx.android.synthetic.main.card_view_row.view.*

class ServerAdapter(
        private val clickListener: OnItemClickListener,
        private val longClickListener: OnItemLongClickListener
) : ListAdapter<Server, ServerAdapter.ServerHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Server>() {
            override fun areItemsTheSame(oldItem: Server, newItem: Server): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Server, newItem: Server): Boolean {
                return oldItem.serverTitle == newItem.serverTitle &&
                        oldItem.serverIP == newItem.serverIP &&
                        oldItem.serverPort == newItem.serverPort
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.card_view_row, parent, false)
        return ServerHolder(itemView)
    }

    override fun onBindViewHolder(holder: ServerHolder, position: Int) {
        val server = getServerAt(position)
        holder.bind(server, clickListener, longClickListener)
    }

    fun getServerAt(position: Int): Server {
        return getItem(position)
    }

    inner class ServerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var textViewName: TextView = itemView.card_nickname
        private var textViewHost: TextView = itemView.card_host
        private var textViewPort: TextView = itemView.card_port

        fun bind(server: Server, clickListener: OnItemClickListener, longClickListener: OnItemLongClickListener) {

            textViewName.text = server.serverTitle
            textViewHost.text = server.serverIP
            textViewPort.text = server.serverPort

            itemView.setOnClickListener {
                clickListener.onItemClick(server)
            }

            itemView.setOnLongClickListener {
                longClickListener.onItemLongClick(server)
                true
            }

        }

    }

    interface OnItemClickListener {
        fun onItemClick(server: Server)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(server: Server)
    }
}