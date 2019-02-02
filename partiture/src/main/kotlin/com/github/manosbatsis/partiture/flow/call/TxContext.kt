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
package com.github.manosbatsis.partiture.flow.call

import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * The transaction context of the current FlowLogic call
 * Stores transaction builder, participants, results and metadata
 */
data class TxContext(
        /** A fully initialized TransactionBuilder instance */
        val transactionBuilder: TransactionBuilder,
        /** The participants for this transaction */
        val participants: List<AbstractParty>,
        /** The main initially signed transaction */
        var initial: SignedTransaction? = null,
        /** The main counter-signed transaction */
        var counterSigned: SignedTransaction? = null,
        /** The main finalized transaction */
        var finalized: SignedTransaction? = null,
        /**  Other custom transactions by key */
        var others: MutableMap<String, SignedTransaction>? = null,
        /**  Additional metadata */
        var meta: MutableMap<String, Any>? = null
) {
    /**
     * Add a custom transaction result
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    fun addOther(key: String, tx: SignedTransaction): SignedTransaction? {
        return if (this.others != null) this.others!!.put(key, tx)
        else {
            this.others = mutableMapOf(key to tx)
            null
        }
    }

    /**
     * Add a custom metadatum
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    fun addMeta(key: String, value: Any): Any? {
        return if (this.meta != null) this.meta!!.put(key, value)
        else {
            this.meta = mutableMapOf(key to value)
            null
        }
    }
}