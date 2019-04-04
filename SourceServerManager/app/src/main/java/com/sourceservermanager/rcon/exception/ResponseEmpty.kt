package com.sourceservermanager.rcon.exception

/**
 * This exception will be throwed whenever the server don't send a response to our command
 */
class ResponseEmpty : Exception() {
    companion object {
        private const val serialVersionUID = 1L
    }

}
