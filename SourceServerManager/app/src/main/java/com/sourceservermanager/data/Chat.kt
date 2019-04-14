package com.sourceservermanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Object which holds the data parsed from a chat message sent by the Chat Relay.
 *
 * @param protocolVersion The Chat Relay protocol version used by the message packet
 * @param sayTeamFlag Whether or not this is a say_team message (0x00 = say, 0x01 = say_team)
 * @param serverTimestamp A timestamp in Unix epoch format added by the Chat Relay server
 * @param gameServerIP The IP address of the game server from which the message originated
 * @param gameServerPort The port of the game server from which the message originated
 * @param messageTimestamp The original timestamp included in the message
 * @param playerName The name of the player who sent the message
 * @param playerTeam The team of the player who sent the message
 * @param message The text of the message
 */

@Entity(tableName = "chat_table")
data class Chat(
        var protocolVersion: Byte,
        var sayTeamFlag: Byte,
        var serverTimestamp: Int,
        var gameServerIP: String,
        var gameServerPort: String,
        var messageTimestamp: String,
        var playerName: String,
        var playerTeam: String,
        var message: String
)
{
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}


