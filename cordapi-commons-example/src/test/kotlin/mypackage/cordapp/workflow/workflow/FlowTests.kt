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
package mypackage.cordapp.workflow

import mypackage.cordapp.contract.YO_CONTRACT_PACKAGE
import mypackage.cordapp.contract.YoContract
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp.Companion.findCordapp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals


@Suppress("DEPRECATION")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class YoFlowTests {

    // Works as long as the main and test package names are  in sync
    val cordappPackages = listOf(
            YO_CONTRACT_PACKAGE,
            this.javaClass.`package`.name)
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @BeforeAll
    fun setup() {

        network = MockNetwork(MockNetworkParameters(
                cordappsForAllNodes = cordappPackages.map {
                    findCordapp(it)
                }))

        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @AfterAll
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun flowWorksCorrectly() {
        val yo = YoContract.YoState(a.info.legalIdentities.first(), b.info.legalIdentities.first())
        val flow = YoFlow(b.info.legalIdentities.first())
        val future = a.startFlow(flow)
        network.runNetwork()
        val stx = future.getOrThrow()
        // Check yo transaction is stored in the storage service.
        val bTx = b.services.validatedTransactions.getTransaction(stx.id)
        assertEquals(bTx, stx)
        print("bTx == $stx\n")
        // Check yo state is stored in the vault.
        b.transaction {
            // Simple query.
            val bYo = b.services.vaultService.queryBy<YoContract.YoState>().states.single().state.data
            assertEquals(bYo.toString(), yo.toString())
            print("$bYo == $yo\n")
            // Using a custom criteria directly referencing schema entity attribute.
            val expression = builder { YoContract.YoState.YoSchemaV1.PersistentYoState::yo.equal("Yo!") }
            val customQuery = VaultCustomQueryCriteria(expression)
            val bYo2 = b.services.vaultService.queryBy<YoContract.YoState>(customQuery).states.single().state.data
            assertEquals(bYo2.yo, yo.yo)
            print("$bYo2 == $yo\n")
        }
    }
}
