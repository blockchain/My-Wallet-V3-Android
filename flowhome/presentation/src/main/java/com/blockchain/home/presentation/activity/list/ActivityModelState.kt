package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent

data class ActivityModelState(
    val activity: DataResource<Map<TransactionGroup, List<ActivityComponent>>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState
