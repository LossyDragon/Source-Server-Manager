package com.sourceservermanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ServerDao {

    @Insert
    fun insert(note: Server)

    @Update
    fun update(note: Server)

    @Delete
    fun delete(note: Server)

    @Query("DELETE FROM server_table")
    fun deleteAllServers()

    @Query("SELECT * FROM server_table ORDER BY serverTitle DESC")
    fun getAllServers(): LiveData<List<Server>>

}