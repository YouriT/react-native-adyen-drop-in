#import <Foundation/Foundation.h>
#import "React/RCTBridgeModule.h"

@interface RCT_EXTERN_MODULE(AdyenDropInPayment, NSObject)

RCT_EXTERN_METHOD(startPayment:(NSDictionary *)paymentMethodsObj configObj:(NSDictionary *)config)
RCT_EXTERN_METHOD(handleResponse:(NSDictionary *)responseObj)
RCT_EXTERN_METHOD(handleReturnUrl:(NSString)url)

@end
