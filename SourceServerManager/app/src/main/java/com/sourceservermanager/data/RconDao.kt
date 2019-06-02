package com.sourceservermanager.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RconDao {

    @Insert
    fun insert(chat: Rcon)

    @Query("DELETE FROM rcon_table")
    fun deleteAllRcon()

    @Query("DELETE FROM rcon_table WHERE rconIP=:ip")
    fun deleteRconHistory(ip: String)

    @Query("SELECT * FROM rcon_table WHERE rconIP=:ip ORDER BY rconTimestamp ASC")
    fun getRconHistory(ip: String): LiveData<List<Rcon>>

}