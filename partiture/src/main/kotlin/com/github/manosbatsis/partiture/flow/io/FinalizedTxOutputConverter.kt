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
package com.github.manosbatsis.partiture.flow.io

import com.github.manosbatsis.partiture.flow.delegate.FlowDelegateBase
import com.github.manosbatsis.partiture.flow.tx.TxContext
import net.corda.core.transactions.SignedTransaction

/** Converts to the finalized transaction of the given [TxContext] by if available, throws an error otherwise. */
class FinalizedTxOutputConverter : FlowDelegateBase(), OutputConverter<SignedTransaction> {
    override fun convert(source: TxContext): SignedTransaction {
        return source.finalized ?: throw IllegalArgumentException("Could not find a finalized TX while trying to convert")
    }
}