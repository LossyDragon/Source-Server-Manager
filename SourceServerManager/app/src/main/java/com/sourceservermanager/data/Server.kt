package com.sourceservermanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_table")
data class Server(
        var serverTitle: String,
        var serverIP: String,
        var serverPort: String,
        var serverPassword: String
)
{
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}