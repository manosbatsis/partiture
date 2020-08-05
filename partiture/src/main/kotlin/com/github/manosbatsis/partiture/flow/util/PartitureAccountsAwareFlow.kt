package com.github.manosbatsis.partiture.flow.util

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.partiture.flow.io.input.InputConverter
import com.github.manosbatsis.partiture.flow.io.output.OutputConverter
import com.github.manosbatsis.partiture.flow.tx.initiating.SimpleTxStrategy
import com.github.manosbatsis.partiture.flow.tx.initiating.TxStrategy
import com.github.manosbatsis.partiture.flow.util.IdentitySyncMode.SKIP
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey



/**
 * Extends [PartitureFlow] to provide a Corda Accounts-aware,
 * base [FlowLogic] implementation for flow composition
 */
abstract class PartitureAccountsAwareFlow<IN, OUT>(
        input: IN,
        txStrategy: TxStrategy = SimpleTxStrategy(),
        inputConverter: InputConverter<IN>? = null,
        outputConverter: OutputConverter<OUT>?
) : PartitureFlow<IN, OUT>(
        input, txStrategy, inputConverter, outputConverter, SKIP
){

    /** Assume all participants are accounts and need a [FlowSession] */
    @Suspendable
    override fun createFlowSessions(
            counterParties: Iterable<AbstractParty>
    ): Set<FlowSession> {
        val parties = (counterParties)
        val sessions = mutableSetOf<FlowSession>()
        for(party in parties){
            sessions.add(initiateFlow(party))
        }
        return sessions
    }

    /** Use both our account and node identity keys in context */
    @Suspendable
    override fun ourParticipatingKeys(ourParties: List<AbstractParty>): Iterable<PublicKey> {
        val identityService = serviceHub.identityService
        val nodeKeys = ourParties
                .filterIsInstance(AnonymousParty::class.java)
                .map { identityService.requireWellKnownPartyFromAnonymous(AnonymousParty(it.owningKey)) }
                .map { it.owningKey }
        val accountKeys = ourParties.map { it.owningKey }
        return nodeKeys + accountKeys

    }
}