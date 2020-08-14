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

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.OutputConverter
import com.github.manosbatsis.partiture.flow.tx.initiating.SimpleTxStrategy
import com.github.manosbatsis.partiture.flow.tx.initiating.TxStrategy
import com.github.manosbatsis.partiture.flow.util.IdentitySyncMode.SKIP
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey


/**
 * Extends [PartitureFlow] to provide a Corda Accounts-aware,
 * base [FlowLogic] implementation for flow composition
 */
abstract class PartitureAccountsAwareFlow<IN, OUT>(
        input: IN,
        txStrategy: TxStrategy = SimpleTxStrategy(),
        inputConverter: InputConverter<IN>? = null,
        outputConverter: OutputConverter<OUT>?
) : PartitureFlow<IN, OUT>(
        input, txStrategy, inputConverter, outputConverter, SKIP
){

    /** Assume all participants are accounts and need a [FlowSession] */
    @Suspendable
    override fun createFlowSessions(
            participants: Iterable<AbstractParty>
    ): Set<FlowSession> {
        val ourId = this.ourIdentity.name.toString()
        println("$ourId createFlowSessions, parties: ${participants.joinToString (",") }}")
        val sessions = mutableSetOf<FlowSession>()
        for(party in participants){
            println("$ourId createFlowSessions, open session for AbstractParty: ${party}")

            val wellKnown = toWellKnownParty(party)
            println("$ourId createFlowSessions, open session for Party: ${wellKnown.name}")
/*
Our parties:     [Anonymous(DLEGd12j5kANU38Cgio9SfwxP4vDy9C99kwwAPyharm5GB),
              Anonymous(DL34bbWXUpqJ4ULeXjhJVHw3GQkYmR8fSfUC8R1RQVznLB)] {actor_id=Only For Testing, actor_owning_identity=O=PartyB, L=New York, C=US, actor_store_id=TEST, fiber-id=10000009, flow-id=ecff83ae-d0b7-4d14-9658-1b35bd7cef0b, invocation_id=fdd4d7e7-1314-421d-abf8-4f5281145465, invocation_timestamp=2020-08-12T08:48:37.233Z, origin=Only For Testing, session_id=fdd4d7e7-1314-421d-abf8-4f5281145465, session_timestamp=2020-08-12T08:48:37.233Z, thread-id=1660, tx_id=A6AAC37BCFCDFDE01593CFD09E5917EF681B5E1F5387F2E4CB73B422C668BC29}
Counter parties: [Anonymous(DLCHickUdb2oFss7HE2KnZt9s8cti2vbCQiLc62EvALAz6),
              Anonymous(DLBC5NWkPosprW4kASn4BkGqi9M4vu4cvaHY98d925jqRt)] {actor_id=Only For Testing, actor_owning_identity=O=PartyB, L=New York, C=US, actor_store_id=TEST, fiber-id=10000009, flow-id=ecff83ae-d0b7-4d14-9658-1b35bd7cef0b, invocation_id=fdd4d7e7-1314-421d-abf8-4f5281145465, invocation_timestamp=2020-08-12T08:48:37.233Z, origin=Only For Testing, session_id=fdd4d7e7-1314-421d-abf8-4f5281145465, session_timestamp=2020-08-12T08:48:37.233Z,
*/
            sessions.add(initiateFlow(party))

        }
        return sessions
    }

    /** Use both our account and node identity keys in context */
    @Suspendable
    override fun ourParticipatingKeys(ourParties: List<AbstractParty>): Iterable<PublicKey> {
        val identityService = serviceHub.identityService
        val nodeKeys = ourParties
                .filterIsInstance(AnonymousParty::class.java)
                .map { identityService.requireWellKnownPartyFromAnonymous(AnonymousParty(it.owningKey)) }
                .map { it.owningKey }
        val accountKeys = ourParties.map { it.owningKey }
        return nodeKeys + accountKeys
    }


    @Suspendable
    override fun postExecuteFor(
            ourParties: List<AbstractParty>,
            counterParties: List<AbstractParty>,
            sessions: Set<FlowSession>,
            ccEntry: CallContextEntry) {
        val tx = ccEntry.finalized ?: throw FlowException("CallContextEntry has no finalized TX")
        val outputs = tx.coreTransaction.outRefsOfType<ContractState>()
        for(state in outputs) {
            for(session in sessions) {
                // TODO: use ShareStateWithAccountFlow instead?
                subFlow(ShareStateAndSyncAccounts(state, session.counterparty))
            }
        }
    }

}