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
package com.github.manosbatsis.partiture.adapter.tokensdk10.functions

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.services.IdentityService

/**
 *  Pretty much a coy of Corda's internal functions
 *  to work around not having TX builder interface,
 *  so this is mostly R3's Copyright under Apache License Version 2.0
 *  see qlso https://github.com/corda/token-sdk/blob/master/LICENCE
 */

// Check that all states share the same notary.
@Suspendable
internal fun checkSameNotary(stateAndRefs: List<StateAndRef<AbstractToken>>) {
    val notary = stateAndRefs.first().state.notary
    check(stateAndRefs.all { it.state.notary == notary }) {
        "All states should have the same notary. Automatic notary change isn't supported for now."
    }
}

// Checks if all states have the same issuer. If the issuer is provided as a parameter then it checks if all states
// were issued by this issuer.
@Suspendable
internal fun checkSameIssuer(
        stateAndRefs: List<StateAndRef<AbstractToken>>,
        issuer: Party? = null
) {
    val issuerToCheck = issuer ?: stateAndRefs.first().state.data.issuer
    check(stateAndRefs.all { it.state.data.issuer == issuerToCheck }) {
        "Tokens with different issuers."
    }
}

// Check if owner of the states is well known. Check if states come from the same owner.
// Should be called after synchronising identities step.
@Suspendable
internal fun checkOwner(
        identityService: IdentityService,
        stateAndRefs: List<StateAndRef<AbstractToken>>,
        counterparty: Party
) {
    val owners = stateAndRefs.map { identityService.wellKnownPartyFromAnonymous(it.state.data.holder) }
    check(owners.all { it != null }) {
        "Received states with owner that we don't know about."
    }
    check(owners.all { it == counterparty }) {
        "Received states that don't come from counterparty that initiated the flow."
    }
}
