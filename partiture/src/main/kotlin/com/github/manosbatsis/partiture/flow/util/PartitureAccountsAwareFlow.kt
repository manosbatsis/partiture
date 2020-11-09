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
import java.security.PublicKey


/**
 * Extends [PartitureFlow] to provide a Corda Accounts-aware,
 * base [FlowLogic] implementation for flow composition
 */
abstract class PartitureAccountsAwareFlow<IN, OUT>(
        input: IN,
        txStrategy: TxStrategy = SimpleTxStrategy(),
        inputConverter: InputConverter<IN>? = null,
        outputConverter: OutputConverter<OUT>? = null
) : PartitureFlow<IN, OUT>(
        input, txStrategy, inputConverter, outputConverter, SKIP
){

    /** Assume all participants are accounts and need a [FlowSession] */
    @Suspendable
    override fun createFlowSessions(
            participants: Iterable<AbstractParty>
    ): Set<FlowSession> {
        val partySessions = mutableMapOf<PublicKey, FlowSession>()
        for(party in participants){
            val wellKnown = toWellKnownParty(party)
            if(!partySessions.keys.contains(wellKnown.owningKey))
                partySessions[wellKnown.owningKey] = initiateFlow(wellKnown)

        }
        return partySessions.values.toSet()
    }

    /** Use both our account and node identity keys in context */
    @Suspendable
    override fun ourParticipatingKeys(ourParties: List<AbstractParty>): Iterable<PublicKey> {
        val nodeKeys = ourParties.map { toWellKnownParty(it).owningKey }.toSet()
        val accountKeys = ourParties.map { it.owningKey }.toSet()
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