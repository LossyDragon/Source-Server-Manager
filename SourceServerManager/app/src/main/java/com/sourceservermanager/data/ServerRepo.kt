package com.sourceservermanager.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

class ServerRepo(application: Application) {

    private var serverDao: ServerDao
    private var chatDAO: ChatDao

    private var allServers: LiveData<List<Server>>
    private var allChats: LiveData<List<Chat>>

    init {
        val database: ApplicationDatabase = ApplicationDatabase.getInstance(
                application.applicationContext
        )!!
        serverDao = database.serverDao()
        allServers = serverDao.getAllServers()

        chatDAO = database.chatDao()
        allChats = chatDAO.getAllChats()
    }

    fun insert(server: Server) {
        InsertServerAsyncTask(serverDao).execute(server)
    }

    fun insert(chat: Chat) {
        InsertChatAsyncTask(chatDAO).execute(chat)
    }

    fun update(server: Server) {
        UpdateServerAsyncTask(serverDao).execute(server)
    }

    fun update(chat: Chat) {
        UpdateChatAsyncTask(chatDAO).execute(chat)
    }

    fun delete(server: Server) {
        DeleteServerAsyncTask(serverDao).execute(server)
    }

    fun delete(chat: Chat) {
        DeleteChatAsyncTask(chatDAO).execute(chat)
    }

    fun deleteAllServers() {
        DeleteAllServersAsyncTask(serverDao).execute()
    }

    fun deleteAllChats() {
        DeleteAllChatsAsyncTask(chatDAO).execute()
    }

    fun getAllServers(): LiveData<List<Server>> {
        return allServers
    }

    fun getAllChats(): LiveData<List<Chat>> {
        return allChats
    }

    companion object {
        private class InsertServerAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.insert(p0[0]!!)
            }
        }

        private class UpdateServerAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.update(p0[0]!!)
            }
        }

        private class DeleteServerAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.delete(p0[0]!!)
            }
        }

        private class DeleteAllServersAsyncTask(val serverDao: ServerDao) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                serverDao.deleteAllServers()
            }
        }
        
        //Chat Stuff
        private class InsertChatAsyncTask(val chatDao: ChatDao) : AsyncTask<Chat, Unit, Unit>() {
            override fun doInBackground(vararg p0: Chat?) {
                chatDao.insert(p0[0]!!)
            }
        }

        private class UpdateChatAsyncTask(val chatDao: ChatDao) : AsyncTask<Chat, Unit, Unit>() {
            override fun doInBackground(vararg p0: Chat?) {
                chatDao.update(p0[0]!!)
            }
        }

        private class DeleteChatAsyncTask(val chatDao: ChatDao) : AsyncTask<Chat, Unit, Unit>() {
            override fun doInBackground(vararg p0: Chat?) {
                chatDao.delete(p0[0]!!)
            }
        }

        private class DeleteAllChatsAsyncTask(val chatDao: ChatDao) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                chatDao.deleteAllChats()
            }
        }
    }
}