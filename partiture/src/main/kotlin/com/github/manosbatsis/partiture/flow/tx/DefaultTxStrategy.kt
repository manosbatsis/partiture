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
package com.github.manosbatsis.partiture.flow.tx

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.delegate.FlowDelegateBase
import com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AnonymousParty
import com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion as Steps

/** Default call txStrategy implementation */
open class DefaultTxStrategy : FlowDelegateBase(), TxStrategy {

    override val progressTracker = ProgressTrackerUtil.defaultProgressTracker()

    @Suspendable
    override fun execute(txContext: TxContext): TxContext {
        clientFlow.progressTracker.currentStep = Steps.SIGN_INITIAL_TX
        // Perform initial transaction signature
        txContext.initial = clientFlow.signInitialTransaction(txContext.transactionBuilder)
        // Get our own and counter-parties
        val (ourIdentities, counterParties) = clientFlow.toOursAndTheirs(txContext.participants)
        // Counter-sign and sync
        var sessions: List<FlowSession>? = null
        if (counterParties.isNotEmpty()) {
            // Create counter-party sessions
            clientFlow.progressTracker.currentStep = Steps.CREATE_SESSIONS
            sessions = clientFlow.toFlowSessions(counterParties)

            // Perform an ID sync, if any of our parties is anonymous
            if (ourIdentities.any { it is AnonymousParty }) {
                clientFlow.progressTracker.currentStep = Steps.SYNC_IDENTITIES
                clientFlow.performIdentitySync(sessions, txContext.initial!!.tx)
            }
            // Retrieve counter-party signatures
            clientFlow.progressTracker.currentStep = Steps.GATHER_SIGNATURES
            txContext.counterSigned = clientFlow.subFlow(
                    CollectSignaturesFlow(txContext.initial!!, sessions, Steps.GATHER_SIGNATURES.childProgressTracker()))
        }
        // Verify TX builder state
        clientFlow.progressTracker.currentStep = Steps.VERIFY_TRANSACTION_DATA
        txContext.transactionBuilder.verify(clientFlow.serviceHub)
        // Finalize
        clientFlow.progressTracker.currentStep = Steps.FINALIZE
        txContext.finalized = if (sessions != null) clientFlow.subFlow(
                FinalityFlow(txContext.counterSigned!!, sessions, Steps.FINALIZE.childProgressTracker()))
        else clientFlow.subFlow(FinalityFlow(txContext.initial!!, Steps.FINALIZE.childProgressTracker()))

        return txContext
    }


}