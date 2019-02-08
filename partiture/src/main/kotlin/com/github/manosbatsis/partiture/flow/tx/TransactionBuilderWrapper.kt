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
package com.github.manosbatsis.partiture.flow.tx

import co.paralleluniverse.strands.Strand
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.FlowStateMachine
import net.corda.core.internal.toWireTransaction
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.contextLogger
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Wraps [TransactionBuilder] to provide convenient access to `participants` of all
 * input and output states (but not `StateRef `s) that have already been added at any point.
 */
class TransactionBuilderWrapper() {

    private companion object {
        private fun defaultLockId() = (Strand.currentStrand() as? FlowStateMachine<*>)?.id?.uuid ?: UUID.randomUUID()
        private val log = contextLogger()
    }

    constructor(notary: Party) : this(notary, window = null)

    constructor(notary: Party? = null,
                lockId: UUID = defaultLockId(),
                inputs: MutableList<StateRef> = arrayListOf(),
                attachments: MutableList<SecureHash> = arrayListOf(),
                outputs: MutableList<TransactionState<ContractState>> = arrayListOf(),
                commands: MutableList<Command<*>> = arrayListOf(),
                window: TimeWindow? = null,
                privacySalt: PrivacySalt = PrivacySalt()
    ) : this(notary, lockId, inputs, attachments, outputs, commands, window, privacySalt, arrayListOf())

    constructor(
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
    ) : this (
            TransactionBuilder(
                    notary = notary,
                    lockId = lockId,
                    inputs = inputs,
                    attachments = attachments,
                    outputs = outputs,
                    commands = commands,
                    window = window,
                    privacySalt = privacySalt,
                    references = references,
                    serviceHub = serviceHub))

    private constructor(transactionBuilder: TransactionBuilder, participants: Iterable<AbstractParty>? = null) : this() {
        this.transactionBuilder = transactionBuilder
        if(participants != null) this.participants.addAll(participants)
    }

    /** Create a [TransactionBuilder] */
    private lateinit var transactionBuilder: TransactionBuilder

    /** Provides the participants of all input and output states - but not StateRefs */
    private val participants: MutableSet<AbstractParty> = mutableSetOf()

    /** Adds a [Command] created using the given [CommandData] and currently known participants */
    fun addAttachment(attachmentId: SecureHash): TransactionBuilderWrapper {
        transactionBuilder.addAttachment(attachmentId)
        return this
    }

    /** Adds an output state to the transaction. */
    fun addOutputState(state: TransactionState<*>): TransactionBuilderWrapper {
        participants.addAll(state.data.participants)
        transactionBuilder.addOutputState(state)
        return this
    }

    /** Adds an output state, with associated contract code (and constraints), and notary, to the transaction. */
    fun addOutputState(
            state: ContractState,
            contract: ContractClassName ,
            notary: Party,
            encumbrance: Int? = null,
            constraint: AttachmentConstraint
    ): TransactionBuilderWrapper {
        participants.addAll(state.participants)
        transactionBuilder.addOutputState(state, contract, notary, encumbrance, constraint)
        return this
    }

    /** A default notary must be specified during builder construction to use this method */
    fun addOutputState(
            state: ContractState,
            contract: ContractClassName,
            constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ): TransactionBuilderWrapper {
        participants.addAll(state.participants)
        transactionBuilder.addOutputState(state, contract, constraint = constraint)
        return this
    }

    /** Adds a [Command] to the transaction. */
    fun addCommand(arg: Command<*>): TransactionBuilderWrapper {
        transactionBuilder.addCommand(arg)
        return this
    }

    /**
     * Adds a [Command] to the transaction, specified by the encapsulated [CommandData] object and required list of
     * signing [PublicKey]s.
     */
    fun addCommand(data: CommandData, vararg keys: PublicKey): TransactionBuilderWrapper = addCommand(Command(data, listOf(*keys)))
    fun addCommand(data: CommandData, keys: List<PublicKey>): TransactionBuilderWrapper = addCommand(Command(data, keys))

    /**
     * Sets the [TimeWindow] for this transaction, replacing the existing [TimeWindow] if there is one. To be valid, the
     * transaction must then be signed by the notary service within this window of time. In this way, the notary acts as
     * the Timestamp Authority.
     */
    fun setTimeWindow(timeWindow: TimeWindow) : TransactionBuilderWrapper {
        transactionBuilder.setTimeWindow(timeWindow)
        return this
    }

    /**
     * The [TimeWindow] for the transaction can also be defined as [time] +/- [timeTolerance]. The tolerance should be
     * chosen such that your code can finish building the transaction and sending it to the Timestamp Authority within
     * that window of time, taking into account factors such as network latency. Transactions being built by a group of
     * collaborating parties may therefore require a higher time tolerance than a transaction being built by a single
     * node.
     */
    fun setTimeWindow(time: Instant, timeTolerance: Duration): TransactionBuilderWrapper = setTimeWindow(TimeWindow.withTolerance(time, timeTolerance))

    fun setPrivacySalt(privacySalt: PrivacySalt): TransactionBuilderWrapper {
        transactionBuilder.setPrivacySalt(privacySalt)
        return this
    }

    /** Adds an input [StateRef] to the transaction, taking note of it's participants. */
    fun addInputState(stateAndRef: StateAndRef<*>): TransactionBuilderWrapper {
        participants.addAll(stateAndRef.state.data.participants)
        transactionBuilder.addInputState(stateAndRef)
        return this
    }

    /**
     * Adds a reference input [StateRef] to the transaction.
     *
     * Note: Reference states are only supported on Corda networks running a minimum platform version of 4.
     * [toWireTransaction] will throw an [IllegalStateException] if called in such an environment.
     */
    fun addReferenceState(referencedStateAndRef: ReferencedStateAndRef<*>): TransactionBuilderWrapper {
        transactionBuilder.addReferenceState(referencedStateAndRef)
        return this
    }

    /** Adds a [Command] created using the given [CommandData] and currently known participants */
    fun addCommand(commandData: CommandData): TransactionBuilderWrapper {
        transactionBuilder.addCommand(Command(commandData, this.participants.map { it.owningKey }))
        return this
    }

    /** Returns an immutable list of input [StateRef]s. */
    fun inputStates(): List<StateRef> = transactionBuilder.inputStates()

    /** Returns an immutable list of reference input [StateRef]s. */
    fun referenceStates(): List<StateRef> = transactionBuilder.referenceStates()

    /** Returns an immutable list of attachment hashes. */
    fun attachments(): List<SecureHash> = transactionBuilder.attachments()

    /** Returns an immutable list of output [TransactionState]s. */
    fun outputStates(): List<TransactionState<*>> = transactionBuilder.outputStates()

    /** Returns an immutable list of [Command]s, grouping by [CommandData] and joining signers (from v4, v3 and below return all commands with duplicates for different signers). */
    fun commands(): List<Command<*>> = transactionBuilder.commands()

    fun withItems(vararg items: Any) = apply {
        for (t in items) {
            when (t) {
                is StateAndRef<*> -> addInputState(t)
                is ReferencedStateAndRef<*> -> addReferenceState(t)
                is SecureHash -> addAttachment(t)
                is TransactionState<*> -> addOutputState(t)
                is StateAndContract -> addOutputState(t.state, t.contract)
                is ContractState -> throw UnsupportedOperationException("Removed as of V1: please use a StateAndContract instead")
                is Command<*> -> addCommand(t)
                is CommandData -> throw IllegalArgumentException("You passed an instance of CommandData, but that lacks the pubkey. You need to wrap it in a Command object first.")
                is TimeWindow -> setTimeWindow(t)
                is PrivacySalt -> setPrivacySalt(t)
                else -> throw IllegalArgumentException("Wrong argument type: ${t.javaClass}")
            }
        }
    }

    /** Creates a copy of the wrapper */
    fun copy(): TransactionBuilderWrapper =
            TransactionBuilderWrapper(
                    transactionBuilder = transactionBuilder.copy(),
                    participants = participants
            )

    /** Get the inner [TransactionBuilder] */
    fun transactionBuilder() = this.transactionBuilder
    /** Get the participants of current input/output states */
    fun participants() = this.participants
}