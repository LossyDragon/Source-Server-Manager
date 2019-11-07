package com.sourceservermanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Server::class, Chat::class, Rcon::class], version = 1, exportSchema = false)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao
    abstract fun chatDao(): ChatDao
    abstract fun rconDao(): RconDao

    companion object {
        private var instance: ApplicationDatabase? = null

        fun getInstance(context: Context): ApplicationDatabase? {
            if (instance == null) {
                synchronized(ApplicationDatabase::class) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            ApplicationDatabase::class.java, "ssm_database")
                            .fallbackToDestructiveMigration() //Delete all if DB change
                            .build()
                }
            }
            return instance
        }

        //fun destroyInstance() {
        //    instance = null
        //}

    }

}