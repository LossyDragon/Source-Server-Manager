package com.sourceservermanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChatDao {

    @Insert
    fun insert(chat: Chat)

    @Query("DELETE FROM chat_table")
    fun deleteAllChats()

    @Query("DELETE FROM chat_table WHERE gameServerIP=:ip")
    fun deleteChatHistory(ip: String)

    @Query("SELECT * FROM chat_table WHERE gameServerIP=:ip ORDER BY messageTimestamp ASC")
    fun getChatHistory(ip: String): LiveData<List<Chat>>

}