package com.sourceservermanager.rcon.exception

class NotOnlineException : Exception {

    constructor(msg: String) : super(msg)

    constructor(msg: String, exception: Exception) : super(msg, exception)

    companion object {

        private const val serialVersionUID = 5263271934736639145L
    }

}