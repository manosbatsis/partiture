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
 *     AccountYou should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package mypackage.cordapp.workflow


import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.SingleFinalizedTxOutputConverter
import com.github.manosbatsis.partiture.flow.io.output.TypedOutputStatesConverter
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import com.github.manosbatsis.partiture.flow.util.PartitureAccountsAwareFlow
import com.github.manosbatsis.vaultaire.annotation.VaultaireFlowResponder
import mypackage.cordapp.contract.ACCOUNT_YO_CONTRACT_ID
import mypackage.cordapp.contract.AccountYoContract
import mypackage.cordapp.contract.AccountYoContract.AccountYoState
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

open class BaseAccountYoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                AccountYoState::class.java)
)

class AccountYoInputConverter : PartitureFlowDelegateBase(), InputConverter<AccountYoStateClientDto> {

    @Suspendable
    override fun convert(input: AccountYoStateClientDto): CallContext {
        val stateService =AccountYoStateService(clientFlow.serviceHub)
        val contractState = input.toTargetType(stateService)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(contractState, ACCOUNT_YO_CONTRACT_ID)
                .addCommand(AccountYoContract.Send())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

class AccountYoInputsConverter : PartitureFlowDelegateBase(), InputConverter<Iterable<AccountYoStateClientDto>> {

    @Suspendable
    override fun convert(inputs: Iterable<AccountYoStateClientDto>): CallContext {
        val stateService = AccountYoStateService(clientFlow.serviceHub)
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
        for(input in inputs) txBuilder
                .addOutputState(input.toTargetType(stateService), ACCOUNT_YO_CONTRACT_ID)
        txBuilder.addCommand(AccountYoContract.Send())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}


/**
 * Create a AccountYo! transaction/state for each input recipient/party.
 * Tests SingleFinalizedTxOutputConverter
 */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseAccountYoFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class AccountYoFlow1(input: AccountYoStateClientDto) : PartitureAccountsAwareFlow<AccountYoStateClientDto, SignedTransaction>(
        input = input, // Input can be anything
        inputConverter = AccountYoInputConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = SingleFinalizedTxOutputConverter())



/**
 * Create a AccountYo! transaction/state for each input recipient/party.
 * Tests TypedOutputStatesConverter
 */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseAccountYoFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class AccountYoFlow2(input: AccountYoStateClientDto) : PartitureAccountsAwareFlow<AccountYoStateClientDto, List<AccountYoContract.AccountYoState>>(
        input = input, // Input can be anything
        inputConverter = AccountYoInputConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = TypedOutputStatesConverter(AccountYoState::class.java))

/**
 * Create a AccountYo! transaction/state for each input recipient/party.
 * Tests TypedOutputStatesConverter
 */
@InitiatingFlow
@StartableByRPC
@VaultaireFlowResponder(
        value = BaseAccountYoFlowResponder::class,
        comment = "A basic responder for countersigning and listening for finality"
)
class AccountYoFlow3(input: Iterable<AccountYoStateClientDto>) : PartitureAccountsAwareFlow<Iterable<AccountYoStateClientDto>, List<AccountYoState>>(
        input = input, // Input can be anything
        inputConverter = AccountYoInputsConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = TypedOutputStatesConverter(AccountYoState::class.java))
