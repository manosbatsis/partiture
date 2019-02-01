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
package com.github.manosbatsis.cordapi.commons.flow.call

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.cordapi.commons.flow.base.BaseFlowLogic
import com.github.manosbatsis.cordapi.commons.flow.tx.TxContext
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.WireTransaction

/** Simple call callDelegate implementation */
open class SimpleCallDelegate : CallDelegate {

    @Suspendable
    override fun <T : BaseFlowLogic<*>> execute(
            client: T,
            txContext: TxContext): TxContext {
        // Perform initial transaction signature
        txContext.initial = client.signInitialTransaction(txContext.transactionBuilder)
        // Verify TX builder state
        //transactionBuilder.verify(client.serviceHub)
        // Get counter-parties
        val counterParties = client.getCounterParties(txContext.participants)
        // Create counter-party sessions
        val sessions = client.toFlowSessions(counterParties)
        if (sessions.isNotEmpty()) {
            // Perform ID sync, if implemented
            this.performIdentitySync(client, txContext.initial!!.tx, sessions)
            // Retrieve counter-party signatures
            txContext.counterSigned = client.gatherCounterPartySignatures(txContext.initial!!, sessions)
        }
        // Finalize the transaction
        txContext.finalized = client.toFinalizedTransaction(txContext.counterSigned!!, sessions)
        // Return results
        return txContext
    }

    /** NO-OP: Override to implement or use [SyncIdCallDelegate] instead */
    @Suppress("UNUSED_PARAMETER")
    @Suspendable
    fun <T : BaseFlowLogic<*>> performIdentitySync(client: T, tx: WireTransaction, sessions: List<FlowSession>) {
        client.logger.debug("SimpleCallDelegate: NO-OP performIdentitySync(), either override or use SyncIdCallDelegate")
    }

}