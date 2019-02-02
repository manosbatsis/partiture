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
package com.github.manosbatsis.partiture.flow.tx

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
 * A [TransactionBuilder] implementation with convenient access to `participants` of all
 * input and output states that have already been added at any point.
 *
 * Since `addOutput` methods cannot be overridden, this implementation caches the participants
 * in a backing property and uses in/out state counts to manage the cache.
 *
 * This implementation will not work properly when `copy` or `withItems` are used,
 * see also https://github.com/corda/corda/issues/4708
 */
class ParticipantsAwareTransactionBuilder(
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
) : TransactionBuilder(notary, lockId, inputs, attachments, outputs, commands, window, privacySalt, references, serviceHub) {

    constructor(notary: Party) : this(notary, window = null)

    private companion object {
        private fun defaultLockId() = (Strand.currentStrand() as? FlowStateMachine<*>)?.id?.uuid ?: UUID.randomUUID()
    }

    /** Keep a record of input state participants */
    private val inputParticipants = mutableSetOf<AbstractParty>()

    /** Latest count of input states taken when providing and caching participants. Used in cache validity check. */
    private var lastInStateCount = 0
    /** Latest count of output states taken when providing and caching participants. Used in cache validity check. */
    private var lastOutStateCount = 0
    /** The cached list of participants. */
    private var cachedParticipants: List<AbstractParty> = listOf<AbstractParty>()

    /** Provides the participants of all input and output states */
    var participants: List<AbstractParty> = cachedParticipants
        private set
        get() {
            // if valid cache
            return if (lastInStateCount == inputs.size && lastOutStateCount == outputs.size) cachedParticipants
            // refresh cache
            else {
                lastInStateCount = inputs.size; lastOutStateCount = outputs.size
                cachedParticipants = outputs.flatMap { it.data.participants }.union(inputParticipants).distinct()
                cachedParticipants
            }
        }

    /** Adds an input [StateRef] to the transaction, taking note of it's participants. */
    override fun addInputState(stateAndRef: StateAndRef<*>): ParticipantsAwareTransactionBuilder {
        inputParticipants.addAll(stateAndRef.state.data.participants)
        super.addInputState(stateAndRef)
        return this
    }

    /** Adds a [Command] created using the given [CommandData] and currently known participants */
    fun addCommandFromData(commandData: CommandData): ParticipantsAwareTransactionBuilder {
        super.addCommand(Command(commandData, this.participants.map { it.owningKey }))
        return this
    }

}