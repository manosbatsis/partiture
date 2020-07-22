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
package com.github.manosbatsis.partiture.flow.io.output

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import net.corda.core.transactions.SignedTransaction

const val MSG_NO_FIN_TX = "Could not find a finalized TX while trying to convert"

/**
 * Converts to the finalized transactions of (each entry in) the given [CallContext] if available,
 * throws an error otherwise.
 */
class FinalizedTxOutputConverter(
        /** Whether to ignore [CallContext] entries without a finalized TX */
        val allowMissing: Boolean = false
) : PartitureFlowDelegateBase(), OutputConverter<List<SignedTransaction>> {

    @Suspendable
    override fun convert(input: CallContext): List<SignedTransaction> {
        return input.entries.mapNotNull {
            it.finalized ?: if(allowMissing) null else throw IllegalArgumentException(MSG_NO_FIN_TX)
        }
    }
}

/**
 * Converts to the finalized transaction of the single [CallContext] entry if available,
 * throws an error otherwise.
 */
class SingleFinalizedTxOutputConverter : PartitureFlowDelegateBase(), OutputConverter<SignedTransaction> {

    @Suspendable
    override fun convert(input: CallContext): SignedTransaction {
        return input.entries.mapNotNull { it.finalized }.single()
    }
}