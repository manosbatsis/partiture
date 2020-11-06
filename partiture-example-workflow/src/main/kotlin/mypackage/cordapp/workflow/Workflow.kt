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
package mypackage.cordapp.workflow


import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.SingleFinalizedTxOutputConverter
import com.github.manosbatsis.partiture.flow.io.output.TypedOutputStatesConverter
import com.github.manosbatsis.partiture.flow.tx.TransactionBuilderWrapper
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import mypackage.cordapp.contract.YO_CONTRACT_ID
import mypackage.cordapp.contract.YoContract
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/** Used as flow input, to send a recipient a message */
data class YoMessage(
        val recepient: Party,
        val message: String
)

class YoInputConverter : PartitureFlowDelegateBase(), InputConverter<YoMessage> {

    @Suspendable
    override fun convert(input: YoMessage): CallContext {
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
                .addOutputState(
                        YoContract.YoState(clientFlow.ourIdentity, input.recepient, input.message),
                        YO_CONTRACT_ID)
                .addCommand(YoContract.Send())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

class YoInputsConverter : PartitureFlowDelegateBase(), InputConverter<Iterable<YoContract.YoState>> {

    @Suspendable
    override fun convert(inputs: Iterable<YoContract.YoState>): CallContext {
        // Prepare a TX builder
        val txBuilder = TransactionBuilderWrapper(clientFlow.getFirstNotary())
        for(input in inputs) txBuilder.addOutputState(input, YO_CONTRACT_ID)
        txBuilder.addCommand(YoContract.Send())
        // Return a TX context with builder and participants
        return CallContext(CallContextEntry(txBuilder))
    }
}

open class BaseYoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                YoContract.YoState::class.java)
)

/**
 * Create a Yo! transaction/state for each input recipient/party.
 * Tests SingleFinalizedTxOutputConverter
 */
@InitiatingFlow
@StartableByRPC
class YoFlow1(input: YoMessage) : PartitureFlow<YoMessage, SignedTransaction>(
        input = input, // Input can be anything
        inputConverter = YoInputConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = SingleFinalizedTxOutputConverter())


/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(YoFlow1::class)
class YoFlow1Responder(otherPartySession: FlowSession) : BaseYoFlowResponder(otherPartySession)


/**
 * Create a Yo! transaction/state for each input recipient/party.
 * Tests TypedOutputStatesConverter
 */
@InitiatingFlow
@StartableByRPC
class YoFlow2(input: YoMessage) : PartitureFlow<YoMessage, List<YoContract.YoState>>(
        input = input, // Input can be anything
        inputConverter = YoInputConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = TypedOutputStatesConverter(YoContract.YoState::class.java))

/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(YoFlow2::class)
class YoFlow2Responder(otherPartySession: FlowSession) : BaseYoFlowResponder(otherPartySession)

/**
 * Create a Yo! transaction/state for each input recipient/party.
 * Tests TypedOutputStatesConverter
 */
@InitiatingFlow
@StartableByRPC
class YoFlow3(input: Iterable<YoContract.YoState>) : PartitureFlow<Iterable<YoContract.YoState>, List<YoContract.YoState>>(
        input = input, // Input can be anything
        inputConverter = YoInputsConverter(),// Our custom IN converter
        // OUT build-in converter
        outputConverter = TypedOutputStatesConverter(YoContract.YoState::class.java))

/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(YoFlow3::class)
class YoFlow3Responder(otherPartySession: FlowSession) : BaseYoFlowResponder(otherPartySession)
