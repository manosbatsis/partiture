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
 *     AccountYou should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package mypackage.cordapp.contract

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


// Contract and state.
val YO_CONTRACT_PACKAGE = AccountYoContract::class.java.`package`.name
val YO_CONTRACT_ID = AccountYoContract::class.java.canonicalName

class AccountYoContract : Contract {

    // Command.
    class Send : TypeOnlyCommandData()

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Send>()
        "There can be no inputs when AccountYo'ing other parties." using (tx.inputs.isEmpty())
        //"There must be one output: The AccountYo!" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<AccountYoState>().first()
        "No sending AccountYo's to yourself!" using (yo.recepient != yo.sender)
        "The AccountYo! must be signed by the sender." using (command.signers.contains(yo.sender.owningKey))
        //"The AccountYo! must be signed by the recipient." using (command.signers.contains(yo.recepient.owningKey))
    }

    // State.
    @BelongsToContract(AccountYoContract::class)
    data class AccountYoState(val sender: AccountParty,
                       val recepient: Party,
                       val yo: String = "AccountYo!") : ContractState, QueryableState {
        override val participants get() = listOf(sender, recepient)
        //override fun toString() = "${sender.name}: $yo"
        override fun supportedSchemas() = listOf(AccountYoSchemaV1)

        override fun generateMappedObject(schema: MappedSchema) = AccountYoSchemaV1.PersistentAccountYoState(
                sender.name.toString(), recepient.name.toString(), yo)

        object AccountYoSchema

        object AccountYoSchemaV1 : MappedSchema(AccountYoSchema.javaClass, 1, listOf(PersistentAccountYoState::class.java)) {
            @Entity
            @Table(name = "yos")
            class PersistentAccountYoState(
                    @Column(name = "sender")
                    var origin: String = "",
                    @Column(name = "recepient")
                    var target: String = "",
                    @Column(name = "yo")
                    var yo: String = ""
            ) : PersistentState()
        }
    }
}


