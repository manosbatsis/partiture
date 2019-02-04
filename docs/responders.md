---
title: Responders
---

Implementations of `ResponderTxStrategy` are used by `PartitureResponderFlow`s 
to create a `SignTransactionFlow` with the appropriate verification checks.

> Responder APIs are highly unstable. They may be merged to the main flow/TX 
strategy equivalents, or otherwise change in future versions.

`PartitureResponderFlow`s require a `FlowSession` given as `otherPartySession` 
and a `ResponderTxStrategy` given as `responderTxStrategy`.
 
Consider the `YoFlowResponder` from our [Introduction](introduction):

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

## Build-in Responder Strategies

Build-in converters reside in the `com.github.manosbatsis.partiture.flow.io.output` package.

| Class			                   	   	| Description                                                                                       	                                        |
|-------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `SimpleResponderTxStrategy` 	    	| Simple responder flow delegate that verifies the (ledger) transaction before signing. 														|
| `SingleFinalizedTxOutputConverter`	| Simple responder flow delegate that verifies the (ledger) transaction and ensures all output states are of a certain type(s) before signing.	|


## Custom Responder Strategies

Creating responder TX strategies is a critical part of your application. Here's a 
template to get you started:

```kotlin
class FooResponderTxStrategy : PartitureResponderFlowDelegateBase(), ResponderTxStrategy {

    /** Create a [SignTransactionFlow] with the appropriate verification checks. */
    @Suspendable
    override fun createSignTransactionFlow(): SignTransactionFlow {
        // Create our custom SignTransactionFlow
        return object : SignTransactionFlow(
                clientFlow.otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Do stuff to ensure the given signed transaction  
                // actually satisfies your requirements:
                return requireThat {itsAllGood()}
            }
        }
    }
}
```

