---
id: introduction
title: Introduction
---

Partiture is a compact component framework for your Corda apps.
For the time being, it's primary goal is flow composition. 

## Installation

```groovy
cordaCompile "com.github.manosbatsis.partiture:partiture:$partiture_version"
```

## Example Flows

The following sections demonstrate simple examples of initiating and 
responding flows.

### Sample Initiating Flow 

Start with a `Yo!` sending flow, that uses `List<Party>` and `List<SignedTransaction>` 
for `PartitureFlow`'s `IN` and `OUT` type variables respectively:

```kotlin
/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class YoFlow(input: List<Party>) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        // Can be any type, just match with PartitureFlow's IN generic argument above
        input = input 
) {
    /** Override to manually init the flow's CallContext */
    override fun processInput(): CallContext {
        // do stuff...
    }
    /** Override to manually create the flow's OUT instance */
    override fun processOutput(): List<SignedTransaction> {
        // do stuff...
    }
}
```

Better yet, use an `InputConverter` and `OutputConverter` instead of 
overriding `processInput()` and `processOutput()` as we did above, 
thus reducing the flow to a declaration-only class that binds everything together:

```kotlin
/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class YoFlow(input: List<Party>) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        // Can be any type, just match with PartitureFlow's IN generic argument above
        input = input, 
        inputConverter = YoInputConverter(),// Our custom IN converter
        outputConverter = FinalizedTxOutputConverter()) // build-in converter matching OUT
// No implementation needed!   
```

Both of the above flow implementations will:
 
 1. Use the overriden `processInput()` or provided `inputConverter` respectively to initialize a `CallContext` with a call/tx entry per input `Party`
 2. Use the build-in _default_ `TxStrategy` (since we have not provided one) on each of `CallContext.entries` to:
 	1. Sign an initial transaction
 	2. Create flow sessions for counter-parties, if any exist
 	3. Perform an identity sync if any own anonymous parties are participating in the input/output states in context
	4. Gather and verify counter-party signatures
	5. Verify the original transaction builder
	6. Finalize the transaction
3. Use the provided `outputConverter` to produce and return the flow's `call()` result, i.e. a signed TX per _Yo!_ state created


### Sample Responding Flow 

This is our responder flow. It uses the biuld-in  `SimpleTypeCheckingResponderTxStrategy`:

```kotlin
@InitiatedBy(YoFlow::class)
class YoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = 
        	SimpleTypeCheckingResponderTxStrategy(YoContract.YoState::class.java)
)
```

The above responder flow will verify the transaction 
and ensure all output states are instances of `YoState` before signing.

