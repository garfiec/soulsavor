package com.garfiec.util.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> dbQuery(
    db: Database?,
    block: suspend () -> T
): T =
    newSuspendedTransaction(Dispatchers.IO, db) { block() }

suspend fun <T> dbQueryAsync(
    db: Database?,
    block: suspend () -> T
): Deferred<T> = CoroutineScope(Dispatchers.IO).async {
    dbQuery(db, block)
}
