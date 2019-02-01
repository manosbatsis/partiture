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
package com.github.manosbatsis.cordapi.commons.flow.tx

import co.paralleluniverse.strands.Strand
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.FlowStateMachine
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * A [TransactionBuilder] with convenient access to state participants
 */
class SignersAwareTransactionBuilder(
        notary: Party? = null,
        lockId: UUID = defaultLockId(),
        inputs: MutableList<StateRef> = arrayListOf(),
        attachments: MutableList<SecureHash> = arrayListOf(),
        outputs: MutableList<TransactionState<ContractState>> = arrayListOf(),
        commands: MutableList<Command<*>> = arrayListOf(),
        window: TimeWindow? = null,
        privacySalt: PrivacySalt = PrivacySalt(),
        references: MutableList<StateRef> = arrayListOf(),
        serviceHub: ServiceHub? = (Strand.currentStrand() as? FlowStateMachine<*>)?.serviceHub
): TransactionBuilder(notary, lockId, inputs, attachments, outputs, commands, window, privacySalt, references, serviceHub) {

    constructor(notary: Party) : this(notary, window = null)

    private companion object {
        private fun defaultLockId() = (Strand.currentStrand() as? FlowStateMachine<*>)?.id?.uuid ?: UUID.randomUUID()
    }

    private val inputParticipants = mutableSetOf<AbstractParty>()

    /** Adds an input [StateRef] to the transaction. */
    override fun addInputState(stateAndRef: StateAndRef<*>): TransactionBuilder {
        inputParticipants.addAll(stateAndRef.state.data.participants)
        return super.addInputState(stateAndRef)
    }

    /** Return participants of both input and output states */
    fun participants(): List<AbstractParty> {
        return this.outputs.flatMap { it.data.participants }.union(this.inputParticipants).distinct()
    }
}