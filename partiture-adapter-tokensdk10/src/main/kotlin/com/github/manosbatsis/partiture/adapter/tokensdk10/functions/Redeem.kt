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
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStateAndRefs
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.utilities.heldTokensByTokenIssuer
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountWithIssuerCriteria
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria

/**
 *  Pretty much a coy of Corda's redemption functions
 *  to work around not having TX builder interface,
 *  so this is mostly R3's Copyright under Apache License Version 2.0
 *  see qlso https://github.com/corda/token-sdk/blob/master/LICENCE
 */

/**
 * Add redeeming of multiple [inputs] to the [transactionBuilder] with possible [changeOutput].
 */
@Suspendable
@JvmOverloads
fun addTokensToRedeem(
        transactionBuilder: TransactionBuilderWrapper,
        inputs: List<StateAndRef<AbstractToken>>,
        changeOutput: AbstractToken? = null
): TransactionBuilderWrapper {
    checkSameIssuer(inputs, changeOutput?.issuer)
    checkSameNotary(inputs)
    if (changeOutput != null && changeOutput is FungibleToken) {
        check(inputs.filterIsInstance<StateAndRef<FungibleToken>>().sumTokenStateAndRefs() > changeOutput.amount) {
            "Change output should be less than sum of inputs."
        }
    }
    val firstState = inputs.first().state
    transactionBuilder.notary  = firstState.notary
    val issuerKey = firstState.data.issuer.owningKey
    val moveKeys = inputs.map { it.state.data.holder.owningKey }

    var inputIdx = transactionBuilder.inputStates().size
    val outputIdx = transactionBuilder.outputStates().size
    transactionBuilder.apply {
        val inputIndicies = inputs.map {
            addInputState(it)
            inputIdx++
        }
        val outputs = if (changeOutput != null) {
            addOutputState(changeOutput)
            listOf(outputIdx)
        } else {
            emptyList()
        }
        addCommand(RedeemTokenCommand(firstState.data.issuedTokenType, inputIndicies, outputs), moveKeys + issuerKey)
    }
    val states = inputs.map { it.state.data } + if (changeOutput == null) emptyList() else listOf(changeOutput)
    addTokenTypeJar(states, transactionBuilder)
    return transactionBuilder
}


/**
 * Redeem non-fungible [heldToken] issued by the [issuer] and add it to the [transactionBuilder].
 */
@Suspendable
fun addNonFungibleTokensToRedeem(
        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        heldToken: TokenType,
        issuer: Party
): TransactionBuilderWrapper {
    val heldTokenStateAndRef = serviceHub.vaultService.heldTokensByTokenIssuer(heldToken, issuer).states
    check(heldTokenStateAndRef.size == 1) {
        "Exactly one held token of a particular type $heldToken should be in the vault at any one time."
    }
    val nonFungibleState = heldTokenStateAndRef.first()
    transactionBuilder.notary  = nonFungibleState.state.notary
    generateExitNonFungible(transactionBuilder, nonFungibleState)
    return transactionBuilder
}

/**
 * Redeem amount of certain type of the token issued by [issuer]. Pay possible change to the [changeHolder] - it can be confidential identity.
 * Additional query criteria can be provided using [additionalQueryCriteria].
 */
@Suspendable
@JvmOverloads
fun addFungibleTokensToRedeem(
        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        amount: Amount<TokenType>,
        issuer: Party,
        changeHolder: AbstractParty,
        additionalQueryCriteria: QueryCriteria? = null
): TransactionBuilderWrapper {
    // TODO For now default to database query, but switch this line on after we can change API in 2.0
//    val selector: Selector = ConfigSelection.getPreferredSelection(serviceHub)
    val selector = DatabaseTokenSelection(serviceHub)
    val baseCriteria = tokenAmountWithIssuerCriteria(amount.token, issuer)
    val queryCriteria = additionalQueryCriteria?.let { baseCriteria.and(it) } ?: baseCriteria
    val fungibleStates = selector.selectTokens(amount, TokenQueryBy(issuer = issuer, queryCriteria = queryCriteria), transactionBuilder.lockId)
    checkSameNotary(fungibleStates)
    check(fungibleStates.isNotEmpty()) {
        "Received empty list of states to redeem."
    }
    val notary = fungibleStates.first().state.notary
    transactionBuilder.notary = notary
    val (exitStates, change) = selector.generateExit(
            exitStates = fungibleStates,
            amount = amount,
            changeHolder = changeHolder
    )

    addTokensToRedeem(transactionBuilder, exitStates, change)
    return transactionBuilder
}
