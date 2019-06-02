package com.sourceservermanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sourceservermanager.data.Rcon
import com.sourceservermanager.data.ServerRepo

class RconViewModel(application: Application): AndroidViewModel(application) {

    private var repository: ServerRepo = ServerRepo(application)

    fun insert(rcon: Rcon) {
        repository.insert(rcon)
    }

    fun deleteAllRcon() {
        repository.deleteAllRcon()
    }

    fun deleteRconHistory(ip: String) {
        repository.deleteRconHistory(ip)
    }

    fun getRconHistory(ip: String): LiveData<List<Rcon>> {
        return repository.getRconHistory(ip)
    }

}