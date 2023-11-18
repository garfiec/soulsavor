package com.garfiec.db.schema

import com.garfiec.util.db.dbQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

typealias MerchantGroupUuid = String

class MerchantGroup(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<MerchantGroup>(MerchantGroupDBService.MerchantGroups)
    val merchantGroupUuid by MerchantGroupDBService.MerchantGroups.merchantGroupUuid
    var owner by MerchantGroupDBService.MerchantGroups.owner
    var isPublic by MerchantGroupDBService.MerchantGroups.isPublic
    var groupName by MerchantGroupDBService.MerchantGroups.groupName
    var groupDescription by MerchantGroupDBService.MerchantGroups.groupDescription
    var groupPictureId by MerchantGroupDBService.MerchantGroups.groupPictureId
    var creditsName by MerchantGroupDBService.MerchantGroups.creditsName
    var defaultCreditAmount by MerchantGroupDBService.MerchantGroups.defaultCreditAmount
}

class MerchantGroupDBService(private val database: Database) {
    object MerchantGroups : IntIdTable() {
        val merchantGroupUuid: Column<UUID> = uuid("merchant_group_uuid").autoGenerate().uniqueIndex()
        val owner = reference("owner_id", UserDBService.Users)
        val isPublic = bool("is_public").default(false)
        val groupName = varchar("group_name", 255).uniqueIndex()
        val groupDescription = varchar("group_description", 1024)
        val groupPictureId = reference("group_picture_id", ImageDBService.Images).nullable()
        val creditsName = varchar("credits_name", 255).default("Credits")
        val defaultCreditAmount = integer("default_credit_amount").default(0)
    }

    init {
        transaction(database) {
            SchemaUtils.create(MerchantGroups)
        }
    }

    suspend fun createMerchantGroup(name: String, ownerId: Int, description: String = "", groupPictureId: Int? = null): MerchantGroup {
        return dbQuery(database) {
            MerchantGroup.new {
                owner = EntityID(ownerId, UserDBService.Users)
                groupName = name
                groupDescription = description
                if (groupPictureId != null) {
                    this.groupPictureId = EntityID(groupPictureId, ImageDBService.Images)
                }
            }
        }
    }

    suspend fun findGroupById(merchantGroupId: Int): MerchantGroup? {
        return dbQuery(database) {
            MerchantGroup.findById(merchantGroupId)
        }
    }

    suspend fun findGroupByUuid(merchantGroupUuid: MerchantGroupUuid): MerchantGroup? {
        return dbQuery(database) {
            MerchantGroup.find { MerchantGroups.merchantGroupUuid eq UUID.fromString(merchantGroupUuid) }.singleOrNull()
        }
    }

    suspend fun findGroupByName(name: String): MerchantGroup? {
        return dbQuery(database) {
            MerchantGroup.find { MerchantGroups.groupName eq name }.singleOrNull()
        }
    }

    suspend fun deleteGroup(groupId: Int) {
        dbQuery(database) {
            MerchantGroup.findById(groupId)?.delete()
        }
    }

    suspend fun updateGroupVisibility(groupId: Int, isPublic: Boolean) {
        dbQuery(database) {
            MerchantGroup.findById(groupId)?.apply {
                this.isPublic = isPublic
            }
        }
    }

    suspend fun updateGroupDetails(groupId: Int, newGroupName: String?, newDescription: String?, groupPictureId: Int?) {
        dbQuery(database) {
            MerchantGroup.findById(groupId)?.apply {
                newGroupName?.let { this.groupName = it }
                newDescription?.let { this.groupDescription = it }
                groupPictureId?.let { this.groupPictureId = EntityID(it, ImageDBService.Images) }
            }
        }
    }

    suspend fun getPublicGroups(): List<MerchantGroup> {
        return dbQuery(database) {
            MerchantGroup.find { MerchantGroups.isPublic eq true }.toList()
        }
    }

    suspend fun getVisibleGroupsForUser(userId: Int): List<MerchantGroup> {
        return dbQuery(database) {
            val userGroups = MerchantGroupMember.find { MerchantGroupMemberDBService.MerchantGroupMembers.user eq userId }.map { it.group }
            val groupsWithExistingMembership = userGroups.map { userGroup -> MerchantGroup.find { MerchantGroups.id eq userGroup } }.flatten()
            val groupsWithPublicVisibility = MerchantGroup.find { MerchantGroups.isPublic eq true }

            (groupsWithExistingMembership + groupsWithPublicVisibility).distinct()
        }
    }
}
