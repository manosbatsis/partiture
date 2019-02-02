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
package com.github.manosbatsis.partiture.flow

import co.paralleluniverse.fibers.Suspendable
import com.github.manosbatsis.partiture.flow.delegate.FlowConverterDelegate
import com.github.manosbatsis.partiture.flow.io.InputConverter
import com.github.manosbatsis.partiture.flow.io.OutputConverter
import com.github.manosbatsis.partiture.flow.call.SimpleCallStrategy
import com.github.manosbatsis.partiture.flow.call.TxContext
import com.github.manosbatsis.partiture.flow.call.TxStrategy
import com.github.manosbatsis.partiture.flow.util.PartitureUtilsFlowLogic
import com.github.manosbatsis.partiture.flow.util.ProgressTrackerUtil
import net.corda.core.flows.FlowLogic

/**
 * Base [FlowLogic] implementation utilizing [TxContext] and [TxStrategy]
 */
abstract class PartitureFlow<IN, OUT>(
        val input: IN,
        val txStrategy: TxStrategy = SimpleCallStrategy(),
        val inputConverter: InputConverter<IN>? = null,
        val outputConverter: OutputConverter<OUT>? = null
) : PartitureUtilsFlowLogic<OUT>() {

    /** Obtain the appropriate progressTracker from the given [TxStrategy] */
    override val progressTracker = txStrategy.progressTracker

    /**
     * Prepare a context with items relevant to flow transaction(s):
     * Notary, input/output/reference states, attachments, commands and so on.
     *
     * The default implementation requires a non-null [InputConverter] parameter
     * via the flow's constructor ; either provide one or override the method to convert manually.
     */
    @Suspendable
    open fun convertInput(): TxContext {
        return convert(inputConverter, input, "Input")
    }

    /**
     * Convert the given signed transactions to the desired type for flow `out`.
     *
     * The default implementation requires a non-null [OutputConverter] parameter
     * via the flow's constructor ; either provide one or override the method to convert manually.
     */
    @Suspendable
    fun convertOutput(txContext: TxContext): OUT {
        return convert(outputConverter, txContext, "Output")
    }

    @Suspendable
    final override fun call(): OUT {
        progressTracker.currentStep = ProgressTrackerUtil.Companion.INITIALISE
        // Delegate processing to txStrategy
        txStrategy.clientFlow = this
        val txResults = txStrategy.execute(convertInput())
        // Process and return output
        progressTracker.currentStep = ProgressTrackerUtil.Companion.PROCESS_OUTPUT
        return this.convertOutput(txResults)
    }

    private fun <I, O> convert(converter: FlowConverterDelegate<I, O>?, input: I, sourceName: String): O {
        return if (converter != null) {
            converter.clientFlow = this
            converter.convert(input)
        } else throw NotImplementedError("This convert${sourceName}() implementation requires an ${sourceName}ConverterDelegate. " +
                "Either provide one in the flow's constructor or override the method to convert manually.")
    }
}