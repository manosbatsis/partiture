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
import net.corda.core.utilities.ProgressTracker

/** Transaction strategy delegate. */
interface TxStrategy : PartitureFlowDelegate {

    /**
     * Provide the appropriate [ProgressTracker] to the calling flow.
     * Must start with:
     *
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.INITIALIZE]
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.PROCESS_INPUT]
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.POST_PROCESS_INPUT]
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.EXECUTE_TRANSACTIONS]
     *
     * and end with
     *
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.POST_EXECUTE_TRANSACTIONS]
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.PROCESS_OUTPUT]
     * - [com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil.Companion.FINISHED]
     *
     * as those steps are always used by the calling flow. Any other steps depend to your implementation.
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
        clientFlow.progressTracker.currentStep = step
        return step
    }

    /**
     * Process the mutable transactions (i.e. [CallContextEntry] instances) available
     * in the calling flow's [CallContext]. The default implementation iterates the current
     * entries of the flow's [CallContext], calling
     * [com.github.manosbatsis.partiture.flow.tx.TxStrategy.executeTransaction]
     * with each entry.
     */
    @Suspendable
    fun execute() {
        clientFlow.callContext.entries.forEach { executeFor(it) }
    }

    /** Process the given [CallContextEntry] */
    @Suspendable
    fun executeFor(ccEntry: CallContextEntry)
}