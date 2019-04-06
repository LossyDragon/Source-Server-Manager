package com.sourceservermanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ServerDao {

    @Insert
    fun insert(server: Server)

    @Update
    fun update(server: Server)

    @Delete
    fun delete(server: Server)

    @Query("DELETE FROM server_table")
    fun deleteAllServers()

    @Query("SELECT * FROM server_table ORDER BY serverTitle DESC")
    fun getAllServers(): LiveData<List<Server>>

}