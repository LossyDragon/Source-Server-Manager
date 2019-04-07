package com.sourceservermanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_table")
data class Chat(
        var timestamp: String,
        var playerName: String,
        var playerTeam: String,
        var message: String,
        var sayTeamFlag: String //?

)
{
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}