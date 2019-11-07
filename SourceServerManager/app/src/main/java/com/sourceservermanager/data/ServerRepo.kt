package com.sourceservermanager.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

class ServerRepo(application: Application) {

    private var serverDao: ServerDao
    private var chatDAO: ChatDao
    private var rconDao: RconDao

    init {
        val database: ApplicationDatabase = ApplicationDatabase.getInstance(
                application.applicationContext
        )!!

        serverDao = database.serverDao()
        chatDAO = database.chatDao()
        rconDao = database.rconDao()
    }

    fun insert(server: Server) {
        InsertServerAsyncTask(serverDao).execute(server)
    }

    fun insert(chat: Chat) {
        InsertChatAsyncTask(chatDAO).execute(chat)
    }

    fun insert(rcon: Rcon) {
        InsertRconAsyncTask(rconDao).execute(rcon)
    }

    fun update(server: Server) {
        UpdateServerAsyncTask(serverDao).execute(server)
    }

    fun delete(server: Server) {
        DeleteServerAsyncTask(serverDao).execute(server)
    }

    fun deleteAllServers() {
        DeleteAllServersAsyncTask(serverDao).execute()
    }

    fun deleteAllChats() {
        DeleteAllChatsAsyncTask(chatDAO).execute()
    }

    fun deleteAllRcon() {
        DeleteAllRconAsyncTask(rconDao).execute()
    }

    fun getAllServers(): LiveData<List<Server>> {
        return serverDao.getAllServers()
    }

    fun getChatHistory(ip: String): LiveData<List<Chat>> {
        return chatDAO.getChatHistory(ip)
    }

    fun getRconHistory(ip: String): LiveData<List<Rcon>> {
        return rconDao.getRconHistory(ip)
    }

    fun deleteChatHistory(ip: String) {
        DeleteChatHistoryAsyncTask(chatDAO, ip).execute()
    }

    fun deleteRconHistory(ip: String) {
        DeleteRconHistoryAsyncTask(rconDao, ip).execute()
    }

    companion object {

        //Server stuff
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

        private class DeleteAllChatsAsyncTask(val chatDao: ChatDao) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                chatDao.deleteAllChats()
            }
        }

        private class DeleteChatHistoryAsyncTask(val chatDao: ChatDao, val ip: String) : AsyncTask<Chat, Unit, Unit>() {
            override fun doInBackground(vararg params: Chat?) {
                chatDao.deleteChatHistory(ip)
            }
        }

        //Rcon Stuff
        private class InsertRconAsyncTask(val rconDao: RconDao) : AsyncTask<Rcon, Unit, Unit>() {
            override fun doInBackground(vararg params: Rcon?) {
                rconDao.insert(params[0]!!)
            }
        }

        private class DeleteAllRconAsyncTask(val rconDao: RconDao) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg params: Unit?) {
                rconDao.deleteAllRcon()
            }
        }

        private class DeleteRconHistoryAsyncTask(val rconDao: RconDao, val ip: String) : AsyncTask<Rcon, Unit, Unit>() {
            override fun doInBackground(vararg params: Rcon?) {
                rconDao.deleteRconHistory(ip)
            }
        }
    }
}