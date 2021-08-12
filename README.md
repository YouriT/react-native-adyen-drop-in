# React Native Adyen Drop-In

## Abandonned

**This project is not maintained anymore, if you wish to maintain it, please do not hesitate to get in touch via email or via an issue.**

**React Native Adyen Drop-In** is a cross platform (Android & iOS) plugin enabling Adyen Drop-In integration in a React-Native project.

## Current Adyen versions

* Android: 3.4.0
* iOS: ~>3.1.3

## Disclamer

At the moment the implementation is very opiniated on the use case we encountered. Feel free to submit P.R.

## Getting Started

`$ yarn add react-native-adyen-drop-in`

This module uses autolinking and has not been tested on RN<0.60.

## API

```jsx
import AdyenDropIn from 'react-native-adyen-drop-in'

// configures the public key used for encryption by the native library and sets the environment "test/live" that Adyen should be using.
AdyenDropIn.configPayment(publicKey, env);

// Launch a CardComponent
AdyenDropIn.cardPaymentMethod(
    paymentMethodJson, // accepted cards, use: https://docs.adyen.com/api-explorer/#/PaymentSetupAndVerificationService/paymentMethods result or provide yours.
    name, // form name
    showHolderField, // Display "Holder name" field
    showStoreField, // Display toggle to save card for future payments
    buttonTitle // Content on the call to action ("Pay" button)
)

// Supply Adyen with the available payment methods. Populate it from https://docs.adyen.com/api-explorer/#/PaymentSetupAndVerificationService/paymentMethods or supply custom JSON yourself.
AdyenDropIn.paymentMethods(paymentMethodJson);

// Use a stored payment method
AdyenDropIn.storedCardPaymentMethod(paymentMethodJson, index)

// Set contract payment method
AdyenDropIn.contractPaymentMethod(paymentMethodJson, index)

// Handle further actions (like 3DS etc) asked by Adyen in (action key in /payments response - ie iDEAL, Bancontact)
AdyenDropIn.handleAction(actionJson)

// Notify Adyen Drop In of the payment result.
AdyenDropIn.handlePaymentResult(paymentResult)

// Register a listener on Adyen3DS2Component and RedirectComponent responses
AdyenDropIn.onPaymentProvide((response) => {})
/**
 * response {
 *  isDropIn: boolean,
 *  env: string,
 *  msg: string,
 *  data: {
 *      paymentData,
 *      details,
 *  }
 * }
 */

// Register a listener on payment failures
AdyenDropIn.onPaymentFail((error) => {})
/**
 * error {
 *  isDropIn: boolean,
 *  env: string,
 *  msg: string, // error message
 *  error: string // exception message
 * }
 */

// Register a listener when payment form is submitted
AdyenDropIn.onPaymentSubmit((response) => {})
/**
 * response {
 *  isDropIn: boolean,
 *  env: string,
 *  data: {
 *      paymentMethod: {
 *          type: "scheme",
 *          recurringDetailReference: string
 *      },
 *      storePaymentMethod: true,
 *  }
 * }
 */


```

## License

This repository is open source and available under the MIT license.
