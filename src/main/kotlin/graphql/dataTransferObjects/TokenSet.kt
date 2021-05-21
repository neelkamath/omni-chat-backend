@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.TokenSet

class TokenSet(private val tokenSet: TokenSet) : RequestTokenSetResult {
    fun getAccessToken(): String = tokenSet.accessToken.value

    fun getRefreshToken(): String = tokenSet.refreshToken.value
}
