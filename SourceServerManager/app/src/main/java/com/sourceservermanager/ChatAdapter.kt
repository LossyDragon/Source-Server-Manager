package com.sourceservermanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.sourceservermanager.data.Chat
import kotlinx.android.synthetic.main.card_view_chat.view.*

class ChatAdapter: ListAdapter<Chat, ChatAdapter.ChatHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.timestamp == newItem.timestamp &&
                        oldItem.playerName == newItem.playerName &&
                        oldItem.playerTeam == newItem.playerTeam &&
                        oldItem.message == newItem.message &&
                        oldItem.sayTeamFlag == newItem.sayTeamFlag
            }
        }
    }

    private var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHolder {
        val itemView: View = LayoutInflater.from((parent.context)).inflate(R.layout.card_view_chat, parent, false)
        return ChatHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatHolder, position: Int) {
        val currentChat: Chat = getItem(position)

        holder.chatViewTimeStamp.text = currentChat.timestamp
        holder.chatViewMessage.text = currentChat.message
        holder.chatWho.text = currentChat.sayTeamFlag // <-- I think this is right
    }

    inner class ChatHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(getItem(position))
                }
            }
        }

        val chatViewTimeStamp: TextView = itemView.chat_card_time
        val chatViewMessage: TextView = itemView.chat_card_message
        val chatWho: TextView = itemView.chat_card_who
    }

    interface OnItemClickListener {
        fun onItemClick(chat: Chat)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

}