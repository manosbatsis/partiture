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
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegate
import com.github.manosbatsis.partiture.flow.lifecycle.SimpleInitiatingLifecycle
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger

/** Transaction strategy delegate. */
interface TxStrategy : PartitureFlowDelegate {

    companion object{
        private val logger = contextLogger()
    }

    /**
     * Provide the appropriate [ProgressTracker] to the calling flow.
     * Must start with:
     *
     * - [SimpleInitiatingLifecycle.INITIALIZE]
     * - [SimpleInitiatingLifecycle.PROCESS_INPUT]
     * - [SimpleInitiatingLifecycle.POST_PROCESS_INPUT]
     * - [SimpleInitiatingLifecycle.EXECUTE_TRANSACTIONS]
     *
     * and end with
     *
     * - [SimpleInitiatingLifecycle.TRANSACTION_POST_EXECUTE]
     * - [SimpleInitiatingLifecycle.PROCESS_OUTPUT]
     *
     * as those steps are always used by the calling flow. Any in-between steps depend solely on your implementation.
     *
     * Recommended: implement a reusable [com.github.manosbatsis.partiture.flow.lifecycle.Lifecycle] `object`
     * that can then be shared between similar strategies for obtaining a properly configured progress tracker.
     * See [SimpleInitiatingLifecycle] for an example.
     *
     */
    val progressTracker: ProgressTracker

    /** Convenient chain method */
    fun setClientFlow(clientFlow: PartitureFlow<*, *>): TxStrategy {
        this.clientFlow = clientFlow;
        return this
    }

    /**
     * Set the given step as current in the client flow's [ProgressTracker]
     * @return the same step
     */
    fun step(step: ProgressTracker.Step): ProgressTracker.Step {
        logger.debug("Step: $step")
        clientFlow.progressTracker.currentStep = step
        return step
    }

    /**
     * Process the mutable transactions (i.e. [CallContextEntry] instances) available
     * in the calling flow's [CallContext]. The default implementation iterates the current
     * entries of the flow's [CallContext], calling
     * [com.github.manosbatsis.partiture.flow.tx.TxStrategy.executeTransaction]
     * with each entry.
     *
     * It is a good practice to only throw [TxStrategyExecutionException]s from this method.
     * @see com.github.manosbatsis.partiture.flow.PartitureFlow.handleFailedTxStrategy
     */
    @Suspendable
    @Throws(TxStrategyExecutionException::class)
    fun execute() {
        clientFlow.callContext.entries.forEach { executeFor(it) }
    }

    /**
     * Process the given [CallContextEntry]
     *
     * It is a good practice to only throw [TxStrategyExecutionException]s from this method.
     * @see com.github.manosbatsis.partiture.flow.PartitureFlow.handleFailedTxStrategy
     */
    @Suspendable
    @Throws(TxStrategyExecutionException::class)
    fun executeFor(ccEntry: CallContextEntry)
    @Suspendable
    fun postExecuteFor(ourParties: List<AbstractParty>, counterParties: List<AbstractParty>, sessions: Set<FlowSession>, ccEntry: CallContextEntry)
}
