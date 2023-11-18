package com.garfiec

import com.garfiec.ktor.routes.accountRoutes
import com.garfiec.ktor.routes.dishBrowseRoutes
import com.garfiec.ktor.routes.merchantGroupRoutes
import com.garfiec.ktor.routes.customerOrderRoutes
import com.garfiec.plugins.*
import com.garfiec.poormandi.DI
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*

lateinit var di: DI
fun main() {
    di = DI()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    install(CallLogging)

    accountRoutes()
    merchantGroupRoutes()
    dishBrowseRoutes()
    customerOrderRoutes()

    configureTemplating()
    configureSecurity()
    configureRouting()
}
