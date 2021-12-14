import Adyen
import Foundation
import PassKit

@objc(AdyenDropInPayment)
class AdyenDropInPayment: RCTEventEmitter {
  var dropInComponent: DropInComponent?
  var returnUrl: String?

  @objc override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func supportedEvents() -> [String]! {
    return [
      "payment",
      "payment-details",
      "result",
    ]
  }

  func parseConfig(_ config: NSDictionary) -> DropInComponent.Configuration {
    let env = config["environment"] as? String ?? "test"
    let environment: Environment = {
      if (env == "liveEurope") { return Environment.liveEurope }
      else if (env == "liveUnitedStates") { return Environment.liveUnitedStates }
      else if (env == "liveAustralia") { return Environment.liveAustralia }
      else { return Environment.test }
    }()

    returnUrl = config["returnUrl"] as? String

    let clientKey = config["clientKey"] as! String
    let apiContext = APIContext(environment: environment, clientKey: clientKey)
    let configuration = DropInComponent.Configuration(apiContext: apiContext)

    let amountDict: [String:Any] = config["amount"] as? [String:Any] ?? [:]
    let amount = Amount(value: (amountDict["value"] as! NSNumber).intValue, currencyCode: amountDict["currency"] as! String)

    let countryCode = config["countryCode"] as! String
    configuration.payment = Payment(amount: amount, countryCode: countryCode)

    let card: [String:Any] = config["card"] as? [String:Any] ?? [:]
    configuration.card.showsHolderNameField = card["showsHolderNameField"] as? Bool ?? false
    configuration.card.showsStorePaymentMethodField = card["showsStorePaymentMethodField"] as? Bool ?? false

    if let applePay: [String:Any] = config["applePay"] as? [String:Any] {
      configuration.applePay = ApplePayComponent.Configuration(
        summaryItems: [PKPaymentSummaryItem(
          label: config["summaryLabel"] as? String ?? "Total",
          amount: AmountFormatter.decimalAmount(
            amount.value,
            currencyCode: amount.currencyCode
          ),
          type: .final
        )],
        merchantIdentifier: applePay["merchantIdentifier"] as! String
      )
    }

    return configuration
  }

  @objc func startPayment(_ paymentMethodsObj: NSDictionary, configObj: NSDictionary) {
    let paymentMethods = try! decode(paymentMethodsObj) as PaymentMethods
    let configuration = parseConfig(configObj)

    dropInComponent = DropInComponent(paymentMethods: paymentMethods, configuration: configuration)
    dropInComponent?.delegate = self

    dispatch {
      UIApplication.shared.keyWindow?.rootViewController?.adyen.topPresenter.present(
        self.dropInComponent!.viewController,
        animated: true
      )
    }
  }

  @objc func handleResponse(_ responseObj: NSDictionary?) {
    do {
      let response = try decode(responseObj) as AdyenResponse
      if let action = response.action {
        dispatch {
          self.dropInComponent?.handle(action)
        }
      } else {
        finish(resultCode: response.resultCode)
      }
    } catch {
      finish(error: error)
    }
  }

  @objc func handleReturnUrl(_ url: String) {
    if url.hasPrefix(returnUrl ?? "") {
      dispatch {
        RedirectComponent.applicationDidOpen(from: URL(string: url)!)
      }
    }
  }

  func finish(resultCode: AdyenResponse.ResultCode? = nil, error: Error? = nil) {
    let success = resultCode == .authorised || resultCode == .received || resultCode == .pending
    dispatch {
      self.dropInComponent?.finalizeIfNeeded(with: success)
      self.dropInComponent?.viewController.presentingViewController?.dismiss(animated: true, completion: {
        self.dropInComponent = nil
        if resultCode != nil {
          self.sendEvent(withName: "result", body: [
            "type": "finished",
            "result": resultCode?.rawValue,
          ])
        } else if error != nil {
          self.sendEvent(withName: "result", body: [
            "type": "error",
            "message": error?.localizedDescription,
            "detail": String(describing: error),
          ])
        }
      })
    }
  }

  func decode<T: Decodable>(_ obj: NSDictionary?) throws -> T {
    if !JSONSerialization.isValidJSONObject(obj as Any) { throw UnknownError() }
    return try Coder.decode(JSONSerialization.data(withJSONObject: obj as Any))
  }

  func encode<T: Encodable>(_ obj: T) -> [String: Any] {
    let data = try? JSONSerialization.jsonObject(with: Coder.encode(obj))
    return data as? [String: Any] ?? [:]
  }

  func dispatch(_ closure: @escaping () -> Void) {
    if Thread.isMainThread { closure() }
    else { DispatchQueue.main.async(execute: closure) }
  }
}

extension AdyenDropInPayment : DropInComponentDelegate {
  /// Invoked when a payment method is selected and the initial details have been filled.
  func didSubmit(_ data: PaymentComponentData, for paymentMethod: PaymentMethod, from component: DropInComponent) {
    sendEvent(withName: "payment", body: [
      "paymentMethod": encode(data.paymentMethod.encodable),
      "storePaymentMethod": data.storePaymentMethod,
      "returnUrl": returnUrl as Any,
    ])
  }

  /// Invoked when additional details have been provided for a payment method.
  func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
    sendEvent(withName: "payment-details", body: [
      "paymentData": data.paymentData as Any,
      "details": encode(data.details.encodable),
    ])
  }

  /// Invoked when the action component finishes,
  /// without any further steps needed by the application, for example in case of voucher payment methods.
  /// The application just needs to dismiss the `DropInComponent`.
  func didComplete(from component: DropInComponent) {
    finish(resultCode: AdyenResponse.ResultCode.authorised)
  }

  /// Invoked when the drop in component failed with an error.
  func didFail(with error: Error, from component: DropInComponent) {
    finish(error: error)
  }

  /// Invoked when user closes a payment component.
  func didCancel(component: PaymentComponent, from dropInComponent: DropInComponent) {
    sendEvent(withName: "result", body: [
      "type": "canceled-by-user",
    ])
  }
}

internal struct AdyenResponse: Decodable {
  internal let resultCode: ResultCode
  internal let action: Action?

  internal init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    self.resultCode = try container.decode(ResultCode.self, forKey: .resultCode)
    self.action = try container.decodeIfPresent(Action.self, forKey: .action)
  }

  private enum CodingKeys: String, CodingKey {
    case resultCode
    case action
  }

  enum ResultCode: String, Decodable {
    case authorised = "Authorised"
    case refused = "Refused"
    case pending = "Pending"
    case cancelled = "Cancelled"
    case error = "Error"
    case received = "Received"
    case redirectShopper = "RedirectShopper"
    case identifyShopper = "IdentifyShopper"
    case challengeShopper = "ChallengeShopper"
    case presentToShopper = "PresentToShopper"
  }
}
