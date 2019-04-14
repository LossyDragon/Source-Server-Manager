package com.sourceservermanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sourceservermanager.data.Chat
import com.sourceservermanager.data.ServerRepo

class ChatViewModel(application: Application): AndroidViewModel(application) {

    private var repository: ServerRepo = ServerRepo(application)
    private var allChats: LiveData<List<Chat>> = repository.getAllChats()

    fun insert(chat: Chat) {
        repository.insert(chat)
    }

    fun deleteAllChats() {
        repository.deleteAllChats()
    }

    fun getAllChats(): LiveData<List<Chat>> {
        return allChats
    }

}