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

import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.PartitureResponderFlow
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.call.CallContextEntry
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.FinalizedTxOutputConverter
import com.github.manosbatsis.partiture.flow.tx.initiating.ParticipantsAwareTransactionBuilder
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleResponderTxStrategy
import com.github.manosbatsis.partiture.flow.tx.responder.SimpleTypeCheckingResponderTxStrategy
import mypackage.cordapp.contract.YO_CONTRACT_ID
import mypackage.cordapp.contract.YoContract
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class YoFlow(
        input: List<Party>
//  Flow:         IN ,         OUT
) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        input = input, // Input can be anything
        inputConverter = YoInputConverter(),// Our custom IN converter
        outputConverter = FinalizedTxOutputConverter()) // OUT build-in converter

class YoInputConverter : PartitureFlowDelegateBase(), InputConverter<List<Party>> {
    override fun convert(input: List<Party>): CallContext {
        val entries = input.map { party ->
            // Prepare a TX builder
            val txBuilder = ParticipantsAwareTransactionBuilder(clientFlow.getFirstNotary())
            txBuilder.addOutputState(YoContract.YoState(clientFlow.ourIdentity, party), YO_CONTRACT_ID)
            txBuilder.addCommandFromData(YoContract.Send())
            // Return a TX context with builder and participants
            CallContextEntry(txBuilder, txBuilder.participants)
        }
        return CallContext(entries)
    }
}

/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(YoFlow::class)
class YoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                YoContract.YoState::class.java)
)


/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class ManualConversionYoFlow(
        input: List<Party>
//  Flow:         IN ,         OUT
) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        input = input, // IN can be anything
        // Build-in OUT converter
        outputConverter = FinalizedTxOutputConverter()
) {
    /** Override to manually init the flow's CallContext */
    override fun processInput(): CallContext {
        val entries = input.map { party ->
            // Prepare a TX builder
            val txBuilder = ParticipantsAwareTransactionBuilder(getFirstNotary())
            txBuilder.addOutputState(YoContract.YoState(ourIdentity, party), YO_CONTRACT_ID)
            txBuilder.addCommandFromData(YoContract.Send())
            // Return a call context/TX entry context with builder and participants
            CallContextEntry(txBuilder, txBuilder.participants)
        }
        return CallContext(entries)
    }
}

/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(ManualConversionYoFlow::class)
class ManualConversionYoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleResponderTxStrategy()
)

