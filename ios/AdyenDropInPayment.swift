//
//  AdyenDropInPayment.swift
//  ReactNativeAdyenDropin
//
//  Created by 罗立树 on 2019/9/27.
//  Copyright © 2019 Facebook. All rights reserved.
//

import Adyen
import Foundation
import SafariServices
import PassKit

@objc(AdyenDropInPayment)
class AdyenDropInPayment: RCTEventEmitter {
  func dispatch(_ closure: @escaping () -> Void) {
    if Thread.isMainThread {
      closure()
    } else {
      DispatchQueue.main.async(execute: closure)
    }
  }

  func requiresMainQueueSetup() -> Bool {
    return true
  }
  var customCardComponent:CustomCardComponent?
  var dropInComponent: DropInComponent?
  var cardComponent: CardComponent?
  var threeDS2Component: ThreeDS2Component?
  var publicKey: String?
  var env: Environment?
  var isDropIn:Bool?
  var envName: String?
  var configuration: DropInComponent.PaymentMethodsConfiguration?
  override func supportedEvents() -> [String]! {
    return [
      "onPaymentFail",
      "onPaymentProvide",
      "onPaymentSubmit",
    ]
  }
}

extension AdyenDropInPayment: DropInComponentDelegate {
  @objc func configPayment(_ publicKey: String, env: String, merchantId: String) {
    configuration = DropInComponent.PaymentMethodsConfiguration()
    configuration?.card.publicKey = publicKey
    configuration?.card.showsHolderNameField = true
    self.publicKey = publicKey
    configuration?.card.showsStorePaymentMethodField = true

    configuration?.applePay.merchantIdentifier = merchantId

    envName = env
    switch env {
    case "live":
      self.env = .live
    default:
      self.env = .test
    }
  }
   @objc func encryptCard(_ cardNumber: String,expiryMonth:Int, expiryYear:Int,securityCode:String,resolver resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock)  {
       let card = CardEncryptor.Card(number: cardNumber,
                                     securityCode: securityCode,
                                     expiryMonth:  String(expiryMonth),
                                     expiryYear: "20" + String(expiryYear))
     let encryptedCard = CardEncryptor.encryptedCard(for: card, publicKey: self.publicKey!)

     let resultMap:Dictionary? = [
       "encryptedNumber":encryptedCard.number,
       "encryptedExpiryMonth":encryptedCard.expiryMonth,
       "encryptedExpiryYear":encryptedCard.expiryYear,
       "encryptedSecurityCode":encryptedCard.securityCode,
     ]
     resolve(resultMap)
   }

    @objc func paymentMethods(_ paymentMethodsJson: String, summary summaryJson: String) {
    self.isDropIn = true
    let jsonData: Data? = paymentMethodsJson.data(using: String.Encoding.utf8) ?? Data()
    let jsonSummaryData: Data? = summaryJson.data(using: String.Encoding.utf8) ?? Data()

    let paymentMethods: PaymentMethods? = try? JSONDecoder().decode(PaymentMethods.self, from: jsonData!)
    let summary: Summary? = try? JSONDecoder().decode(Summary.self, from: jsonSummaryData!);

    let dropInComponent = DropInComponent(paymentMethods: paymentMethods!,
                                          paymentMethodsConfiguration: configuration!)

    let payment = Payment(amount: Payment.Amount(value: NSDecimalNumber(decimal: summary!.total * 100).intValue, currencyCode: (summary?.currencyCode)!), countryCode: summary?.countryCode);
    dropInComponent.payment = payment;

    configuration?.applePay.summaryItems = [
        PKPaymentSummaryItem(label: summary!.title, amount: NSDecimalNumber(decimal: summary!.total), type: .final)
    ];

    self.dropInComponent = dropInComponent
    dropInComponent.delegate = self
    dropInComponent.environment = self.env!

    dispatch {
      UIApplication.shared.delegate?.window??.rootViewController!.present(dropInComponent.viewController, animated: true)
    }
  }

  func didSubmit(_ data: PaymentComponentData, from component: DropInComponent) {
    if(!self.isDropIn! || ((data.paymentMethod as? ApplePayDetails)?.type == "applepay")) {
        component.viewController.dismiss(animated: true)
    }
    var paymentMethodMap: Dictionary? = data.paymentMethod.dictionaryRepresentation
    paymentMethodMap!["recurringDetailReference"] = paymentMethodMap!["storedPaymentMethodId"]
    let resultData = ["paymentMethod": paymentMethodMap, "storePaymentMethod": data.storePaymentMethod] as [String: Any]

    sendEvent(
      withName: "onPaymentSubmit",
      body: [
        "isDropIn": self.isDropIn,
        "env": self.envName,
        "data": resultData,
      ]
    )
  }

  /// Invoked when additional details have been provided for a payment method.
  ///
  /// - Parameters:
  ///   - data: The additional data supplied by the drop in component..
  ///   - component: The drop in component from which the additional details were provided.
  func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
    component.viewController.dismiss(animated: true)
    let resultData = ["details": data.details.dictionaryRepresentation, "paymentData": data.paymentData] as [String: Any]
    sendEvent(
      withName: "onPaymentProvide",
      body: [
        "isDropIn": self.isDropIn,
        "env": self.envName,
        "data": resultData,
      ]
    )
  }

  /// Invoked when the drop in component failed with an error.
  ///
  /// - Parameters:
  ///   - error: The error that occurred.
  ///   - component: The drop in component that failed.
  func didFail(with error: Error, from component: DropInComponent) {
    component.viewController.dismiss(animated: true)
    sendEvent(
      withName: "onPaymentFail",
      body: [
        "isDropIn": self.isDropIn,
        "env": self.envName,
        "msg": error.localizedDescription,
        "error": String(describing: error),
      ]
    )
  }
}

extension AdyenDropInPayment: PaymentComponentDelegate {
  func getStoredCardPaymentMethod(_ paymentMethods: PaymentMethods, index: Int) -> StoredCardPaymentMethod {
    var paymentMethod: StoredCardPaymentMethod?
    if paymentMethods.stored.count == 1 {
      return paymentMethods.stored[0] as! StoredCardPaymentMethod
    }
    if paymentMethods.stored.count > 1 {
      paymentMethod = paymentMethods.stored[index] as! StoredCardPaymentMethod
    }
    return paymentMethod!
  }

  func getCardPaymentMethodByName(_ paymentMethods: PaymentMethods, name _: String) -> CardPaymentMethod {
    var paymentMethod: CardPaymentMethod?
    if paymentMethods.regular.count == 1 {
      return paymentMethods.regular[0] as! CardPaymentMethod
    }
    if paymentMethods.regular.count > 1 {
      for p in paymentMethods.regular {
        if p.name == "Credit Card" {
          paymentMethod = (p as! CardPaymentMethod)
          break
        }
      }
    }
    return paymentMethod!
  }

  @objc func storedCardPaymentMethod(_ paymentMethodsJson: String, index: Int) {
    self.isDropIn = false
    self.threeDS2Component = nil
    self.cardComponent?.viewController.dismiss(animated: true)
    let jsonData: Data? = paymentMethodsJson.data(using: String.Encoding.utf8) ?? Data()
    let paymentMethods: PaymentMethods? = try? JSONDecoder().decode(PaymentMethods.self, from: jsonData!)
    let cardPaymentMethod: StoredCardPaymentMethod? = self.getStoredCardPaymentMethod(paymentMethods!, index: index)
    let cardComponent = CardComponent(paymentMethod: cardPaymentMethod!,
                                      publicKey: self.publicKey!)
    self.cardComponent = cardComponent
    // Replace CardComponent with the payment method Component that you want to add.
    // Check specific payment method pages to confirm if you need to configure additional required parameters.
    // For example, to enable the Card form, you need to provide your Client Encryption Public Key.
    cardComponent.delegate = self
    cardComponent.environment = env!
    // When you're ready to go live, change this to .live
    // or to other environment values described in https://adyen.github.io/adyen-ios/Docs/Structs/Environment.html
    dispatch { UIApplication.shared.delegate?.window??.rootViewController!.present(cardComponent.viewController, animated: true)
    }
  }
  @objc func contractPaymentMethod(_ paymentMethodsJson: String,index: Int) {
    self.isDropIn = false
    self.threeDS2Component = nil
    let jsonData: Data? = paymentMethodsJson.data(using: String.Encoding.utf8) ?? Data()
    let paymentMethods:PaymentMethods? = try? JSONDecoder().decode(PaymentMethods.self, from: jsonData!)
    let cardPaymentMethod:StoredCardPaymentMethod? = self.getStoredCardPaymentMethod(paymentMethods!,index: index)
    var paymentMethodMap:Dictionary? = ["type": "scheme","recurringDetailReference":cardPaymentMethod?.identifier]
    let resultData = ["paymentMethod":paymentMethodMap,"storePaymentMethod":true] as [String : Any]
   self.sendEvent(
     withName: "onPaymentSubmit",
     body: [
       "isDropIn":self.isDropIn,
       "env":self.envName,
       "data": resultData
     ]
   )
  }


    @objc func cardPaymentMethod(_ paymentMethodsJson: String, name: String, showHolderField: Bool, showStoreField: Bool,buttonTitle: String) {
    self.isDropIn = false
    self.threeDS2Component = nil
    self.customCardComponent?.viewController.dismiss(animated: true)
    let jsonData: Data? = paymentMethodsJson.data(using: String.Encoding.utf8) ?? Data()
    let paymentMethods: PaymentMethods? = try? JSONDecoder().decode(PaymentMethods.self, from: jsonData!)
    let cardPaymentMethod: CardPaymentMethod? = self.getCardPaymentMethodByName(paymentMethods!, name: name)

    let cardComponent = CustomCardComponent(paymentMethod:cardPaymentMethod!,
                                            publicKey: self.publicKey!,buttonTitle: buttonTitle)
    self.customCardComponent = cardComponent
    self.customCardComponent!.showsStorePaymentMethodField = showStoreField
    self.customCardComponent!.showsHolderNameField = showHolderField
    self.customCardComponent!.delegate = self
    self.customCardComponent!.environment = self.env!

    // When you're ready to go live, change this to .live
    // or to other environment values described in https://adyen.github.io/adyen-ios/Docs/Structs/Environment.html
    dispatch {
      UIApplication.shared.delegate?.window??.rootViewController!.present(cardComponent.viewController, animated: true)
    }
  }

  /// Invoked when the payment component finishes, typically by a user submitting their payment details.
  ///
  /// - Parameters:
  ///   - data: The data supplied by the payment component.
  ///   - component: The payment component from which the payment details were submitted.
  func didSubmit(_ data: PaymentComponentData, from _: PaymentComponent) {
    self.cardComponent?.viewController.dismiss(animated: true)
    self.customCardComponent?.viewController.dismiss(animated: true)

    var paymentMethodMap: Dictionary? = data.paymentMethod.dictionaryRepresentation
    paymentMethodMap!["recurringDetailReference"] = paymentMethodMap!["storedPaymentMethodId"]
    let resultData = ["paymentMethod": paymentMethodMap, "storePaymentMethod": data.storePaymentMethod] as [String: Any]

    sendEvent(
      withName: "onPaymentSubmit",
      body: [
        "isDropIn": self.isDropIn,
        "env": self.envName,
        "data": resultData,
      ]
    )
  }

  /// Invoked when the payment component fails.
  ///
  /// - Parameters:
  ///   - error: The error that occurred.
  ///   - component: The payment component that failed.
  func didFail(with error: Error, from _: PaymentComponent) {
    cardComponent?.viewController.dismiss(animated: true)
    customCardComponent?.viewController.dismiss(animated: true)

    sendEvent(
      withName: "onPaymentFail",
      body: [
        "isDropIn": self.isDropIn,
        "env": self.envName,
        "msg": error.localizedDescription,
        "error": String(describing: error),
      ]
    )
  }
}

extension AdyenDropInPayment: ActionComponentDelegate {
  @objc func handleAction(_ actionJson: String) {
    if(actionJson == nil||actionJson.count<=0){
        return;
    }
    var parsedJson = actionJson.replacingOccurrences(of: "THREEDS2FINGERPRINT", with: "threeDS2Fingerprint")
    parsedJson = actionJson.replacingOccurrences(of: "THREEDS2CHALLENGE", with: "threeDS2Challenge")
    parsedJson = actionJson.replacingOccurrences(of: "REDIRECT", with: "redirect")
    if(self.isDropIn!){
        let actionData: Data? = parsedJson.data(using: String.Encoding.utf8) ?? Data()
        let action = try? JSONDecoder().decode(Action.self, from: actionData!)
        DispatchQueue.main.async {
            self.dropInComponent?.handle(action!);
        }
      return;
    }
    let actionData: Data? = parsedJson.data(using: String.Encoding.utf8) ?? Data()
    let action:Action? = try! JSONDecoder().decode(Action.self, from: actionData!)

    switch action {
    /// Indicates the user should be redirected to a URL.
    case .redirect(let executeAction):
       let redirectComponent:RedirectComponent = RedirectComponent(action: executeAction)
       redirectComponent.delegate = self
      break;
      /// Indicates a 3D Secure device fingerprint should be taken.
    case .threeDS2Fingerprint(let executeAction):
      if(self.threeDS2Component == nil){
        self.threeDS2Component = ThreeDS2Component()
        self.threeDS2Component!.delegate = self
      }
      self.threeDS2Component!.handle(executeAction)
      break;
      /// Indicates a 3D Secure challenge should be presented.
    case .threeDS2Challenge(let executeAction):
      if(self.threeDS2Component == nil){
        self.threeDS2Component = ThreeDS2Component()
        self.threeDS2Component!.delegate = self
      }
      self.threeDS2Component?.handle(executeAction)
      break;
    default :
      break;
    }
  }
  @objc func handlePaymentResult(_ paymentResult: String) {
    DispatchQueue.main.async {
        self.dropInComponent?.viewController.dismiss(animated: true)
    }
  }

  /// Invoked when the action component finishes
  /// and provides the delegate with the data that was retrieved.
  ///
  /// - Parameters:
  ///   - data: The data supplied by the action component.
  ///   - component: The component that handled the action.
  func didProvide(_ data: ActionComponentData, from _: ActionComponent) {
    let resultData = ["details": data.details.dictionaryRepresentation, "paymentData": data.paymentData] as [String: Any]
    sendEvent(
      withName: "onPaymentProvide",
      body: [
        "isDropIn": self.isDropIn as Any,
        "env": self.envName as Any,
        "data": resultData,
      ]
    )
  }

  /// Invoked when the action component fails.
  ///
  /// - Parameters:
  ///   - error: The error that occurred.
  ///   - component: The component that failed.
  func didFail(with error: Error, from _: ActionComponent) {
    sendEvent(
      withName: "onPaymentFail",
      body: [
        "isDropIn": self.isDropIn as Any,
        "env": self.envName as Any,
        "msg": error.localizedDescription,
        "error": String(describing: error),
      ]
    )
  }
}
