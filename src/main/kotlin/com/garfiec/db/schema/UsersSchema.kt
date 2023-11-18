package com.garfiec.db.schema

import com.garfiec.util.db.dbQuery
import com.garfiec.util.security.hashPassword
import com.garfiec.util.security.verifyPassword
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import java.util.*


typealias UserUuid = String

@Serializable
data class ExposedUser(val username: String, val password: String)


@Serializable
data class LoginUserRequest(val username: String, val password: String)

@Serializable
data class LoginUserResponse(val userUuid: String)

data class LoggedInUser(
    val userId: Int,
    val userUuid: UUID,
    val firstName: String,
    val lastName: String
) {
    companion object {
        fun fromUser(user: User): LoggedInUser = LoggedInUser(
            userId = user.id.value,
            userUuid = user.userUuid,
            firstName = user.firstName,
            lastName = user.lastName
        )
    }
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UserDBService.Users)

    var userUuid by UserDBService.Users.userUuid
    var username by UserDBService.Users.username
    var email by UserDBService.Users.email
    var hashedPassword by UserDBService.Users.hashedPassword
    var firstName by UserDBService.Users.firstName
    var lastName by UserDBService.Users.lastName
    var googleAccountId by UserDBService.Users.googleAccountId
}
class UserDBService(private val database: Database) {
    object Users : IntIdTable() {
//        override val id: Column<EntityID<Int>> = integer("id").autoIncrement()
        val userUuid: Column<UUID> = uuid("user_uuid").autoGenerate().uniqueIndex()
        val username = varchar("username", length = 50).uniqueIndex()
        val email = varchar("email", 255).uniqueIndex().default("")
        val hashedPassword = varchar("hashed_password", length = 255)
        val firstName = varchar("firstname", length = 50).default("")
        val lastName = varchar("lastname", length = 50).default("")
//        val age = integer("age")
        val googleAccountId = varchar("google_account_id", 255).nullable().uniqueIndex() // Null if not signed up with Google

//        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun doesUserExist(username: String): Boolean {
        val user = dbQuery(database) {
            User.find { Users.username eq username }.singleOrNull()
        }

        return user != null
    }

    suspend fun createUser(
        requestedUsername: String,
        password: String,
        firstName: String = "",
        lastName: String = "",
    ) = dbQuery(database) {
        if (doesUserExist(requestedUsername)) throw IllegalArgumentException("Username already taken")

        val user = User.new {
            username = requestedUsername
            hashedPassword = hashPassword(password)
            this.firstName = firstName
            this.lastName = lastName
        }
        user
    }

    suspend fun getUser(userId: Int): User {
        return dbQuery(database) {
            User.find { Users.id eq userId }.singleOrNull()
        } ?: throw IllegalArgumentException("User not found")
    }

    suspend fun getUser(userUuid: UserUuid): User {
        return dbQuery(database) {
            User.find { Users.userUuid eq UUID.fromString(userUuid) }.singleOrNull()
        } ?: throw IllegalArgumentException("User not found")
    }

    suspend fun getUser(username: String, password: String): User {
        val user = dbQuery(database) {
            User.find { Users.username eq username }.singleOrNull()
        } ?: throw IllegalArgumentException("User not found or incorrect password")

        if (!verifyPassword(password, user.hashedPassword)) {
            throw IllegalArgumentException("User not found or incorrect password")
        }

        return user
    }

//    suspend fun read(id: Int): ExposedUser? {
//        return dbQuery {
//            Users.select { Users.id eq id }
//                .map { ExposedUser(it[Users.name], it[Users.age]) }
//                .singleOrNull()
//        }
//    }

//    suspend fun update(id: Int, user: ExposedUser) {
//        dbQuery {
//            Users.update({ Users.id eq id }) {
//                it[name] = user.name
//                it[age] = user.age
//            }
//        }
//    }

    suspend fun delete(id: Int) {
        dbQuery(database) {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }
}
