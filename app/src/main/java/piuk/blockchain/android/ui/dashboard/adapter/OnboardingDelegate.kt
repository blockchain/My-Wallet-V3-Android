package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.include_onboarding_complete.view.*
import kotlinx.android.synthetic.main.include_onboarding_viewpager.view.*
import kotlinx.android.synthetic.main.item_onboarding.view.*
import me.relex.circleindicator.CircleIndicator
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.LockableViewPager
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.onboarding.OnboardingPagerAdapter
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.setOnPageChangeListener

class OnboardingDelegate<in T>(
    private val context: Context
) : AdapterDelegate<T> {

    private val onboardingPagerAdapter by unsafeLazy {
        OnboardingPagerAdapter(
            context
        )
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is OnboardingModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        OnboardingViewHolder(parent.inflate(R.layout.item_onboarding))

    @Suppress("CascadeIf")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as OnboardingViewHolder

        val data = items[position] as OnboardingModel
        val pagerItems = data.pagerContent

        holder.viewPager.adapter = onboardingPagerAdapter
        holder.viewPager.setOnPageChangeListener {
            onPageScrolled { position, positionOffset ->
                val count = onboardingPagerAdapter.count
                if (position == count - 1) {
                    // Last page
                    holder.completeLayout.visible()
                    holder.viewPager.setPagingEnabled(false)
                    data.onboardingComplete()
                } else if (position == count - 2) {
                    // Second to last page
                    holder.completeLayout.visible()
                    holder.indicator.alpha = 1 - positionOffset
                    holder.skipAll.alpha = 1 - positionOffset
                    holder.completeLayout.alpha = positionOffset
                    data.onboardingNotComplete()
                } else {
                    holder.indicator.visible()
                    holder.skipAll.visible()
                    holder.completeLayout.invisible()
                    holder.indicator.alpha = 1.0f
                    holder.skipAll.alpha = 1.0f
                    data.onboardingNotComplete()
                }
            }
        }

        holder.skipAll.setOnClickListener { data.dismissOnboarding() }
        holder.closeButton.setOnClickListener { data.dismissOnboarding() }

        holder.startOver.setOnClickListener {
            holder.completeLayout.invisible()
            holder.viewPager.currentItem = 0
            holder.viewPager.setPagingEnabled(true)
            holder.indicator.visible()
            holder.skipAll.visible()
            holder.indicator.alpha = 1.0f
            holder.skipAll.alpha = 1.0f
            data.onboardingNotComplete()
        }

        onboardingPagerAdapter.notifyPagesChanged(pagerItems)

        holder.viewPager.post({
            holder.progressBar.gone()
        })

        holder.indicator.setViewPager(holder.viewPager)
    }

    private class OnboardingViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        // Root Layout
        internal var progressBar: ProgressBar = itemView.progress_bar_onboarding
        internal var completeLayout: View = itemView.onboarding_complete_layout
        // Complete Layout
        internal var closeButton: ImageButton = itemView.onboarding_close
        internal var startOver: TextView = itemView.button_start_over
        // ViewPager Layout
        internal var skipAll: TextView = itemView.btn_skip_all
        internal var viewPager: LockableViewPager = itemView.pager_onboarding
        internal var indicator: CircleIndicator = itemView.indicator
    }
}