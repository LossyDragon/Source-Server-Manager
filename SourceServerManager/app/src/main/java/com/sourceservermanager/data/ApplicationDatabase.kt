package com.sourceservermanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Server::class], version = 1)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao


    companion object {
        private var instance: ApplicationDatabase? = null

        fun getInstance(context: Context): ApplicationDatabase? {
            if (instance == null) {
                synchronized(ApplicationDatabase::class) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            ApplicationDatabase::class.java, "ssm_database")
                            //.fallbackToDestructiveMigration()
                            .build()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }

    }

}