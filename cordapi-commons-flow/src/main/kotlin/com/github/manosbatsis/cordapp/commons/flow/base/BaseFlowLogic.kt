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
package com.github.manosbatsis.cordapp.commons.flow.base

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.cordapp.commons.service.PartyService
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party

/**
 * Base [FlowLogic] implementation, includes common utilities
 */
abstract class BaseFlowLogic<out T> : FlowLogic<T>() {

    /** Lazily initialized party service. */
    val partyService: PartyService by lazy { this. serviceHub.cordaService(PartyService::class.java) }

    /** Get a single session per counter-party for the given contract states. */
    @Suspendable
    fun toFlowSessions(states: Iterable<ContractState>): List<FlowSession> =
            states.toList().map { partyService.toWellKnownParticipants(it) }
                    .flatten().toSet().filter { it.name != this.ourIdentity.name }
                    .map { party: Party -> initiateFlow(party) }

    /** Get a single session per counter-party for the given contract state(s). */
    @Suspendable
    fun toFlowSessions(vararg states: ContractState): List<FlowSession> = toFlowSessions(states.toList())
}