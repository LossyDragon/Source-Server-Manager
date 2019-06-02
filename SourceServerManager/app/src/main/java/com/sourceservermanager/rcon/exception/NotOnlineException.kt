package com.sourceservermanager.rcon.exception

class NotOnlineException(msg: String) : Exception(msg) {

    companion object {
        private const val serialVersionUID = 5263271934736639145L
    }

}