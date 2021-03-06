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
package com.github.manosbatsis.partiture.flow.io.output

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.call.CallContext
import com.github.manosbatsis.partiture.flow.delegate.initiating.PartitureFlowDelegateBase
import net.corda.core.contracts.ContractState
import java.util.function.Predicate

/**
 * Produce the output by gathering output states,
 * optionally applying an additional filter
 */
open class OutputStatesConverter(
        val filter: Predicate<ContractState>? = null
) : PartitureFlowDelegateBase(), OutputConverter<List<ContractState>> {

    @Suspendable
    override  fun convert(input: CallContext): List<ContractState> {
        // Gather the output states
        val outputStates = input.entries.mapNotNull { it.finalized }
                .flatMap { it.tx.outputStates }
        // Return as-is or use filter if available
        return if(filter == null) outputStates
        else {
            outputStates.filter { filter.test(it) }
        }
    }
}

/**
 * Output by gathering output states of the given
 * type, optionally applying an additional filter
 */
open class TypedOutputStatesConverter<T: ContractState>(
        val contractStateType: Class<T>,
        val filter: Predicate<ContractState>? = null
) : PartitureFlowDelegateBase(), OutputConverter<List<T>> {

    @Suspendable
    override  fun convert(input: CallContext): List<T> {
        // Gather the output states that match the target type
        val outputStates = input.entries.mapNotNull { it.finalized }
                .flatMap { it.tx.outRefsOfType(contractStateType) }
                .map { it.state.data }
        // Return as-is or use filter if available
        return if(filter == null) outputStates
        else {
            outputStates.filter { filter.test(it) }
        }
    }
}

/**
 * Output the single output state of the given type.
 */
open class TypedOutputSingleStateConverter<T: ContractState>(
        val contractStateType: Class<T>
) : PartitureFlowDelegateBase(), OutputConverter<T> {

    @Suspendable
    override  fun convert(input: CallContext): T =
            TypedOutputStatesConverter(contractStateType)
                    .convert(input)
                    .single()

}