package com.sourceservermanager.data

import android.app.Application
import androidx.lifecycle.LiveData

class ServerRepo(application: Application) {

    private val database = ApplicationDatabase.getInstance(application.applicationContext)

    private var serverDao = database.serverDao()
    private var chatDao = database.chatDao()
    private var rconDao = database.rconDao()

    fun insert(server: Server) = database.dbWriter.execute { serverDao.insert(server) }

    fun insert(chat: Chat) = database.dbWriter.execute { chatDao.insert(chat) }

    fun insert(rcon: Rcon) = database.dbWriter.execute { rconDao.insert(rcon) }

    fun update(server: Server) = database.dbWriter.execute { serverDao.update(server) }

    fun delete(server: Server) = database.dbWriter.execute { serverDao.delete(server) }

    fun deleteAllServers() = database.dbWriter.execute { serverDao.deleteAllServers() }

    fun deleteAllChats() = database.dbWriter.execute { chatDao.deleteAllChats() }

    fun deleteAllRcon() = database.dbWriter.execute { rconDao.deleteAllRcon() }

    fun getAllServers(): LiveData<List<Server>> = serverDao.getAllServers()

    fun getChatHistory(ip: String): LiveData<List<Chat>> = chatDao.getChatHistory(ip)

    fun getRconHistory(ip: String): LiveData<List<Rcon>> = rconDao.getRconHistory(ip)

    fun deleteChatHistory(ip: String) = database.dbWriter.execute { chatDao.getChatHistory(ip) }

    fun deleteRconHistory(ip: String) = database.dbWriter.execute { rconDao.deleteRconHistory(ip) }

}