package com.garfiec.db.schema

import com.garfiec.db.schema.SessionDBService.Sessions
import com.garfiec.util.db.dbQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

typealias SessionUuid = UUID

class Session(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Session>(Sessions)

    var user by SessionDBService.Sessions.user
    var sessionUuid by SessionDBService.Sessions.sessionUuid
    var createdAt by SessionDBService.Sessions.createdAt
    var lastAccessedAt by SessionDBService.Sessions.lastAccessedAt
    var expiresAt by SessionDBService.Sessions.expiresAt
}

class SessionDBService(private val database: Database) {
    object Sessions : IntIdTable() {
        val user = reference("user_id", UserDBService.Users)
        val sessionUuid: Column<UUID> = uuid("session_uuid").autoGenerate().uniqueIndex()
        val createdAt = datetime("created_at").default(LocalDateTime.now())
        val lastAccessedAt = datetime("last_accessed_at").default(LocalDateTime.now())
        val expiresAt = datetime("expires_at").default(LocalDateTime.now().plusWeeks(1))
    }

    init {
        transaction(database) {
            SchemaUtils.create(Sessions)
        }
    }

    suspend fun updateLastAccessed(sessionUuid: UUID) {
        dbQuery(database) {
            val session = Sessions.select { Sessions.sessionUuid eq sessionUuid }.singleOrNull()
            session?.let {
                Sessions.update({ Sessions.sessionUuid eq sessionUuid }) {
                    it[lastAccessedAt] = LocalDateTime.now()
                }
            }
        }
    }

    suspend fun createSession(userId: Int): UUID {
        return dbQuery(database) {
            val session = Session.new {
                user = EntityID(userId, UserDBService.Users)
                lastAccessedAt = LocalDateTime.now()
                expiresAt = LocalDateTime.now().plusWeeks(1)
            }
            session.sessionUuid
        }
    }

    /**
     * Get the user associated with the session
     */
    suspend fun getUserId(sessionUuid: UUID): Int {
        return dbQuery(database) {
            val session = Session.find { Sessions.sessionUuid eq sessionUuid }.singleOrNull()
                ?: throw IllegalArgumentException("User session not found")

            session.user.value
        }
    }
}
