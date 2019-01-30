/*
 *     Cordapi: Common components for Cordapps
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
package com.github.manosbatsis.cordapp.commons.service

import net.corda.core.contracts.ContractState
import net.corda.core.identity.*
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.IdentityService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor

/**
 * Decorates the [IdentityService] normally obtained by [ServiceHub] with [Party]-related
 * utility methods. To use:
 * ```kotlin
 * // Obtain the service
 * val identityService = serviceHub.cordaService(PartyService::class.java)
 * // Use the service
 * val knownParticipants = identityService.toWellKnownParticipants(contractStates)
 * ```
 */
@CordaService
class PartyService(
        val serviceHub: AppServiceHub
) : SingletonSerializeAsToken() {

    private companion object {
        val log = loggerFor<PartyService>()
    }

    init {
        log.info("${this.javaClass.simpleName} initialized}")
    }

    /** Filter out self from said parties */
    private fun List<Party>.exceptMe() = this.filter { !serviceHub.myInfo.legalIdentities.contains(it) }

    /**
     * Resolve to a known party if [AnonymousParty], return as-is (i.e. [Party]) otherwise
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownParty(abstractParty: AbstractParty): Party =
            abstractParty as? Party
                    ?: serviceHub.identityService.wellKnownPartyFromAnonymous(abstractParty)
                    ?: throw RuntimeException("Could resolve to known party: ${abstractParty.nameOrNull()?:"unknown"}")

    /**
     * Go over participants, resolving instances of [AnonymousParty] to known parties
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownParticipants(state: ContractState): List<Party> =
            state.participants.map { toWellKnownParty(it) }

    /**
     * Resolve participating parties
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownParticipants(states: Iterable<ContractState>): List<Party> =
            states.map { it.participants }.flatten().map { toWellKnownParty(it) }

    /**
     * Go over counter-party participants of each state, resolving instances of [AnonymousParty] to known parties
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownCounterParties(state: ContractState): List<Party> =
            toWellKnownParticipants(state).exceptMe()


    /**
     * Resolve participating counter-parties
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun wellKnownCounterParties(states: Iterable<ContractState>): List<Party> =
            toWellKnownParticipants(states).exceptMe()

    /**
     * Find the first notary found in the network map if it exists
     * @return the first notary if any exists, `null` otherwise
     */
    fun findFirstNotary(): Party? = serviceHub.networkMapCache.notaryIdentities.firstOrNull()

    /**
     * Get the first notary found in the network map
     * @throws [RuntimeException] if no notaries are found
     */
    fun getFirstNotary(): Party = findFirstNotary()
            ?: throw RuntimeException("No notaries found in network map cache")

    /**
     * Get the first notary matching the given organisation name if it exists
     * @return the first matching notary if any exists, `null` otherwise
     */
    fun findNotaryByOrganisation(organisation: String) : Party? =
            serviceHub.networkMapCache.notaryIdentities.firstOrNull { it.name.organisation == organisation }

    /**
     * Get the first notary matching the given organisation name
     * @throws [RuntimeException] if no matching notary is found
     */
    fun getNotaryByOrganisation(organisation: String) : Party = findNotaryByOrganisation(organisation)
            ?: throw RuntimeException("No notaries found in network map cache for organisation $organisation")

}