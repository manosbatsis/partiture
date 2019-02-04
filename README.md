# Partiture ![version](https://img.shields.io/badge/version-0.1-blue.svg?maxAge=2592000)  [![Build Status](https://travis-ci.org/manosbatsis/partiture.svg?branch=master)](https://travis-ci.org/manosbatsis/partiture)

Partiture is a compact component framework for your Corda apps.

## Documentation

See https://manosbatsis.github.io/partiture


## What Does It Do?

For the time being, Partiture's primary goal is flow composition. 
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

## Partiture Components

Partiture provides a number of build-in components: 
input/output converters, TX strategies, responder strategies, flow utilities 
and so on. 

However, it's main feature is the conventions and ease with which you can develop and 
reuse components specifically for your application requirements - see the 
[documentation](https://manosbatsis.github.io/partiture) for more details.

 
 