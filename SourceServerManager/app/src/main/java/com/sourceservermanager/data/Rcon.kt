package com.sourceservermanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rcon_table")
data class Rcon(
        var rconTitle: String,
        var rconIP: String,
        var rconMessage: String,
        var rconTimestamp: String

)
{
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}