# React Native Adyen Drop-in

**React Native Adyen Drop-in** is a cross platform (Android & iOS) plugin enabling Adyen Drop-in integration in a React Native project.

## Disclamer

At the moment the implementation is very opiniated on the use case we encountered. Feel free to submit P.R.

## Getting Started

`$ yarn add react-native-adyen-drop-in`

This module uses autolinking and has not been tested on RN<0.60.

## API

```jsx
import AdyenDropIn from 'react-native-adyen-drop-in';

const config = {
    environment: 'test', // test / liveEurope / liveUnitedStates / liveAustralia
    clientKey: 'YOUR_CLIENT_KEY',
    shopperLocale: 'en_US',
    amount: {currency: 'EUR', value: 2500}, // 25€
    countryCode: 'GB', // The code of the country in which the payment is made (in ISO-3166-1 alpha-2 format)
    returnUrl: 'your_custom_scheme://custom/url', // only for iOS
    skipListWhenSinglePaymentMethod: true, // android only
    card: { // optional configuration for card payment
        showsHolderNameField: true,
        showsStorePaymentMethodField: true,
        shopperReference: 'unique_shopper_id',
    },
    applePay: {
        merchantIdentifier: 'your.apple.pay.merchant.id',
        summaryLabel: 'Total',
    },
};

// Open the Drop-in component by providing a config and the available
// payment methods
AdyenDropIn.startPayment(paymentMethodJson, config);

// When the payment form is submitted the Drop-in component sends a
// `payment` event containing the data that needs to be sent in the
// `/payments` request
AdyenDropIn.addListener('payment', data => {
    // data format:
    // {
    //    paymentMethod: { ... },
    //    storePaymentMethod: true / false,
    //    returnUrl: '...', // on android the Drop-in provides its custom url
    // }

    // make the `/payments` request and hand back the response:
    AdyenDropIn.handleResponse(response);
});

// When some additional payment action is completed a `payment-details`
// event is sent with the data that needs to be sent in the
// `/payments/details` request
AdyenDropIn.addListener('payment-details', data => {
    // data format:
    // {
    //    paymentData: ...,
    //    details: { ... },
    // }

    // make the `/payments/details` request and hand back the response:
    AdyenDropIn.handleResponse(response);
});

// The `result` event is sent when the payment is over
AdyenDropIn.addListener('result', data => {
    // `data.type` is one of:
    //  * 'finished' (check `data.result` for the result code)
    //  * 'error' (details in `data.message`) 
    //  * 'canceled-by-user'
});

```

## License

This repository is open source and available under the MIT license.