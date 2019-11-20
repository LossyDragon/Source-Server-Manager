package com.sourceservermanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [Server::class, Chat::class, Rcon::class], version = 1, exportSchema = false)
abstract class ApplicationDatabase : RoomDatabase() {
    val dbWriter: ExecutorService = Executors.newFixedThreadPool(4)

    abstract fun serverDao(): ServerDao
    abstract fun chatDao(): ChatDao
    abstract fun rconDao(): RconDao

    companion object {
        @Volatile
        private var instance: ApplicationDatabase? = null

        fun getInstance(context: Context): ApplicationDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ApplicationDatabase {
            return Room.databaseBuilder(context, ApplicationDatabase::class.java, "ssm_database").build()
        }

    }

}