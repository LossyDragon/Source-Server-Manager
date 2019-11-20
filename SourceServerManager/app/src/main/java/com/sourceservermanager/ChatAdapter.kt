package com.sourceservermanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sourceservermanager.data.Chat
import kotlinx.android.synthetic.main.card_view_chat.view.*

class ChatAdapter(
        private val longListener: OnItemLongClickListener
) : ListAdapter<Chat, ChatAdapter.ChatHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.messageTimestamp == newItem.messageTimestamp &&
                        oldItem.playerName == newItem.playerName &&
                        oldItem.playerTeam == newItem.playerTeam &&
                        oldItem.message == newItem.message &&
                        oldItem.sayTeamFlag == newItem.sayTeamFlag
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHolder {
        val itemView: View = LayoutInflater.from((parent.context)).inflate(R.layout.card_view_chat, parent, false)
        return ChatHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatHolder, position: Int) {
        val currentChat: Chat = getItem(position)
        holder.bind(currentChat, longListener)
    }

    inner class ChatHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var chatViewTimeStamp: TextView = itemView.chat_card_time
        private var chatViewMessage: TextView = itemView.chat_card_message
        private var chatTeam: TextView = itemView.chat_card_team
        private var chatWho: TextView = itemView.chat_card_who

        fun bind(
                chat: Chat,
                longListener: OnItemLongClickListener
        ) {
            chatViewTimeStamp.text = chat.messageTimestamp
            chatViewMessage.text = chat.message
            chatTeam.text = ": "
            chatWho.text = chat.playerName

            itemView.setOnLongClickListener {
                longListener.onItemLongClick(chat)
                true
            }

        }
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(chat: Chat)
    }
}