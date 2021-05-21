@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.graphql.routing.Bio
import com.neelkamath.omniChatBackend.graphql.routing.Name
import com.neelkamath.omniChatBackend.graphql.routing.Username

interface AccountData {
    /** The [Users.id]. */
    val id: Int

    fun getUserId(): Int = id

    fun getUsername(): Username = Users.readUsername(id)

    fun getEmailAddress(): String = Users.readEmailAddress(id)

    fun getFirstName(): Name = Users.readFirstName(id)

    fun getLastName(): Name = Users.readLastName(id)

    fun getBio(): Bio = Users.readBio(id)
}
