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
package com.github.manosbatsis.partiture.flow.lifecycle

import net.corda.confidential.IdentitySyncFlow
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker

/**
 * Declares the default progress tracker steps
 * and a method to obtain a [ProgressTracker] containing them.
 */
object SimpleInitiatingLifecycle : Lifecycle {
    object INITIALIZE : ProgressTracker.Step("Initializing.")
    object PROCESS_INPUT : ProgressTracker.Step("Processing input.")
    object POST_PROCESS_INPUT : ProgressTracker.Step("Input post-processing.")
    object EXECUTE_TRANSACTIONS : ProgressTracker.Step("Executing transactions.")
    object TRANSACTION_SIGN_INITIAL : ProgressTracker.Step("Signing initial transaction.")
    object TRANSACTION_CREATE_SESSIONS : ProgressTracker.Step("Creating counter-party sessions.")
    object SYNC_IDENTITIES : ProgressTracker.Step("Syncing identities.") {
        override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
    }

    object TRANSACTION_GATHER_SIGNATURES : ProgressTracker.Step("Collecting counter-party signatures.") {
        override fun childProgressTracker() = CollectSignaturesFlow.tracker()
    }

    object TRANSACTION_VERIFY_SIGNATURES : ProgressTracker.Step("Verify the transaction's signatures.")
    object TRANSACTION_VERIFY_DATA : ProgressTracker.Step("Verifying transaction data.")
    object TRANSACTION_FINALIZE : ProgressTracker.Step("Finalising transaction.") {
        override fun childProgressTracker() = FinalityFlow.tracker()
    }

    object TRANSACTION_POST_EXECUTE : ProgressTracker.Step("Perform transactions post-processing.")
    object PROCESS_OUTPUT : ProgressTracker.Step("Process output.")

    /** Create a [ProgressTracker] with the appropriate steps */
    override fun progressTracker() = ProgressTracker(
            INITIALIZE,
            PROCESS_INPUT,
            POST_PROCESS_INPUT,
            EXECUTE_TRANSACTIONS,
            TRANSACTION_SIGN_INITIAL,
            TRANSACTION_CREATE_SESSIONS,
            SYNC_IDENTITIES,
            TRANSACTION_GATHER_SIGNATURES,
            TRANSACTION_VERIFY_SIGNATURES,
            TRANSACTION_VERIFY_DATA,
            TRANSACTION_FINALIZE,
            TRANSACTION_POST_EXECUTE,
            PROCESS_OUTPUT)
}