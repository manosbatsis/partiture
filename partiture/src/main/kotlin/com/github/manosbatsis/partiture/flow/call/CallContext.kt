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
package com.github.manosbatsis.partiture.flow.call

import net.corda.core.flows.FlowSession

/**
 * Contains information for each (finalized) transaction relevant to the current FlowLogic call.
 */
data class CallContext(
        /**
         * The entries of this context, each with the info necessary
         * to create a (finalized) transaction.
         */
        val entries: MutableList<CallContextEntry> = mutableListOf(),
        /**
         * Counter-party flow sessions, set during EXECUTE_TRANSACTIONS step by
         * [com.github.manosbatsis.partiture.flow.PartitureFlow.createFlowSessions]
         */
        var sessions: Set<FlowSession> = emptySet(),
        /**  Additional (top-level) metadata */
        override var meta: MutableMap<String, Any>? = null
) : CallMetadata() {

    constructor(entry: CallContextEntry) : this() {
        this.entries.add(entry)
    }

    constructor(entries: Iterable<CallContextEntry>) : this() {
        this.entries.addAll(entries)
    }
}
