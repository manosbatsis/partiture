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

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.IdentityService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import java.security.PublicKey

/**
 * Base [FlowLogic] implementation, includes common utilities
 */
abstract class PartitureUtilsFlowLogic<out T> : FlowLogic<T>() {


    /** Get a list of nodes in the network, including self and notaries */
    fun allNodes(): List<Party> {
        val nodes = serviceHub.networkMapCache.allNodes
        return nodes.map { it.legalIdentities.first() }

    }

    /** Get a list of network peers, i.e. nodes excluding self and notaries  */
    fun peers(): List<Party> {
        val notaries = serviceHub.networkMapCache.notaryIdentities
        val nodes = serviceHub.networkMapCache.allNodes
        val myIdentity = this.ourIdentity
        return nodes.filter { nodeInfo ->
            // Filter out self and notaries
            nodeInfo.legalIdentities.find {
                it == myIdentity || notaries.contains(it)
            } == null
        }.map { it.legalIdentities.first() }
    }

    /** Filter the participants to get a [FlowSession] per counter-party. */
    @Suspendable
    open fun createFlowSessions(participants: Iterable<AbstractParty>): Set<FlowSession> {
        val myKeys = serviceHub.keyManagementService.filterMyKeys(participants.map { it.owningKey })
        val filtered = participants
                .map { toWellKnownParty(it) }
                //.filter { it.owningKey != ourIdentity.owningKey }
                .filter { !myKeys.contains(it.owningKey) }
        val sessions = mutableSetOf<FlowSession>()
        for(party in filtered){
            sessions.add(initiateFlow(party))
        }
        return sessions
    }


    /** Push our identities to the given counter-party sessions */
    @Suspendable
    open fun pushOurIdentities(sessions: Set<FlowSession>, tx: WireTransaction, progressTracker: ProgressTracker) {
        subFlow(IdentitySyncFlow.Send(sessions, tx, progressTracker))

    }

    /** Perform initial signing to the given mutable transaction */
    @Suspendable
    open fun signInitialTransaction(
            transactionBuilder: TransactionBuilder,
            signingPubKeys: Iterable<PublicKey>
    ): SignedTransaction? {
        return serviceHub.signInitialTransaction(transactionBuilder, signingPubKeys)
    }

    /** Finalize the given transaction */
    @Suspendable
    open fun finalizeTransaction(
            tx: SignedTransaction, sessions: Set<FlowSession>, tracker: ProgressTracker
    ): SignedTransaction {
        return subFlow(FinalityFlow(tx, sessions, tracker))
    }

    /**
     * Resolve to a well known party
     * @param name an organization or X500 name
     * @throws [RuntimeException] if a party cannot be resolved
     */
    fun toWellKnownParty(name: String): Party =
        if(name.contains("O=")) toWellKnownParty(CordaX500Name.parse(name))
        else getParty(name, serviceHub.identityService)


    /**
     * Resolve to a well known party
     * @throws [RuntimeException] if a party cannot be resolved
     */
    fun toWellKnownParty(x500Name: CordaX500Name): Party =
            serviceHub.identityService.wellKnownPartyFromX500Name(x500Name)
                    ?: throw RuntimeException("Could resolve to known party: ${x500Name}")

    /**
     * Resolve the given parties
     * @throws [RuntimeException] if a party cannot be resolved
     */
    fun toWellKnownPartiesFromName(parties: Iterable<CordaX500Name>): List<Party> =
            parties.map { toWellKnownParty(it) }

    /**
     * Resolve to a known party if [AnonymousParty], return as-is (i.e. [Party]) otherwise
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownParty(abstractParty: AbstractParty): Party =
            abstractParty as? Party
                    ?: serviceHub.identityService
                            .requireWellKnownPartyFromAnonymous(abstractParty)

    /**
     * Resolve the given parties
     * @throws [RuntimeException] if an anonymous party cannot be resolved
     */
    fun toWellKnownParties(parties: Iterable<AbstractParty>): List<Party> =
            parties.map { toWellKnownParty(it) }

    /** Filter out self from said parties */
    fun Iterable<AbstractParty>.exceptMe(): List<AbstractParty> {
        val myKeys = serviceHub.keyManagementService.filterMyKeys(this.map { it.owningKey })
        return this.filter { !myKeys.contains(it.owningKey) }
    }

    /**
     * Get the first notary found in the network map if it exists
     * @return the first notary if any exists, `null` otherwise
     */
    fun findFirstNotary(): Party? = serviceHub.networkMapCache.notaryIdentities.firstOrNull()

    /**
     * Get the first notary found in the network map
     * @throws [RuntimeException] if no notaries are found
     */
    fun getFirstNotary(): Party = findFirstNotary()
            ?: throw RuntimeException("No notaries found in network map cache")

    /**
     * Return the single notary found in the network map,
     * throw an exception if none or more than one eist.
     * @return the single notary if only one exists, throw an error otherwise
     */
    fun getSingleNotary(): Party = serviceHub.networkMapCache.notaryIdentities.single()

    /**
     * Get the first notary matching the given organisation name if it exists
     * @return the first matching notary if any exists, `null` otherwise
     */
    fun findNotaryByOrganisation(organisation: String): Party? =
            serviceHub.networkMapCache.notaryIdentities.firstOrNull { it.name.organisation == organisation }

    /**
     * Get the first notary matching the given organisation name
     * @throws [RuntimeException] if no matching notary is found
     */
    fun getNotaryByOrganisation(organisation: String): Party = findNotaryByOrganisation(organisation)
            ?: throw RuntimeException("No notaries found in network map cache for organisation $organisation")

    /** Filter out my identities from the given parties */
    fun getCounterParties(parties: Iterable<AbstractParty>): List<AbstractParty> {
        return parties.exceptMe()
    }

    /**
     * Splits the original collection into pair of lists, where first list contains our identities
     * and the second list contains those belonging to counter-parties.
     */
    open fun partitionOursAndTheirs(
            parties: Collection<AbstractParty>
    ): Pair<List<AbstractParty>, List<AbstractParty>> {
        val myKeys = serviceHub.keyManagementService.filterMyKeys(parties.map { it.owningKey })
        return parties.partition { myKeys.contains(it.owningKey) }
    }

    /**
     * Returns a set of [Party] matches for the given strings, trying exact and if needed fuzzy matching for each.
     * If not exactly one match is found for any name an error will be thrown.
     *
     * @param names The names to convert to parties
     */
    fun getParties(names: Iterable<String>): Set<Party> {
        val identityService = this.serviceHub.identityService
        return names.map {
            getParty(it, identityService)
        }.toSet()
    }

    /**
     * Returns a [Party] match for the given name string, trying exact and if needed fuzzy matching.
     * If not exactly one match is found an error will be thrown.
     *
     * @param name The name to convert to a party
     */
    fun getParty(name: String, identityService: IdentityService = this.serviceHub.identityService) =
            if(name.contains("O=") && name.contains("C=")) toWellKnownParty(CordaX500Name.parse(name))
            else (identityService.partiesFromName(name, true).singleOrNull()
                    ?: identityService.partiesFromName(name, false).singleOrNull())
                    ?: throw FlowException("Party not found for string '${name}'")

    open fun ourParticipatingKeys(ourParties: List<AbstractParty>): Iterable<PublicKey> {
        return ourParties.map { it.owningKey }
    }

}
