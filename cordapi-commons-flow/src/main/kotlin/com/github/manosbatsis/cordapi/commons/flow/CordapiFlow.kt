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
package com.github.manosbatsis.cordapi.commons.flow

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.cordapi.commons.flow.base.BaseFlowLogic
import com.github.manosbatsis.cordapi.commons.flow.tx.TxContext
import com.github.manosbatsis.cordapi.commons.flow.call.CallDelegate
import com.github.manosbatsis.cordapi.commons.flow.call.SimpleCallDelegate
import com.github.manosbatsis.cordapi.commons.flow.util.ProgressTrackerUtil
import net.corda.core.flows.FlowLogic
import net.corda.core.utilities.ProgressTracker

/**
 * Base [FlowLogic] implementation utilizing [TxContext] and [CallDelegate]
 */
abstract class CordapiFlow<out OUT>(
        val callDelegate: CallDelegate = SimpleCallDelegate()
) : BaseFlowLogic<OUT>() {

    // TODO  obtain tracker from delegate?
    override val progressTracker: ProgressTracker = ProgressTracker(
            ProgressTrackerUtil.Companion.INITIALISE,
            ProgressTrackerUtil.Companion.PROCESS_INPUT,
            ProgressTrackerUtil.Companion.PREPARE_TRANSACTION_DATA,
            ProgressTrackerUtil.Companion.VERIFY_TRANSACTION_DATA,
            ProgressTrackerUtil.Companion.SIGN_INITIAL_TX,
            ProgressTrackerUtil.Companion.CREATE_SESSIONS,
            ProgressTrackerUtil.Companion.GATHER_SIGNATURES,
            ProgressTrackerUtil.Companion.SYNC_IDENTITIES,
            ProgressTrackerUtil.Companion.FINALIZE)

    init {
        this.progressTracker.currentStep = ProgressTrackerUtil.Companion.INITIALISE
    }


    /**
     * Prepare a context with items relevant to flow transaction(s):
     * Notary, input/output/reference states, attachments, commands and so on.
     * */
    abstract fun createTxContext(): TxContext

    /** Convert the given signed transactions to the desired type for flow `out`. */
    abstract fun toOut(txContext: TxContext): OUT

    @Suspendable
    override fun call(): OUT {
        // Delegate processing to callDelegate
        val txResults = this.callDelegate.execute(this, createTxContext())
        // Process and return output
        return this.toOut(txResults)
    }
}