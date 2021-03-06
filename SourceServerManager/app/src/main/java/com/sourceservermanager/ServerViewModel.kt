package com.sourceservermanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sourceservermanager.data.Server
import com.sourceservermanager.data.ServerRepo

class ServerViewModel(application: Application) : AndroidViewModel(application) {
    private var repository: ServerRepo = ServerRepo(application)

    fun insert(server: Server) {
        repository.insert(server)
    }

    fun update(server: Server) {
        repository.update(server)
    }

    fun delete(server: Server) {
        repository.delete(server)
    }

    fun deleteAllServers() {
        repository.deleteAllServers()
    }

    fun deleteAllChats() {
        repository.deleteAllChats()
    }

    fun deleteAllRcon() {
        repository.deleteAllRcon()
    }

    fun getAllServers(): LiveData<List<Server>> {
        return repository.getAllServers()
    }
}