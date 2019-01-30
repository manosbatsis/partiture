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
package com.github.manosbatsis.cordapp.commons.flow.tx

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash

/**
 * Contains the items for a single Corda transaction. Typically used for providing
 * input to a [net.corda.core.transactions.TransactionBuilder]
 */
data class TransactionItems(
        /** [StateAndRef] objects used as the transaction input states */
        val inputStates: List<StateAndRef<*>> = emptyList(),
        /** [ContractState] objects used as the transaction output states */
        val outputStates: Iterable<ContractState> = emptyList(),
        /** [TransactionState] objects used as the transaction output states */
        val outputTransactionStates: Iterable<TransactionState<*>> = emptyList(),
        /** [StateAndContract] objects used as the transaction output states */
        val outputStateAndContracts: Iterable<StateAndContract> = emptyList(),
        /** [CommandData] objects used to build the transaction commands */
        val commandTypes: Iterable<CommandData> = emptyList(),
        /** [SecureHash] objects corresponding to transaction attachments that must have already been uploaded */
        val attachments: Iterable<SecureHash> = emptyList()
)