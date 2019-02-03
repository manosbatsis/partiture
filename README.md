# Partiture

Partiture is a compact component framework for your Corda apps.

## Documentation

See https://manosbatsis.github.io/partiture


## What Does It Do?

For the time being, Partiture's primary goal is flow composition. 
The following sections demonstrate simple examples of initiating and 
responding flows.

### Initiating Flow 

Consider a `Yo!` sending flow:

```kotlin
/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class ManualConversionYoFlow(
        input: List<Party>
//  Flow:         IN ,         OUT
) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        input = input, // IN can be anything
        // Build-in OUT converter
        outputConverter = FinalizedTxOutputConverter()
) {
    /** Override to manually init the3 flow's CallContext */
    override fun processInput(): CallContext {
        val entries = input.map {party ->
            // Prepare a TX builder
            val txBuilder = ParticipantsAwareTransactionBuilder(getFirstNotary())
            txBuilder.addOutputState(YoContract.YoState(ourIdentity, party), YO_CONTRACT_ID)
            txBuilder.addCommandFromData(YoContract.Send())
            // Return a call context/TX entry context with builder and participants
            CallContextEntry(txBuilder, txBuilder.participants)
        }
        return CallContext(entries)
    }
}
```

In case of using a custom `InputConverter` instead of overriding `processInput()` as we did above, 
the flow can be reduced to a declaration-only class that binds everything together:

```kotlin
/** Create a Yo! transaction/state for each input recipient/party */
@InitiatingFlow
@StartableByRPC
class YoFlow(
        input: List<Party>
//  Flow:         IN ,         OUT
) : PartitureFlow<List<Party>, List<SignedTransaction>>(
        input = input, // Input can be anything
        inputConverter = YoInputConverter(),// Our custom IN converter
        outputConverter = FinalizedTxOutputConverter()) // OUT build-in converter
```

Both of the above flow implementations will:
 
 - Use the overriden `processInput()` or provided `inputConverter` respectively to initialize a `CallContext` with a call/tx entry per input `Party`
 - Use the build-in _default_ `TxStrategy` (since we have not provided one) on each of `CallContext.entries` to:
	- Sign an initial transaction
	- Create flow sessions for counter-parties, if any exist
	- Perform an identity sync if any own anonymous parties are participating in the input/output states in context
	- Gather and verify counter-party signatures
	- Verify the original transaction builder
	- Finalize the transaction
- Use the provided `outputConverter` to produce and return the flow's `call()` result, i.e. a signed TX per _Yo!_ state created


### Responding Flow 

This is our responder flow. It uses the biuld-in  `SimpleTypeCheckingResponderTxStrategy`:

```kotlin
@InitiatedBy(YoFlow::class)
class YoFlowResponder(
        otherPartySession: FlowSession
) : PartitureResponderFlow(
        otherPartySession = otherPartySession,
        responderTxStrategy = SimpleTypeCheckingResponderTxStrategy(
                YoContract.YoState::class.java)
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

 
 