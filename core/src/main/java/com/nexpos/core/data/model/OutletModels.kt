package com.nexpos.core.data.model

data class CreateOutletRequest(
    val name: String
)

data class UpdateOutletRequest(
    val name: String
)

data class OutletResponse(
    val outlet: OutletInfo
)

data class OutletListResponse(
    val outlets: List<OutletInfo>
)
