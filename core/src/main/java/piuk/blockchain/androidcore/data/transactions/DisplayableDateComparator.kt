package piuk.blockchain.androidcore.data.transactions

import piuk.blockchain.androidcore.data.transactions.models.Displayable
import java.util.Comparator

class DisplayableDateComparator : Comparator<Displayable> {

    override fun compare(t1: Displayable, t2: Displayable): Int {

        val before = -1
        val equal = 0
        val after = 1

        return when {
            t1.timeStamp > t2.timeStamp -> before
            t1.timeStamp < t2.timeStamp -> after
            else -> equal
        }
    }
}
