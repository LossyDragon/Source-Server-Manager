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

    fun insert(note: Server) {
        val insertNoteAsyncTask = InsertNoteAsyncTask(serverDao).execute(note)
    }

    fun update(note: Server) {
        val updateNoteAsyncTask = UpdateNoteAsyncTask(serverDao).execute(note)
    }


    fun delete(note: Server) {
        val deleteNoteAsyncTask = DeleteNoteAsyncTask(serverDao).execute(note)
    }

    fun deleteAllNotes() {
        val deleteAllNotesAsyncTask = DeleteAllNotesAsyncTask(
                serverDao
        ).execute()
    }

    fun getAllServers(): LiveData<List<Server>> {
        return allServers
    }

    companion object {
        private class InsertNoteAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.insert(p0[0]!!)
            }
        }

        private class UpdateNoteAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.update(p0[0]!!)
            }
        }

        private class DeleteNoteAsyncTask(val serverDao: ServerDao) : AsyncTask<Server, Unit, Unit>() {
            override fun doInBackground(vararg p0: Server?) {
                serverDao.delete(p0[0]!!)
            }
        }

        private class DeleteAllNotesAsyncTask(val serverDao: ServerDao) : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                serverDao.deleteAllServers()
            }
        }
    }
}