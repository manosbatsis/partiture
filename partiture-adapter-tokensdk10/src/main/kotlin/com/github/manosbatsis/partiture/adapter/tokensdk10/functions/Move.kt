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
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.types.toPairs
import com.r3.corda.lib.tokens.workflows.utilities.heldTokenCriteria
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

/**
 *  Pretty much a coy of Corda's move functions
 *  to work around not having TX builder interface,
 *  so this is mostly R3's Copyright under Apache License Version 2.0
 *  see qlso https://github.com/corda/token-sdk/blob/master/LICENCE
 */

@Suspendable
fun addMoveTokens(
        transactionBuilder: TransactionBuilderWrapper,
        inputs: List<StateAndRef<AbstractToken>>,
        outputs: List<AbstractToken>
): TransactionBuilderWrapper {
    val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
    val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy {
        it.state.data.issuedTokenType
    }

    check(outputGroups.keys == inputGroups.keys) {
        "Input and output token types must correspond to each other when moving tokensToIssue"
    }

    transactionBuilder.apply {
        // Add a notary to the transaction.
        // TODO: Deal with notary change.
        notary = inputs.map { it.state.notary }.toSet().single()
        outputGroups.forEach { issuedTokenType: IssuedTokenType, outputStates: List<AbstractToken> ->
            val inputGroup = inputGroups[issuedTokenType]
                    ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
            val keys = inputGroup.map { it.state.data.holder.owningKey }

            var inputStartingIdx = inputStates().size
            var outputStartingIdx = outputStates().size

            val inputIdx = inputGroup.map {
                addInputState(it)
                inputStartingIdx++
            }

            val outputIdx = outputStates.map {
                transactionBuilder.addOutputState(it)
                outputStartingIdx++
            }

            addCommand(MoveTokenCommand(issuedTokenType, inputs = inputIdx, outputs = outputIdx), keys)
        }
    }

    addTokenTypeJar(inputs.map { it.state.data } + outputs, transactionBuilder)

    return transactionBuilder
}

/**
 * Adds a single token move to a transaction.
 */
@Suspendable
fun addMoveTokens(
        transactionBuilder: TransactionBuilderWrapper,
        input: StateAndRef<AbstractToken>,
        output: AbstractToken
): TransactionBuilderWrapper {
    return addMoveTokens(transactionBuilder = transactionBuilder, inputs = listOf(input), outputs = listOf(output))
}

/**
 * Adds multiple token moves to transaction. [partiesAndAmounts] parameter specify which parties should receive amounts of the token.
 * With possible change paid to [changeHolder]. This method will combine multiple token amounts from different issuers if needed.
 * If you would like to choose only tokens from one issuer you can provide optional [queryCriteria] for move generation.
 * Note: For now this method always uses database token selection, to use in memory one, use [addMoveTokens] with already selected
 * input and output states.
 */
@Suspendable
@JvmOverloads
fun addMoveFungibleTokens(
        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        changeHolder: AbstractParty,
        queryCriteria: QueryCriteria? = null
): TransactionBuilderWrapper {
    // TODO For now default to database query, but switch this line on after we can change API in 2.0
//    val selector: Selector = ConfigSelection.getPreferredSelection(serviceHub)
    val selector = DatabaseTokenSelection(serviceHub)
    val (inputs, outputs) = selector.generateMove(partiesAndAmounts.toPairs(), changeHolder, TokenQueryBy(queryCriteria = queryCriteria), transactionBuilder.lockId)
    return addMoveTokens(transactionBuilder = transactionBuilder, inputs = inputs, outputs = outputs)
}

/**
 * Add single move of [amount] of token to the new [holder]. Possible change output will be paid to [changeHolder].
 * This method will combine multiple token amounts from different issuers if needed.
 * If you would like to choose only tokens from one issuer you can provide optional [queryCriteria] for move generation.
 * Note: For now this method always uses database token selection, to use in memory one, use [addMoveTokens] with already selected
 * input and output states.
 */
@Suspendable
@JvmOverloads
fun addMoveFungibleTokens(        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        amount: Amount<TokenType>,
        holder: AbstractParty,
        changeHolder: AbstractParty,
        queryCriteria: QueryCriteria? = null
): TransactionBuilderWrapper {
    return addMoveFungibleTokens(
            transactionBuilder = transactionBuilder,
            serviceHub = serviceHub,
            partiesAndAmounts = listOf(PartyAndAmount(holder, amount)),
            changeHolder = changeHolder,
            queryCriteria = queryCriteria
    )
}

/* For non-fungible tokens. */

/**
 * Add single move of [token] to the new [holder].
 * Provide optional [queryCriteria] for move generation.
 */
@Suspendable
@JvmOverloads
fun addMoveNonFungibleTokens(
        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        token: TokenType,
        holder: AbstractParty,
        queryCriteria: QueryCriteria? = null
): TransactionBuilderWrapper {
    return generateMoveNonFungible(transactionBuilder, PartyAndToken(holder, token), serviceHub.vaultService, queryCriteria)
}

/**
 * Add single move of token to the new holder specified using [partyAndToken] parameter.
 * Provide optional [queryCriteria] for move generation.
 */
@Suspendable
@JvmOverloads
fun addMoveNonFungibleTokens(
        transactionBuilder: TransactionBuilderWrapper,
        serviceHub: ServiceHub,
        partyAndToken: PartyAndToken,
        queryCriteria: QueryCriteria? = null
): TransactionBuilderWrapper {
    return generateMoveNonFungible(transactionBuilder, partyAndToken, serviceHub.vaultService, queryCriteria)
}

@Suspendable
fun generateMoveNonFungible(
        partyAndToken: PartyAndToken,
        vaultService: VaultService,
        queryCriteria: QueryCriteria?
): Pair<StateAndRef<NonFungibleToken>, NonFungibleToken> {
    val query = queryCriteria ?: heldTokenCriteria(partyAndToken.token)
    val criteria = heldTokenCriteria(partyAndToken.token).and(query)
    val nonFungibleTokens = vaultService.queryBy<NonFungibleToken>(criteria).states
    // There can be multiple non-fungible tokens of the same TokenType. E.g. There can be multiple House tokens, each
    // with a different address. Whilst they have the same TokenType, they are still non-fungible. Therefore care must
    // be taken to ensure that only one token is returned for each query. As non-fungible tokens are also LinearStates,
    // the linearID can be used to ensure you only get one result.
    require(nonFungibleTokens.size == 1) { "Your query wasn't specific enough and returned multiple non-fungible tokens." }
    val input = nonFungibleTokens.single()
    val nonFungibleState = input.state.data
    val output = nonFungibleState.withNewHolder(partyAndToken.party)
    return Pair(input, output)
}

@Suspendable
fun generateMoveNonFungible(
        transactionBuilder: TransactionBuilderWrapper,
        partyAndToken: PartyAndToken,
        vaultService: VaultService,
        queryCriteria: QueryCriteria?
): TransactionBuilderWrapper {
    val (input, output) = generateMoveNonFungible(partyAndToken, vaultService, queryCriteria)
    val notary = input.state.notary
    addTokenTypeJar(listOf(input.state.data, output), transactionBuilder)
    transactionBuilder.notary = notary
    val signingKey = input.state.data.holder.owningKey

    return transactionBuilder.apply {
        val currentInputSize = inputStates().size
        val currentOutputSize = outputStates().size
        addInputState(input)
        addOutputState(state = output withNotary notary)
        addCommand(MoveTokenCommand(output.token, inputs = listOf(currentInputSize), outputs = listOf(currentOutputSize)), signingKey)
    }
}

// All check should be performed before.
@Suspendable
fun generateExitNonFungible(txBuilder: TransactionBuilderWrapper, moveStateAndRef: StateAndRef<NonFungibleToken>) {
    val nonFungibleToken = moveStateAndRef.state.data // TODO What if redeeming many non-fungible assets.
    addTokenTypeJar(nonFungibleToken, txBuilder)
    val issuerKey = nonFungibleToken.token.issuer.owningKey
    val moveKey = nonFungibleToken.holder.owningKey
    txBuilder.apply {
        val currentInputSize = inputStates().size
        addInputState(moveStateAndRef)
        addCommand(RedeemTokenCommand(nonFungibleToken.token, listOf(currentInputSize)), issuerKey, moveKey)
    }
}

fun addTokenTypeJar(tokens: List<AbstractToken>, transactionBuilder: TransactionBuilderWrapper) {
    tokens.forEach {
        // If there's no JAR hash then we don't need to do anything.
        val hash = it.tokenTypeJarHash ?: return
        if (!transactionBuilder.attachments().contains(hash)) {
            transactionBuilder.addAttachment(hash)
        }
    }
}

fun addTokenTypeJar(tokens: Iterable<StateAndRef<AbstractToken>>, transactionBuilder: TransactionBuilderWrapper) {
    addTokenTypeJar(tokens.map { it.state.data }, transactionBuilder)
}

fun addTokenTypeJar(changeOutput: AbstractToken, transactionBuilder: TransactionBuilderWrapper) {
    addTokenTypeJar(listOf(changeOutput), transactionBuilder)
}

fun addTokenTypeJar(input: StateAndRef<AbstractToken>, transactionBuilder: TransactionBuilderWrapper) {
    addTokenTypeJar(input.state.data, transactionBuilder)
}
