//  Summary.swift
//  RNAdyenDropIn
//
//  Created by Octavian Voloaca on 08/07/2021.
//  Copyright © 2021 Facebook. All rights reserved.
//

import Foundation

public struct Summary: Decodable {
    public let title: String
    public let total: Decimal
    public let countryCode: String
    public let currencyCode: String
}
