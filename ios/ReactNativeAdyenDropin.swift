//
//  AdyenDropin.swift
//  ReactNativeAdyenDropin
//
//  Created by 罗立树 on 2019/9/27.
//  Copyright © 2019 Facebook. All rights reserved.
//

import Adyen
import Foundation

@objc class AdyenDropInBridge: NSObject {
  @objc(applicationDidOpenURL:)
  static func applicationDidOpen(_ url: URL) -> Bool {
    let adyenHandled = RedirectComponent.applicationDidOpen(from: url)
    return true
  }
}

@objc class AdyenRedirectBridge: NSObject {
  @objc(applicationDidOpenURL:)
  static func applicationDidOpen(_ url: URL) -> Bool {
    let adyenHandled = RedirectComponent.applicationDidOpen(from: url)
    return adyenHandled
  }
}