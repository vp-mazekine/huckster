package com.broxus.nova.models

/**
 * A model for input to the users/balances method
 *
 * @property workspaceId Unique workspace Id
 */
data class WorkspaceBalanceInput (
    val workspaceId: String
)