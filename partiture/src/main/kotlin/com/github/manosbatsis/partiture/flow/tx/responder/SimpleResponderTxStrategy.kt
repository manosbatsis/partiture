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
package com.github.manosbatsis.partiture.flow.tx.responder

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.delegate.responder.PartitureResponderFlowDelegateBase
import net.corda.core.contracts.requireThat
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.transactions.SignedTransaction

/** Simple responder flow delegate that verifies the transaction before signing. */
class SimpleResponderTxStrategy : PartitureResponderFlowDelegateBase(), ResponderTxStrategy {

    /** Create a [SignTransactionFlow] with the appropriate verification checks. */
    @Suspendable
    override fun createSignTransactionFlow(): SignTransactionFlow {
        // Create our custom SignTransactionFlow
        return object : SignTransactionFlow(
                clientFlow.otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // Ensure the transaction is sane
                stx.tx.toLedgerTransaction(serviceHub).verify()
            }
        }
    }
}