---
title: TX Strategies
---

Partiture flows delegate transaction processing to instances of `TxStrategy`.
This reduces error-prone code duplication, and makes flows easier to implement
and maintain, especially for less experienced developers that now reuse 
strategies provided to them.

Implementations of `TxStrategy` also provide a `ProgressTracker` to their client flow.


## Applying a Strategy

```kotlin
class MyFlow(
	input: Whatever
) : PartitureFlow<Whatever, Whatnot>(
	input: input,
	// Apply the super duper strategy!
	txStrategy = MySuperDuperTxStrategy()
	// add converters etc.
)
```

## Default Strategy

`SimpleTxStrategy` is the default/fallback implementation i.e. value of 
`PartitureFlow`'s `txStrategy` constructor parameter. It inspects the 
`CallContext.entries` of the client flow and performs the following 
for each `CallContextEntry`:

1. Sign an initial transaction
2. Create flow sessions for counter-parties, if any exist
3. Perform an identity sync if any own anonymous parties are participating in the input/output states in context
4. Gather and verify counter-party signatures
5. Verify the original transaction builder
6. Finalize the transaction 

## Custom Strategies

While `SimpleTxStrategy` may be useful for some common cases, you will probably need 
to implement other strategies that better match your application requirements. Here's 
an example/template:

```kotlin
class MyCustomTxStrategy : PartitureFlowDelegateBase(), TxStrategy {

    /** 
    * Provides an instance pre-configured with the default progress steps.
    */
    override val progressTracker = MyCustomLifecycle.progressTracker()

	/** This is actually the default implementation already provided by TxStrategy */
	@Suspendable
	override fun execute() {
		// Iterate entries
		clientFlow.callContext.entries.forEach { executeFor(it) }
	}

	/** Process a CallContextEntry */
    @Suspendable
    override fun executeFor(ccEntry: CallContextEntry) {
    	// Update the currentStep of the client flow's progress tracker  
        var currentStep = step(SimpleInitiatingLifecycle.SIGN_INITIAL_TX)
        // Perform initial transaction signature
        ccEntry.initial = clientFlow
                .signInitialTransaction(ccEntry.transactionBuilder)
        // Do some other stuff...
    }
}
```

### Progress Tracking

`TxStrategy` implementations also provide the appropriate `ProgressTracker` to 
the calling flow, as seen in the `MyCustomTxStrategy` above. 
The provided `ProgressTracker` steps __must__ start with:

- `SimpleInitiatingLifecycle.INITIALIZE`
- `SimpleInitiatingLifecycle.PROCESS_INPUT`
- `SimpleInitiatingLifecycle.POST_PROCESS_INPUT`
- `SimpleInitiatingLifecycle.EXECUTE_TRANSACTIONS`

and __must__ end with:

- `SimpleInitiatingLifecycle.POST_EXECUTE_TRANSACTIONS`
- `SimpleInitiatingLifecycle.PROCESS_OUTPUT`

as those steps are always used by the calling flow. 
Any in-between steps depend solely on your implementation.

> Recommended: implement a reusable 
`com.github.manosbatsis.partiture.flow.lifecycle.Lifecycle` `object` 
that can then be shared between similar strategies for obtaining a 
properly configured progress tracker. 
See `SimpleInitiatingLifecycle` for an example.

### Updating the Context

`TxStrategy` implementations are expected to update members of the client flow's 
`CallContext` and/or of it's `CallContextEntry` entries (mostly resulting 
`SignedTransactions`), thus providing all necessary information as appropriate 
for an `OutputConverter` may require to produce the appropriate output. 

For more info see the next section: _Output Converters_.




