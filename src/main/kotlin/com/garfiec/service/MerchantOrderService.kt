package com.garfiec.service

import com.garfiec.db.schema.DishDBService
import com.garfiec.db.schema.OrderDBService

class MerchantOrderService(
    private val orderDBService: OrderDBService,
    private val dishDBService: DishDBService,
) {

}
