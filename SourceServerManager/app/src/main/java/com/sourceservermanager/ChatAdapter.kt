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

class ChatAdapter: ListAdapter<Chat, ChatAdapter.ChatHolder>(DIFF_CALLBACK) {

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

    private var listener: OnItemLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHolder {
        val itemView: View = LayoutInflater.from((parent.context)).inflate(R.layout.card_view_chat, parent, false)
        return ChatHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatHolder, position: Int) {
        val currentChat: Chat = getItem(position)

        holder.chatViewTimeStamp.text = currentChat.messageTimestamp
        holder.chatViewMessage.text = currentChat.message
        holder.chatTeam.text = ": "
        holder.chatWho.text = currentChat.playerName

        //Log.d("ChatAdapter", "protocolVersion >" + currentChat.protocolVersion.toString())
        //Log.d("ChatAdapter", "sayTeamFlag >" + currentChat.sayTeamFlag)
        //Log.d("ChatAdapter", "serverTimestamp >" + currentChat.serverTimestamp)
        //Log.d("ChatAdapter", "gameServerIP >" + currentChat.gameServerIP)
        //Log.d("ChatAdapter", "gameServerPort >" + currentChat.gameServerPort)
        //Log.d("ChatAdapter", "messageTimestamp >" + currentChat.messageTimestamp)
        //Log.d("ChatAdapter", "playerName >" + currentChat.playerName)
        //Log.d("ChatAdapter", "playerTeam >" + currentChat.playerTeam)
        //Log.d("ChatAdapter", "message >" + currentChat.message)
    }

    inner class ChatHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemLongClick(getItem(position))
                }
                true
            }
        }

        val chatViewTimeStamp: TextView = itemView.chat_card_time
        val chatViewMessage: TextView = itemView.chat_card_message
        val chatTeam: TextView = itemView.chat_card_team
        val chatWho: TextView = itemView.chat_card_who
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(chat: Chat)
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        this.listener = listener
    }

}