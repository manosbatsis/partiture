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
package com.github.manosbatsis.partiture.flow.util

import net.corda.confidential.IdentitySyncFlow
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker

class ProgressTrackerUtil {
    companion object {
        object INITIALISE : ProgressTracker.Step("Initializing.")
        object PROCESS_INPUT : ProgressTracker.Step("Processing io.")
        object PREPARE_TRANSACTION_DATA : ProgressTracker.Step("Preparing transaction data.")
        object SIGN_INITIAL_TX : ProgressTracker.Step("Signing initial transaction.")
        object CREATE_SESSIONS : ProgressTracker.Step("Creating counter-party sessions.")
        object GATHER_SIGNATURES : ProgressTracker.Step("Collecting counter-party signatures.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object SYNC_IDENTITIES : ProgressTracker.Step("Syncing identities.") {
            override fun childProgressTracker() = IdentitySyncFlow.Send.tracker()
        }

        object VERIFY_TRANSACTION_DATA : ProgressTracker.Step("Verifying transaction data.")
        object FINALIZE : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        object PROCESS_OUTPUT : ProgressTracker.Step("Process and return output.")

        fun defaultProgressTracker() = ProgressTracker(
                INITIALISE,
                PROCESS_INPUT,
                PREPARE_TRANSACTION_DATA,
                SIGN_INITIAL_TX,
                CREATE_SESSIONS,
                GATHER_SIGNATURES,
                SYNC_IDENTITIES,
                VERIFY_TRANSACTION_DATA,
                FINALIZE,
                PROCESS_OUTPUT)
    }
}