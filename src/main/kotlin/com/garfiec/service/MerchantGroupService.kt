package com.garfiec.service

import com.garfiec.db.schema.*
import com.garfiec.util.exception.ApiRequestException
import kotlinx.serialization.Serializable

@Serializable
data class MerchantGroupCreateRequest(val groupName: String, val description: String)
@Serializable
data class MerchantGroupCreateResponse(
    val merchantGroupId: MerchantGroupUuid,
    val groupMembershipId: GroupMembershipUuid
)
@Serializable
data class MerchantGroupDetailsRequest(val merchantGroupUuid: MerchantGroupUuid)
@Serializable
data class MerchantGroupDetailsResponse(
    val merchantGroupId: MerchantGroupUuid,
    val ownerUuid: UserUuid,
    val groupName: String,
    val description: String,
    val groupImageUrl: String? = null
)
@Serializable
data class MerchantGroupMembersRequest(val merchantGroupId: MerchantGroupUuid)
@Serializable
data class MerchantGroupMembersResponse(val members: List<MerchantGroupMemberResponseMember>)
@Serializable
data class MerchantGroupMemberResponseMember(
    val userUuid: UserUuid,
    val firstName: String,
    val lastName: String,
    val groupMembershipId: GroupMembershipUuid,
    val merchantName: String,
    val merchantPictureUrl: String
)

@Serializable
data class MerchantGroupsOfUserRequest(val userUuid: UserUuid)
@Serializable
data class MerchantGroupsListResponse(val groups: List<MerchantGroupDetailsResponse>)

class MerchantGroupService(
    private val userDBService: UserDBService,
    private val merchantGroupDBService: MerchantGroupDBService,
    private val merchantGroupMemberDBService: MerchantGroupMemberDBService
) {
    // createMerchantGroup - Maybe add ability to add a group picture in a single request?
    suspend fun createMerchantGroup(loggedInUser: LoggedInUser, merchantGroupCreateRequest: MerchantGroupCreateRequest): MerchantGroupCreateResponse {
        return try {
            // Create merchant group
            val merchantGroup = merchantGroupDBService.createMerchantGroup(
                name = merchantGroupCreateRequest.groupName,
                ownerId = loggedInUser.userId,
                description = merchantGroupCreateRequest.description
            )

            // Add owner to group members list
            val userMembership = merchantGroupMemberDBService.addMemberToGroup(
                groupId = merchantGroup.id.value,
                userId = loggedInUser.userId,
                defaultMerchantName = "${loggedInUser.firstName}'s Store"
            )

            MerchantGroupCreateResponse(
                merchantGroupId = merchantGroup.merchantGroupUuid.toString(),
                groupMembershipId = userMembership.groupMembershipUuid.toString()
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    suspend fun getMerchantGroup(merchantGroupUuid: MerchantGroupUuid): MerchantGroupDetailsResponse {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(merchantGroupUuid) ?: throw ApiRequestException("Merchant group not found")
            val user = userDBService.getUser(merchantGroup.owner.value)

            MerchantGroupDetailsResponse(
                merchantGroupId = merchantGroup.merchantGroupUuid.toString(),
                ownerUuid = user.userUuid.toString(),
                groupName = merchantGroup.groupName,
                description = merchantGroup.groupDescription,
                groupImageUrl = null
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // findGroupByName

    // deleteGroup

    // updateGroupDetails

    // addMemberToGroup
    suspend fun addMemberToGroup(loggedInUser: LoggedInUser, merchantGroupUuid: MerchantGroupUuid, newMemberUuid: UserUuid): MerchantGroupMemberResponseMember {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(merchantGroupUuid) ?: throw ApiRequestException("Merchant group not found")

            // In the future, we can add more ways to add members to a group
            if (merchantGroup.owner.value != loggedInUser.userId) throw ApiRequestException("You are not the owner of this group")

            // Check if user exists
            val newMember = userDBService.getUser(newMemberUuid) // If user doesn't exist, an exception is thrown

            val groupMembership = merchantGroupMemberDBService.addMemberToGroup(
                groupId = merchantGroup.id.value,
                userId = newMember.id.value,
                defaultMerchantName = "${loggedInUser.firstName}'s Store"
            )
            MerchantGroupMemberResponseMember(
                userUuid = newMember.userUuid.toString(),
                firstName = newMember.firstName,
                lastName = newMember.lastName,
                groupMembershipId = groupMembership.groupMembershipUuid.toString(),
                merchantName = groupMembership.merchantName,
                merchantPictureUrl = ""
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // removeMemberFromGroup
    suspend fun removeMemberFromGroup(loggedInUser: LoggedInUser, merchantGroupUuid: MerchantGroupUuid, userToRemoveUuid: UserUuid) {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(merchantGroupUuid)
                ?: throw ApiRequestException("Merchant group not found")

            // In the future, we can add more ways to remove members from a group
            if (merchantGroup.owner.value != loggedInUser.userId) throw ApiRequestException("You are not the owner of this group")

            // Check if user to remove exists
            val memberToRemove =
                userDBService.getUser(userToRemoveUuid) // If user doesn't exist, an exception is thrown

            merchantGroupMemberDBService.removeMemberFromGroup(merchantGroup.id.value, memberToRemove.id.value)
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // getMembersOfGroup
    suspend fun getMembersOfGroup(loggedInUser: LoggedInUser, merchantGroupUuid: MerchantGroupUuid): MerchantGroupMembersResponse {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(merchantGroupUuid)
                ?: throw ApiRequestException("Merchant group not found")

            val isMemberOfGroup = merchantGroupMemberDBService.isMemberOfGroup(merchantGroup.id.value, loggedInUser.userId)

            if (!isMemberOfGroup && !merchantGroup.isPublic) throw ApiRequestException("You are not a member of this group")

            val members = merchantGroupMemberDBService.getMembersOfGroupWithMerchant(merchantGroup.id.value)
            MerchantGroupMembersResponse(
                members = members.map { (user, merchantGroupMember) ->
                    MerchantGroupMemberResponseMember(
                        userUuid = user.userUuid.toString(),
                        firstName = user.firstName,
                        lastName = user.lastName,
                        groupMembershipId = merchantGroupMember.groupMembershipUuid.toString(),
                        merchantName = merchantGroupMember.merchantName,
                        merchantPictureUrl = "",
                    )
                }
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // getGroupsOfUser
    suspend fun getGroupsOfUser(userUuid: UserUuid): MerchantGroupsListResponse {
        return try {
            val user = userDBService.getUser(userUuid)
            val groups = merchantGroupMemberDBService.getGroupsOfUser(user.id.value)

            val merchantGroups = groups.map { merchantGroup ->
                MerchantGroupDetailsResponse(
                    merchantGroupId = merchantGroup.merchantGroupUuid.toString(),
                    ownerUuid = user.userUuid.toString(),
                    groupName = merchantGroup.groupName,
                    description = merchantGroup.groupDescription,
                    groupImageUrl = null
                )
            }

            MerchantGroupsListResponse(groups = merchantGroups)
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // list groups
    suspend fun listAllVisibleMerchantGroups(loggedInUser: LoggedInUser): MerchantGroupsListResponse {
        return try {
            // Get public groups
            val publicGroups = merchantGroupDBService
                .getVisibleGroupsForUser(loggedInUser.userId)
                .map { merchantGroup ->
                    // TODO This will need optimization in the future since this is costly
                    val groupOwnerUuid = userDBService.getUser(merchantGroup.owner.value).userUuid.toString()

                    MerchantGroupDetailsResponse(
                        merchantGroupId = merchantGroup.merchantGroupUuid.toString(),
                        ownerUuid = groupOwnerUuid,
                        groupName = merchantGroup.groupName,
                        description = merchantGroup.groupDescription,
                        groupImageUrl = ""
                    )
                }
            MerchantGroupsListResponse(groups = publicGroups)
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // Make group public/private
    suspend fun updateGroupVisibility(loggedInUser: LoggedInUser, merchantGroupUuid: MerchantGroupUuid, isPublic: Boolean) {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(merchantGroupUuid)
                ?: throw ApiRequestException("Merchant group not found")

            // In the future, we can add more ways to remove members from a group
            if (merchantGroup.owner.value != loggedInUser.userId) throw ApiRequestException("You are not the owner of this group")

            merchantGroupDBService.updateGroupVisibility(merchantGroup.id.value, isPublic)
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }

    // updateMerchantNameInGroup
    suspend fun updateMerchantName(loggedInUser: LoggedInUser, newMerchantName: String) {
        return try {
            val merchantGroup = merchantGroupDBService.findGroupByUuid(loggedInUser.userUuid.toString())
                ?: throw ApiRequestException("Merchant group not found")

            merchantGroupMemberDBService.updateMerchantName(
                groupId = merchantGroup.id.value,
                userId = loggedInUser.userId,
                newMerchantName = newMerchantName
            )
        } catch (e: Exception) {
            throw ApiRequestException(e.message.orEmpty())
        }
    }
}
