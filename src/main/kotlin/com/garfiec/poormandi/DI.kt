package com.garfiec.poormandi

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import com.garfiec.db.schema.*
import com.garfiec.repository.filestore.MinioRepository
import com.garfiec.service.*
import com.garfiec.util.Environment
import org.jetbrains.exposed.sql.Database
import kotlin.time.Duration.Companion.seconds

class DI {
    private val environment = Environment()
    private val inMemDb = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
//    private val postgresDb = Database.connect(
//        url = "jdbc:postgresql://localhost:5432/hw-db",
//        user = "hw-user",
//        driver = "org.postgresql.Driver",
//        password = "bks8RatiyYKWg"
//    )
    private val database = inMemDb

    // Instantiate DB Services
    private val userDBService = UserDBService(database)
    private val sessionDBService = SessionDBService(database)
    private val imageDBService = ImageDBService(database)
    private val merchantGroupDBService = MerchantGroupDBService(database)
    private val merchantGroupMemberDBService = MerchantGroupMemberDBService(database)
    private val dishDBService = DishDBService(database)
    private val orderDBService = OrderDBService(database)

    // Internal Services
    private val s3Repository = MinioRepository(environment)
    private val openai = OpenAI(
        token = environment.openAiKey,
        timeout = Timeout(socket = 60.seconds),
    )


    // Instantiate Application Services
    val accountService = AccountService(
        userDBService = userDBService,
        sessionDBService = sessionDBService
    )

    val imageService = ImageService(
        userDBService = userDBService,
        imageDBService = imageDBService,
        s3Repository = s3Repository
    )

    val merchantGroupService = MerchantGroupService(
        userDBService = userDBService,
        merchantGroupDBService = merchantGroupDBService,
        merchantGroupMemberDBService = merchantGroupMemberDBService
    )

    val dishService = DishService(
        userDBService = userDBService,
        merchantGroupDBService = merchantGroupDBService,
        merchantGroupMemberDBService = merchantGroupMemberDBService,
        dishDBService = dishDBService,
        imageService = imageService
    )

    val dishGenAiService = DishGenAiService(
        openAI = openai,
        dishDBService = dishDBService,
        merchantGroupMemberDBService = merchantGroupMemberDBService,
        imageService = imageService
    )

    val customerOrderService = CustomerOrderService(
        orderDBService = orderDBService,
        dishDBService = dishDBService,
        merchantGroupMemberDBService = merchantGroupMemberDBService
    )

    val merchantOrderService = MerchantOrderService(
        orderDBService = orderDBService,
        dishDBService = dishDBService
    )

}
