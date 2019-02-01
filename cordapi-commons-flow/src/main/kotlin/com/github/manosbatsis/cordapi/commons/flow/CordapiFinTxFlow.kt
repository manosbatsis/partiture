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

import com.github.manosbatsis.cordapi.commons.flow.call.CallDelegate
import com.github.manosbatsis.cordapi.commons.flow.call.SimpleCallDelegate
import com.github.manosbatsis.cordapi.commons.flow.tx.TxContext
import net.corda.core.transactions.SignedTransaction

/** [CordapiFlow] that outputs the finalized transaction */
abstract class CordapiFinTxFlow(
        callDelegate: CallDelegate = SimpleCallDelegate()
) : CordapiFlow<SignedTransaction>(callDelegate) {

    /** Convert the given transaction context to the desired type for flow `out`. */
    override fun toOut(txContext: TxContext): SignedTransaction {
        return txContext.finalized!!
    }
}