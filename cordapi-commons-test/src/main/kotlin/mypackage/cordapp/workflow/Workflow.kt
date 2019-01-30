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
package mypackage.cordapp.workflow

import co.paralleluniverse.fibers.Suspendable
import mypackage.cordapp.contract.YO_CONTRACT_ID
import mypackage.cordapp.contract.YoContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Create a Yo! between sender (initiating) and target parties
 */
@InitiatingFlow
@StartableByRPC
class YoFlow(val target: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a new Yo!")
        object SIGNING : ProgressTracker.Step("Verifying the Yo!")
        object COLLECTING : ProgressTracker.Step("Collecting signatures for Yo!")
        object FINALISING : ProgressTracker.Step("Sending the Yo!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, COLLECTING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING

        val me = serviceHub.myInfo.legalIdentities.first()
        // Retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        // Create the transaction components.
        val state = YoContract.YoState(me, target)
        val requiredSigners = listOf(ourIdentity.owningKey, target.owningKey)
        val command = Command(YoContract.Send(), requiredSigners)
        // Create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(state, YO_CONTRACT_ID)
                .addCommand(command)
        // Verify the transaction.
        progressTracker.currentStep = SIGNING

        // Sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Create a session with the other party.
        val otherPartySession = initiateFlow(target)

        // Obtain the counter party's signature.
        progressTracker.currentStep = COLLECTING
        val fullySignedTx = subFlow(CollectSignaturesFlow(
                signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // Finalising the transaction.
        subFlow(FinalityFlow(fullySignedTx, otherPartySession))
        return fullySignedTx
    }
}

/**
 * A basic responder for countersigning and listening for finality
 */
@InitiatedBy(YoFlow::class)
class YoFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Create our custom SignTransactionFlow
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // Ensure the transaction is sane
                //stx.tx.toLedgerTransaction(serviceHub).verify()
                // Ensure the transaction is a Yo
                val output = stx.tx.outputs.single().data
                "This must be a Yo transaction." using (output is YoContract.YoState)
            }
        }
        // Sign if the check is successful
        subFlow(signTransactionFlow)
        // Receive an update when done
        subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}
