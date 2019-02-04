---
title: Input Converters
---

Partiture attempts to decouple input and TX processing in flows. For that purpose, 
`PartitureFlow.processInput()` attempts to use the `inputConverter` passed via 
it's constructor to initialize the `CallContext`. If `inputConverter` is `null`, 
an error will be thrown.

You may find it convenient to override `processInput()`, e.g. for the first version 
of the flow in the introduction page:

```kotlin
/** Override to manually init the flow's CallContext */
override fun processInput(): CallContext {
	val entries = input.map { party ->
		// Prepare a TX builder
		val txBuilder = ParticipantsAwareTransactionBuilder(getFirstNotary())
		txBuilder.addOutputState(YoContract.YoState(ourIdentity, party), YO_CONTRACT_ID)
		txBuilder.addCommandFromData(YoContract.Send())
		// Return a call context/TX entry context with builder and participants
		CallContextEntry(txBuilder, txBuilder.participants)
	}
	return CallContext(entries)
}
```

however, the recommended way is implementing an `InputConverter` instead. 
In the case of `YoFlow`, the `PartitureFlow`'s `IN` generic argument is `List<Party>` 
so we need to implement an `InputConverter<List<Party>>`:

```kotlin
class YoInputConverter : PartitureFlowDelegateBase(), InputConverter<List<Party>> {
    override fun convert(input: List<Party>): CallContext {
        val entries = input.map { party ->
            // Prepare a TX builder
            val txBuilder = ParticipantsAwareTransactionBuilder(clientFlow.getFirstNotary())
            txBuilder.addOutputState(YoContract.YoState(clientFlow.ourIdentity, party), YO_CONTRACT_ID)
            txBuilder.addCommandFromData(YoContract.Send())
            // Return a TX context with builder and participants
            CallContextEntry(txBuilder, txBuilder.participants)
        }
        return CallContext(entries)
    }
}
``` 

the converter can then be reused between different flows by passing an instance as the 
`inputConverter` in `PartitureFlow`'s constructor when extending it. See the second flow 
version in the introduction for an example.