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
package com.github.manosbatsis.partiture.flow.tx.initiating

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.lifecycle.SimpleInitiatingLifecycle
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AnonymousParty
import java.lang.Exception

/**
 * This (currently default) transaction strategy implementation will:
 *
 * - For each available [CallContextEntry] in the flow's CallContext
 *      - Sign an initial transaction
 *      - Create flow sessions for counter-parties, if any exist
 *      - Perform an identity sync if `forceIdentitySync` is `true` or if any own anonymous parties are participating in the input/output states in context
 *      - Gather and verify counter-party signatures
 *      - Verify the original transaction builder
 *      - Finalize the transaction
 */
open class SimpleTxStrategy : PartitureFlowDelegateBase(), TxStrategy {

    /** Provides an instance pre-configured with the default progress steps */
    override val progressTracker = SimpleInitiatingLifecycle.progressTracker()

    @Suspendable
    @Throws(TxStrategyExecutionException::class)
    override fun execute() {
        try {
            clientFlow.callContext.entries.forEach { executeFor(it) }
        } catch (e: FlowException) {
            throw TxStrategyExecutionException("Failed to execute", e, e.originalErrorId)
        } catch (e: Exception) {
            throw TxStrategyExecutionException("Failed to execute", e)
        }
    }

    @Suspendable
    @Suppress("UNUSED_VALUE")
    override fun executeFor(ccEntry: CallContextEntry) {
        step(SimpleInitiatingLifecycle.SIGN_INITIAL_TX)
        // Perform initial transaction signature
        ccEntry.initial = clientFlow
                .signInitialTransaction(ccEntry.transactionBuilder)
        // Get our own and counter-parties
        val (ourParties, counterParties) =
                clientFlow.partitionOursAndTheirs(ccEntry.participants)
        // Counter-sign and sync
        var sessions: Set<FlowSession> = setOf()
        if (counterParties.isNotEmpty()) {
            // Create counter-party sessions
            step(SimpleInitiatingLifecycle.CREATE_SESSIONS)
            sessions = clientFlow.createFlowSessions(counterParties)
            // Perform an ID sync if any of our own participating parties is anonymous
            if (clientFlow.forceIdentitySync || ourParties.any { it is AnonymousParty }) {
                val currentStep = step(SimpleInitiatingLifecycle.SYNC_IDENTITIES)
                clientFlow.pushOurIdentities(
                        sessions, ccEntry.initial!!.tx, currentStep.childProgressTracker()!!)
            }
            clientFlow.postCreateFlowSessions(sessions)
            // Retrieve counter-party signatures
            val currentStep = step(SimpleInitiatingLifecycle.GATHER_SIGNATURES)
            val counterSigned = clientFlow.subFlow(CollectSignaturesFlow(
                    ccEntry.initial!!, sessions, currentStep.childProgressTracker()!!))
            step(SimpleInitiatingLifecycle.VERIFY_SIGNATURES)
            if(ccEntry.transactionBuilder.notary != null)
                counterSigned.verifySignaturesExcept(ccEntry.transactionBuilder.notary!!.owningKey)
            else counterSigned.verifyRequiredSignatures()
            ccEntry.counterSigned = counterSigned
        }
        // Verify TX builder state
        @Suppress("UNUSED_VALUE")
        step(SimpleInitiatingLifecycle.VERIFY_TRANSACTION_DATA)
        ccEntry.transactionBuilder.verify(clientFlow.serviceHub)
        // Finalize
        val currentStep = step(SimpleInitiatingLifecycle.FINALIZE)
        ccEntry.finalized = clientFlow.finalizeTransaction(
                ccEntry.counterSigned!!, sessions,
                currentStep.childProgressTracker()!!)
    }
}
