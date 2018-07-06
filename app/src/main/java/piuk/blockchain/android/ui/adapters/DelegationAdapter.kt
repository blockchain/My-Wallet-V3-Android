package piuk.blockchain.android.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

/**
 * An abstract class which delegates all important functions to registered [AdapterDelegate]
 * objects. Extend this class as necessary and simply add [AdapterDelegate] objects to the
 * [AdapterDelegatesManager] to handle the different [View] types.
 *
 * @param T The type of object being held in the adapter's [List]
 */
abstract class DelegationAdapter<T> constructor(
    protected var delegatesManager: AdapterDelegatesManager<T>,
    open var items: List<T>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        delegatesManager.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        delegatesManager.onBindViewHolder(items, position, holder, null)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) =
        delegatesManager.onBindViewHolder(items, position, holder, payloads)

    override fun getItemViewType(position: Int): Int =
        delegatesManager.getItemViewType(items, position)
}