package com.garfiec.db.schema

import com.garfiec.db.schema.ImageDBService.Images
import com.garfiec.db.schema.UserDBService.Users
import com.garfiec.util.db.dbQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

typealias GroupMembershipUuid = String
class MerchantGroupMember(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<MerchantGroupMember>(MerchantGroupMemberDBService.MerchantGroupMembers)

    var group by MerchantGroupMemberDBService.MerchantGroupMembers.group
    var user by MerchantGroupMemberDBService.MerchantGroupMembers.user
    val groupMembershipUuid by MerchantGroupMemberDBService.MerchantGroupMembers.groupMembershipUuid
    var merchantName by MerchantGroupMemberDBService.MerchantGroupMembers.merchantName
    val merchantPicture by MerchantGroupMemberDBService.MerchantGroupMembers.merchantPicture
    val credits by MerchantGroupMemberDBService.MerchantGroupMembers.credits
}

class MerchantGroupMemberDBService(private val database: Database) {
    object MerchantGroupMembers : IntIdTable() {
        val group = reference("group_id", MerchantGroupDBService.MerchantGroups)
        val user = reference("user_id", Users)
        val groupMembershipUuid: Column<UUID> = uuid("group_membership_uuid").autoGenerate().uniqueIndex()
        val merchantName = varchar("merchant_name", 255)
        val merchantPicture = reference("image_id", Images).nullable()
        val credits: Column<Int> = integer("credits").default(0)
        init {
            uniqueIndex(group, user)  // create a unique index on group and user
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(MerchantGroupMembers)
        }
    }

    suspend fun isMemberOfGroup(groupId: Int, userId: Int): Boolean {
        return dbQuery(database) {
            MerchantGroupMember.find {
                (MerchantGroupMembers.group eq groupId) and (MerchantGroupMembers.user eq userId)
            }.singleOrNull() != null
        }
    }

    suspend fun addMemberToGroup(groupId: Int, userId: Int, defaultMerchantName: String = ""): MerchantGroupMember {
        return dbQuery(database) {
            MerchantGroupMember.new {
                group = EntityID(groupId, MerchantGroupDBService.MerchantGroups)
                user = EntityID(userId, Users)
                merchantName = defaultMerchantName
            }
        }
    }

    suspend fun removeMemberFromGroup(groupId: Int, userId: Int) {
        dbQuery(database) {
            MerchantGroupMember.find {
                (MerchantGroupMembers.group eq groupId) and (MerchantGroupMembers.user eq userId)
            }.singleOrNull()?.delete()
        }
    }

    suspend fun getMember(userId: Int, groupId: Int): MerchantGroupMember? {
        return dbQuery(database) {
            MerchantGroupMember.find {
                (MerchantGroupMembers.group eq groupId) and (MerchantGroupMembers.user eq userId)
            }.singleOrNull()
        }
    }

    suspend fun getMember(groupMembershipUuid: GroupMembershipUuid): MerchantGroupMember? {
        return dbQuery(database) {
            MerchantGroupMember.find {
                MerchantGroupMembers.groupMembershipUuid eq UUID.fromString(groupMembershipUuid)
            }.singleOrNull()
        }
    }

    suspend fun getMember(merchantGroupId: Int): MerchantGroupMember? {
        return dbQuery(database) {
            MerchantGroupMember.find {
                MerchantGroupMembers.group eq merchantGroupId
            }.singleOrNull()
        }
    }

    suspend fun getMembersOfGroup(groupId: Int): List<User> {
        return dbQuery(database) {
            (MerchantGroupMembers innerJoin Users)
                .select { MerchantGroupMembers.group eq groupId }
                .mapNotNull { User.wrapRow(it) }
        }
    }

    suspend fun getMembersOfGroupWithMerchant(groupId: Int): List<Pair<User, MerchantGroupMember>> {
        return dbQuery(database) {
            (MerchantGroupMembers innerJoin Users)
                .select { MerchantGroupMembers.group eq groupId }
                .mapNotNull { row ->
                    val user = User.wrapRow(row)
                    val member = MerchantGroupMember.wrapRow(row)
                    Pair(user, member)
                }
        }
    }

    suspend fun getGroupsOfUser(userId: Int): List<MerchantGroup> {
        return dbQuery(database) {
            (MerchantGroupMembers innerJoin MerchantGroupDBService.MerchantGroups)
                .select { MerchantGroupMembers.user eq userId }
                .mapNotNull { MerchantGroup.wrapRow(it) }
        }
    }

    suspend fun updateMerchantName(groupId: Int, userId: Int, newMerchantName: String) {
        dbQuery(database) {
            MerchantGroupMember.find {
                (MerchantGroupMembers.group eq groupId) and (MerchantGroupMembers.user eq userId)
            }.singleOrNull()?.apply {
                this.merchantName = newMerchantName
            }
        }
    }

    suspend fun getMerchantName(groupId: Int, userId: Int): String? {
        return dbQuery(database) {
            MerchantGroupMember.find {
                (MerchantGroupMembers.group eq groupId) and (MerchantGroupMembers.user eq userId)
            }.singleOrNull()?.merchantName
        }
    }
}
