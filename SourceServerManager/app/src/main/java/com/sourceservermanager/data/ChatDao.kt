package com.sourceservermanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChatDao {

    @Insert
    fun insert(chat: Chat)

    @Update
    fun update(chat: Chat)

    @Delete
    fun delete(chat: Chat)

    @Query("DELETE FROM chat_table")
    fun deleteAllChats()

    @Query("SELECT * FROM chat_table ORDER BY timestamp DESC")
    fun getAllChats(): LiveData<List<Chat>>

}