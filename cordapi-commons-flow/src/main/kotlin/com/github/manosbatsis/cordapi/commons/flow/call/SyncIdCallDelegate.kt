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
package com.github.manosbatsis.cordapi.commons.flow.call

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.cordapi.commons.flow.base.BaseFlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.WireTransaction

/**
 * Simple call delegate implementation that also
 * performs an identity sync after obtaining counter-party sessions
 */
class SyncIdCallDelegate : SimpleCallDelegate() {

    @Suspendable
    fun <T : BaseFlowLogic<*>> performIdentitySync(
            client: T, sessions: List<FlowSession>, tx: WireTransaction
    ) {
        client.performIdentitySync(sessions, tx)
    }

}