package piuk.blockchain.android.ui.adapters

import android.support.v4.util.SparseArrayCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * This class manages the [AdapterDelegate] objects and delegates functions to the appropriate
 * delegates for the current object type. Register and remove [AdapterDelegate] objects here, and
 * call the appropriate functions from your [RecyclerView.Adapter].
 *
 * @param T The type of the data source
 */
class AdapterDelegatesManager<T> {

    private val arrayList = ArrayList<Any>()

    private var delegates: SparseArrayCompat<AdapterDelegate<T>> = SparseArrayCompat()

    /**
     * Adds an [AdapterDelegate] to the current list of delegates and returns this
     * [AdapterDelegatesManager].
     */
    fun addAdapterDelegate(delegate: AdapterDelegate<T>) {
        var viewType = delegates.size()
        while (delegates[viewType] != null) {
            viewType++
        }

        delegates.put(viewType, delegate)
    }

    /**
     * Removes an [AdapterDelegate] from the current list of delegates and returns this
     * [AdapterDelegatesManager].
     */
    fun removeAdapterDelegate(delegate: AdapterDelegate<T>) {
        val index = delegates.indexOfValue(delegate)
        delegates.removeAt(index)
    }

    /**
     * Gets the current item's View type by checking against the list of available [AdapterDelegate]
     * objects. If no delegate is found to handle the particular object type, this function will
     * throw a [NullPointerException].
     *
     * @param items The list of items currently being displayed by the adapter
     * @param position The position of the current item
     *
     * @return The item View type as an [Int]
     */
    fun getItemViewType(items: List<T>, position: Int): Int {
        for (i in 0 until delegates.size()) {
            val delegate = delegates.get(i)
            if (delegate.isForViewType(items, position)) {
                return delegates.keyAt(i)
            }
        }

        throw NullPointerException("Delegate not found for item at position $position.")
    }

    /**
     * Delegates the creation of the [RecyclerView.ViewHolder] to one of the registered delegates.
     * This must be called by the adapter. If a suitable [AdapterDelegate] is not found, this
     * function will throw a [NullPointerException].
     *
     * @param parent The [ViewGroup] parent of the [RecyclerView]
     * @param viewType The View type of the current object
     *
     * @return An appropriate [RecyclerView.ViewHolder] for the item at the current position
     */
    fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        if (parent == null) throw NullPointerException("ViewGroup parent was null")

        val delegate = getDelegateForViewType(viewType)
            ?: throw NullPointerException("No delegate found for view type $viewType")

        return delegate.onCreateViewHolder(parent)
    }

    /**
     * Delegates the actual binding of the [RecyclerView.ViewHolder] to a suitable delegate if
     * found. This must be called by the adapter. If a suitable [AdapterDelegate] is not found, this
     * function will throw a [NullPointerException].
     *
     * @param items The list of items to be displayed by the adapter
     * @param position The position of the current item
     * @param viewHolder The [RecyclerView.ViewHolder] for the current item
     * @param payloads A list of payloads, sometimes provided by the [RecyclerView]
     */
    fun onBindViewHolder(
        items: List<T>,
        position: Int,
        viewHolder: RecyclerView.ViewHolder?,
        payloads: List<*>?
    ) {
        if (viewHolder == null) throw NullPointerException("ViewHolder was null")

        val delegate = getDelegateForViewType(viewHolder.itemViewType)
            ?: throw NullPointerException(
                "No delegate found for item at position $position for view type  ${viewHolder.itemViewType}"
            )

        delegate.onBindViewHolder(items, position, viewHolder, payloads ?: arrayList)
    }

    /**
     * Delegates the actual binding of the [RecyclerView.ViewHolder] to a suitable delegate if
     * found. This must be called by the adapter. If a suitable [AdapterDelegate] is not found, this
     * function will throw a [NullPointerException].
     *
     * @param items The list of items to be displayed by the adapter
     * @param position The position of the current item
     * @param viewHolder The [RecyclerView.ViewHolder] for the current item
     */
    fun onBindViewHolder(items: List<T>, position: Int, viewHolder: RecyclerView.ViewHolder?) =
        onBindViewHolder(items, position, viewHolder, arrayList)

    /**
     * Returns the [AdapterDelegate] for a given view type
     *
     * @param viewType The current View type as an [Int]
     *
     * @return A suitable [AdapterDelegate] for the View type, or null if not found
     */
    fun getDelegateForViewType(viewType: Int): AdapterDelegate<T>? = delegates.get(viewType)

    /**
     * Returns the view type for a given [AdapterDelegate]
     *
     * @param delegate An [AdapterDelegate]
     *
     * @return An [Int] representing the View type, or -1 if not found
     */
    fun getViewType(delegate: AdapterDelegate<T>): Int {
        val index = delegates.indexOfValue(delegate)
        if (index == -1) {
            return -1
        }
        return delegates.keyAt(index)
    }
}