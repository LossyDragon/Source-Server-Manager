package com.sourceservermanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sourceservermanager.data.Chat
import com.sourceservermanager.data.ServerRepo

class ChatViewModel(application: Application): AndroidViewModel(application) {

    private var repository: ServerRepo = ServerRepo(application)

    fun insert(chat: Chat) {
        repository.insert(chat)
    }

    fun deleteAllChats() {
        repository.deleteAllChats()
    }

    fun deleteChatHistory(ip: String) {
        repository.deleteChatHistory(ip)
    }

    fun getChatHistory(ip: String): LiveData<List<Chat>> {
        return repository.getChatHistory(ip)
    }

}