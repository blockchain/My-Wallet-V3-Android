package piuk.blockchain.android.data.websocket

import info.blockchain.balance.CryptoCurrency
import okhttp3.WebSocket
import timber.log.Timber
import java.util.HashSet

/**
 * Websocket status code as defined by [Section 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4)
 */
private const val STATUS_CODE_NORMAL_CLOSURE = 1000

class CurrencySocket(val currency: CryptoCurrency, private var socket: WebSocket) {

    private val subHashSet = HashSet<String>()

    fun sendToConnection(message: String) {
        // Make sure each message is only sent once per socket lifetime
        if (subHashSet.contains(message)) return
        try {
            socket.send(message)
            subHashSet.add(message)
        } catch (e: Exception) {
            Timber.e(e, "Send to ${currency.symbol} websocket failed")
        }
    }

    fun stop() {
        socket.close(STATUS_CODE_NORMAL_CLOSURE, "${currency.symbol} Websocket deliberately stopped")
    }
}