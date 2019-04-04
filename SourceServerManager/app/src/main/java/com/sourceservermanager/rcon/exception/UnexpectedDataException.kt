package com.sourceservermanager.rcon.exception

/**
 * This exception will be thrown whenever data was received that
 * didn't follow the prescribed format for the Rcon type.
 */
class UnexpectedDataException : Exception() {
    companion object {
        private const val serialVersionUID = 1L
    }

}
