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
package com.github.manosbatsis.partiture.flow

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.tx.responder.ResponderTxStrategy
import com.github.manosbatsis.partiture.flow.util.PartitureUtilsFlowLogic
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.ReceiveFinalityFlow

/**
 * Base responder [FlowLogic] implementation, delegates transaction checking
 * and signing to the [SignTransactionFlow] obtained by the given [ResponderTxStrategy]
 * instance, then proceeds to receive an update with a [ReceiveFinalityFlow].
 */
abstract class PartitureResponderFlow(
        val otherPartySession: FlowSession,
        val responderTxStrategy: ResponderTxStrategy
) : PartitureUtilsFlowLogic<Unit>() {

    /** Override to do something before calling [SignTransactionFlow] */
    @Suspendable
    open fun preSignTransaction() {
        /* NO-OP */
    }
    /**
     * Use the given ResponderTxStrategy to obtain a [SignTransactionFlow]
     * that verifies and signs the transaction
     */
    @Suspendable
    final override fun call() {
        // Call any custom logic
        preSignTransaction()
        // Create our custom SignTransactionFlow
        val signTransactionFlow = responderTxStrategy
                .setClientFlow(this).createSignTransactionFlow()
        // Sign if the check is successful
        subFlow(signTransactionFlow)
        // Receive an update when done
        subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}
