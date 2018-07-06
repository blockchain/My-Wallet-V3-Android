package piuk.blockchain.android.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * This delegate provides methods to hook in this delegate to [RecyclerView.Adapter] lifecycle.
 * This hook is provided by [AdapterDelegatesManager].
 *
 * @param T The type of the data source
 */
interface AdapterDelegate<in T> {

    /**
     * Determines whether or not this [AdapterDelegate] is responsible for the given data element
     *
     * @param items The adapter's data source
     * @param position The current position in the data source
     */
    fun isForViewType(items: List<T>, position: Int): Boolean

    /**
     * Creates the [RecyclerView.ViewHolder] for the given data source item
     *
     * @param parent The ViewGroup parent of the given data source
     *
     * @return The newly instantiated [RecyclerView.ViewHolder]
     */
    fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    /**
     * Called to bind the [RecyclerView.ViewHolder] to the item of the data source set
     *
     * @param items The adapter's data source
     * @param position The current position in the data source
     *
     * @param holder The [RecyclerView.ViewHolder] to bind
     */
    fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    )
}