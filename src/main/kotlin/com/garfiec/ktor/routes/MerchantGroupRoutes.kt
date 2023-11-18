package com.garfiec.ktor.routes

import com.garfiec.di
import com.garfiec.service.MerchantGroupCreateRequest
import com.garfiec.util.ktor.performApiRequest
import com.garfiec.util.ktor.requireLoggedIn
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.merchantGroupRoutes() {
    routing {
        // createMerchantGroup
        post("/group/create") {
            requireLoggedIn {
                performApiRequest {
                    val merchantGroupCreateRequest = call.receive<MerchantGroupCreateRequest>()
                    di.merchantGroupService.createMerchantGroup(this, merchantGroupCreateRequest)
                }
            }
        }

        // getMerchantGroup
        get("/group/{groupId}") {
            requireLoggedIn {
                performApiRequest {
                    val groupId = call.parameters["groupId"] ?: throw IllegalArgumentException("Invalid group ID")
                    di.merchantGroupService.getMerchantGroup(groupId)
                }
            }
        }

        // findGroupByName

        // deleteGroup

        // updateGroupDetails

        // addMemberToGroup
        post("/group/addMember/{groupId}/{userId}") {
            requireLoggedIn {
                performApiRequest {
                    val groupId = call.parameters["groupId"] ?: throw IllegalArgumentException("Invalid group ID")
                    val userUuid = call.parameters["userId"] ?: throw IllegalArgumentException("Invalid user ID")
                    di.merchantGroupService.addMemberToGroup(
                        loggedInUser = this,
                        merchantGroupUuid = groupId,
                        newMemberUuid = userUuid
                    )
                }
            }
        }

        // removeMemberFromGroup
        delete("/group/removeMember/{groupId}/{userId}") {
            requireLoggedIn {
                performApiRequest {
                    val groupId = call.parameters["groupId"] ?: throw IllegalArgumentException("Invalid group ID")
                    val userUuid = call.parameters["userId"] ?: throw IllegalArgumentException("Invalid user ID")
                    di.merchantGroupService.removeMemberFromGroup(
                        loggedInUser = this,
                        merchantGroupUuid = groupId,
                        userToRemoveUuid = userUuid
                    )
                }
            }
        }

        // getMembersOfGroup
        get("/group/getMembers/{groupId}") {
            requireLoggedIn {
                performApiRequest {
                    val groupId = call.parameters["groupId"] ?: throw IllegalArgumentException("Invalid group ID")
                    di.merchantGroupService.getMembersOfGroup(loggedInUser = this, merchantGroupUuid = groupId)
                }
            }
        }

        // getGroupsOfUser
        get("/group/getGroupsOfUser/{userId}") {
            requireLoggedIn {
                performApiRequest {
                    val userUuid = call.parameters["userId"] ?: throw IllegalArgumentException("Invalid user ID")
                    di.merchantGroupService.getGroupsOfUser(userUuid = userUuid)
                }
            }
        }

        // list groups
        get("/group/list") {
            requireLoggedIn {
                performApiRequest {
                    di.merchantGroupService.listAllVisibleMerchantGroups(loggedInUser = this)
                }
            }
        }

        // Make group public/private
        patch("/group/setVisibility/{groupId}/{visibility}") {
            requireLoggedIn {
                performApiRequest {
                    val groupId = call.parameters["groupId"] ?: throw IllegalArgumentException("Invalid group ID")
                    val visibility = call.parameters["visibility"] ?: throw IllegalArgumentException("Invalid visibility")
                    val visibilityBoolean = visibility.toBoolean()
                    di.merchantGroupService.updateGroupVisibility(loggedInUser = this, groupId, visibilityBoolean)
                }
            }
        }
    }
}
