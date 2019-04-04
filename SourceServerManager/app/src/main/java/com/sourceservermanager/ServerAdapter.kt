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

class ServerAdapter : ListAdapter<Server, ServerAdapter.NoteHolder>(DIFF_CALLBACK) {

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

    private var listener: OnItemClickListener? = null
    private var longListener: OnItemLongClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.card_view_row, parent, false)
        return NoteHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val currentNote: Server = getItem(position)

        holder.textViewName.text = currentNote.serverTitle
        holder.textViewHost.text = currentNote.serverIP
        holder.textViewPort.text = currentNote.serverPort
    }

    fun getServerAt(position: Int): Server {
        return getItem(position)
    }

    inner class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(getItem(position))
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    longListener?.onItemLongClick(getItem(position))
                }
                true
            }
        }

        var textViewName: TextView = itemView.card_nickname
        var textViewHost: TextView = itemView.card_host
        var textViewPort: TextView = itemView.card_port
    }

    interface OnItemClickListener {
        fun onItemClick(server: Server)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(server: Server)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        this.longListener = listener
    }
}