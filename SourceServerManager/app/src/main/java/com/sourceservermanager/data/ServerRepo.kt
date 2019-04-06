package com.sourceservermanager.data

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

class ServerRepo(application: Application) {

    private var serverDao: ServerDao

    private var allServers: LiveData<List<Server>>

    init {
        val database: ApplicationDatabase = ApplicationDatabase.getInstance(
                application.applicationContext
        )!!
        serverDao = database.serverDao()
        allServers = serverDao.getAllServers()
    }

    fun insert(server: Server) {
        InsertServerAsyncTask(serverDao).execute(server)
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

    fun getAllServers(): LiveData<List<Server>> {
        return allServers
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
    }
}