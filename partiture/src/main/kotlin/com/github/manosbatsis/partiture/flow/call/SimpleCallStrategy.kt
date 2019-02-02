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
package com.github.manosbatsis.partiture.flow.call

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.delegate.FlowDelegateBase
import com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AnonymousParty
import com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion as Steps

/**
 * The default call strategy implementation will:
 *
 * - Call the client's flow's `convertInput` to process the flow input and initialize the call context
 * - Sign an initial transaction
 * - Create flow sessions for counter-parties, if any exist
 * - Perform an identity sync if any own anonymous parties are participating in the input/output states of the call context
 * - Gather counter-party signatures
 * - Verify the original transaction builder
 * - Finalize the transaction
 */
open class SimpleCallStrategy : FlowDelegateBase(), TxStrategy {

    override val progressTracker = ProgressTrackerUtil.defaultProgressTracker()

    @Suspendable
    override fun execute(txContext: TxContext): TxContext {
        clientFlow.progressTracker.currentStep = Steps.SIGN_INITIAL_TX
        // Perform initial transaction signature
        txContext.initial = clientFlow.signInitialTransaction(txContext.transactionBuilder)
        // Get our own and counter-parties
        val (ourParties, counterParties) =
                clientFlow.partitionOursAndTheirs(txContext.participants)
        // Counter-sign and sync
        var sessions: List<FlowSession> = listOf()
        if (counterParties.isNotEmpty()) {
            // Create counter-party sessions
            clientFlow.progressTracker.currentStep = Steps.CREATE_SESSIONS
            sessions = clientFlow.createFlowSessions(counterParties)

            // Perform an ID sync if any of our own participating parties is anonymous
            if (ourParties.any { it is AnonymousParty }) {
                clientFlow.progressTracker.currentStep = Steps.SYNC_IDENTITIES
                clientFlow.pushOurIdentities(sessions, txContext.initial!!.tx)
            }
            // Retrieve counter-party signatures
            clientFlow.progressTracker.currentStep = Steps.GATHER_SIGNATURES
            txContext.counterSigned = clientFlow.subFlow(
                    CollectSignaturesFlow(
                            txContext.initial!!, sessions, Steps.GATHER_SIGNATURES.childProgressTracker()))
        }
        // Verify TX builder state
        clientFlow.progressTracker.currentStep = Steps.VERIFY_TRANSACTION_DATA
        txContext.transactionBuilder.verify(clientFlow.serviceHub)
        // Finalize
        clientFlow.progressTracker.currentStep = Steps.FINALIZE
        txContext.finalized = clientFlow.finalizeTransaction(
                txContext.counterSigned!!, sessions, Steps.FINALIZE.childProgressTracker())

        return txContext
    }


}