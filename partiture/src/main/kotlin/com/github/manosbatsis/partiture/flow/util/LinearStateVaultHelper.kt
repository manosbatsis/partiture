/*
 *     Partiture: a compact component framework for your Corda apps
 *     Copyright (C) 2018 Manos Batsis
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package com.github.manosbatsis.partiture.flow.util

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.vault.QueryCriteria

/** Vault access helper for linear contract states */
class LinearStateVaultHelper<T : LinearState>(val vaultService: VaultService, val stateType: Class<out T>) {

    /** Lazy reusable set of the single state type used in vault queries etc. */
    val contractStateTypes by lazy { setOf(stateType) }

    /**
     * Get the state matching the linear ID if it exists
     * @throws [FlowException] if no match is found
     */
    fun getByLinearId(
            linearId: UniqueIdentifier,
            status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
            participants: List<AbstractParty>? = null): StateAndRef<T> {
        return findByLinearId(linearId, status, participants) ?: throw FlowException("Linear ID $linearId not found.")
    }

    /** Get the state matching the linear ID if it exists, null otherwise */
    fun findByLinearId(
            linearId: UniqueIdentifier,
            status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
            participants: List<AbstractParty>? = null): StateAndRef<T>? {
        val criteria = QueryCriteria.LinearStateQueryCriteria(
                participants = participants,
                linearId = listOf(linearId),
                status = status,
                contractStateTypes = contractStateTypes)
        return vaultService.queryBy(stateType, criteria).states.singleOrNull()
    }

    /**
     * Get the state matching the external ID if it exists
     * @throws [FlowException] if no match is found
     */
    fun getByExternalId(
            externalId: String,
            status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
            participants: List<AbstractParty>? = null): StateAndRef<T> {
        return findByExternalId(externalId, status, participants) ?: throw FlowException("External ID $externalId not found.")
    }

    /** Get the state matching the external ID if it exists, null otherwise */
    fun findByExternalId(
            externalId: String,
            status: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
            participants: List<AbstractParty>? = null): StateAndRef<T>? {
        val criteria = QueryCriteria.LinearStateQueryCriteria(
                participants = participants,
                externalId = listOf(externalId),
                status = status
        )
        return vaultService.queryBy(stateType, criteria).states.singleOrNull()
    }

}
