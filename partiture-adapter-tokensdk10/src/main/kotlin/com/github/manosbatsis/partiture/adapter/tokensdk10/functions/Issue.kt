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
package com.github.manosbatsis.partiture.adapter.tokensdk10.functions

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.identity.Party

/**
 *  Pretty much a coy of Corda's issuing functions
 *  to work around not having TX builder interface,
 *  so this is mostly R3's Copyright under Apache License Version 2.0
 *  see qlso https://github.com/corda/token-sdk/blob/master/LICENCE
 */


/**
 * A function that adds a list of output [AbstractToken] states to a [TransactionBuilderWrapper]. It automatically adds
 * [IssueTokenCommand] commands for each [IssuedTokenType]. A notary [Party] must be added to the [TransactionBuilderWrapper]
 * before this function can be called.
 */
@Suspendable
fun addIssueTokens(transactionBuilder: TransactionBuilderWrapper, outputs: List<AbstractToken>): TransactionBuilderWrapper {
    val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
    return transactionBuilder.apply {
        outputGroups.forEach { (issuedTokenType: IssuedTokenType, states: List<AbstractToken>) ->
            val issuers = states.map { it.issuer }.toSet()
            require(issuers.size == 1) { "All tokensToIssue must have the same issuer." }
            val issuer = issuers.single()
            var startingIndex = outputStates().size
            val indexesAdded = states.map { state ->
                addOutputState(state)
                startingIndex++
            }
            addCommand(IssueTokenCommand(issuedTokenType, indexesAdded), issuer.owningKey)
        }
    }
}

/**
 * A function that adds a list of output [AbstractToken] states to a [TransactionBuilderWrapper]. It automatically adds
 * [IssueTokenCommand] commands for each [IssuedTokenType]. A notary [Party] must be added to the [TransactionBuilderWrapper]
 * before this function can be called.
 */
@Suspendable
fun addIssueTokens(transactionBuilder: TransactionBuilderWrapper, vararg outputs: AbstractToken): TransactionBuilderWrapper {
    return addIssueTokens(transactionBuilder, outputs.toList())
}

/**
 * A function that adds a single output [AbstractToken] state to a [TransactionBuilderWrapper]. It automatically adds an
 * [IssueTokenCommand] command. A notary [Party] must be added to the [TransactionBuilderWrapper] before this function can be
 * called.
 */
@Suspendable
fun addIssueTokens(transactionBuilder: TransactionBuilderWrapper, output: AbstractToken): TransactionBuilderWrapper {
    return addIssueTokens(transactionBuilder, listOf(output))
}