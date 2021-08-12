//
//  AdyenDropInPayment.m
//  ReactNativeAdyenDropin
//
//  Created by 罗立树 on 2019/9/27.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(AdyenDropInPayment, NSObject)

+(BOOL)requiresMainQueueSetup
{
  return YES;
}
//init payment config
RCT_EXTERN_METHOD(configPayment:(NSString)publicKey env:(NSString *) env merchantId:(NSString) merchantId)
/**
  select paymentMethods and pay
 */
RCT_EXTERN_METHOD(paymentMethods:(NSString)paymentMethodsJson summary:(NSString) summaryJson)

//use cardPaymentMethod
RCT_EXTERN_METHOD(cardPaymentMethod:(NSString)paymentMethodsJson name:(NSString *) name showHolderField:(BOOL) showHolderField showStoreField:(BOOL) showStoreField buttonTitle:(NSString *) buttonTitle)
//payment by contract
RCT_EXTERN_METHOD(contractPaymentMethod:(NSString)paymentMethodsJson index:(NSInteger *) index)

//use stored card
RCT_EXTERN_METHOD(storedCardPaymentMethod:(NSString)paymentMethodsJson index:(NSInteger *) index)
RCT_EXTERN_METHOD(handleAction:(NSString)actionJson)
//payment result
RCT_EXTERN_METHOD(handlePaymentResult:(NSString)paymentResultJson)
//.redirect: Use the Redirect Component or handle the redirect on your own.
//RCT_EXTERN_METHOD(handleRedirectAction:(NSString)actionJson)
//.threeDS2Fingerprint: Use the 3D Secure 2 Component to perform 3D Secure 2 device fingerprinting.
//RCT_EXTERN_METHOD(handleThreeDS2FingerprintAction:(NSString)actionJson)
//.threeDS2Challenge: Use 3D Secure 2 Component to present a challenge to your shopper.
//RCT_EXTERN_METHOD(handleThreeDS2ChallengeAction:(NSString)actionJson)
RCT_EXTERN_METHOD(encryptCard:(NSString) cardNumber expiryMonth:(NSInteger)expiryMonth expiryYear:(NSInteger) expiryYear securityCode:(NSString) securityCode resolver:(RCTPromiseResolveBlock)resolve
rejecter:(RCTPromiseRejectBlock)reject)
@end
