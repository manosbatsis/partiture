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
package mypackage.cordapp.workflow

import com.github.manosbatsis.partiture.flow.PartitureFlow
import mypackage.cordapp.contract.YO_CONTRACT_PACKAGE
import mypackage.cordapp.contract.YoContract
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
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
    val cordappPackages = listOf(YO_CONTRACT_PACKAGE, this.javaClass.`package`.name, "com.github.manosbatsis.partiture.flow")
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @BeforeAll
    fun setup() {

        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, true)),
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
    fun `Test flow with  SingleFinalizedTxOutputConverter`() {
        val msg = "Yo1"
        val yo = YoContract.YoState(a.info.legalIdentities.first(),
                b.info.legalIdentities.first(),
                yo = msg)

        val stx = flowWorksCorrectly(
                YoFlow1(YoMessage(b.info.legalIdentities.first(), msg)))
        // Check yo transaction is stored in the storage service.
        val bTx = b.services.validatedTransactions.getTransaction(stx.id)
        assertEquals(bTx, stx)
        print("bTx == $stx\n")
        // Check yo state is stored in the vault.
        b.transaction {
            // Simple query.
            val bYo = b.services.vaultService.queryBy<YoContract.YoState>()
                    .states.map { it.state.data }.single { it.yo == msg }
            assertEquals(bYo.toString(), yo.toString())
            print("$bYo == $yo\n")
            // Using a custom criteria directly referencing schema entity attribute.
            val expression =
                    builder { YoContract.YoState.YoSchemaV1.PersistentYoState::yo.equal(msg) }
            val customQuery = VaultCustomQueryCriteria(expression)
            val bYo2 = b.services.vaultService.queryBy<YoContract.YoState>(customQuery).states.single().state.data
            assertEquals(bYo2.yo, yo.yo)
            print("$bYo2 == $yo\n")
        }
    }

    @Test
    fun `Test flow with testTypedOutputStatesConverter`() {
        val msg = "Yo2"
        val yo = YoContract.YoState(
                a.info.legalIdentities.first(), b.info.legalIdentities.first(),msg)

        val result = flowWorksCorrectly(
                YoFlow2(YoMessage(b.info.legalIdentities.first(), msg)))

        assertEquals(yo.toString(), result.single().toString())
    }

    @Test
    fun `Test flow with multiple participant instances of the same Corda Account`() {
        val msg = "Yo3"
        val yos = listOf(
                YoContract.YoState(a.info.legalIdentities.first(), b.info.legalIdentities.first(),"${msg}a"),
                YoContract.YoState(a.info.legalIdentities.first(), b.info.legalIdentities.first(),"${msg}b"),
                YoContract.YoState(b.info.legalIdentities.first(), a.info.legalIdentities.first(),"${msg}c"),
                YoContract.YoState(b.info.legalIdentities.first(), a.info.legalIdentities.first(),"${msg}d")
        )

        val result = flowWorksCorrectly(YoFlow3(yos))

        assertEquals(4, result.filter{it.yo.startsWith(msg)}.size)
    }

    inline fun <reified OUT> flowWorksCorrectly(flow: PartitureFlow<*, OUT>): OUT {
        val future = a.startFlow(flow)
        // Ask nodes to process any queued up inbound messages
        network.runNetwork()
        return  future.getOrThrow()

    }
}
