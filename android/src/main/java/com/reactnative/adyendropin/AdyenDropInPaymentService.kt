package com.reactnative.adyendropin

import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.dropin.service.DropInServiceResult
import org.json.JSONObject

class AdyenDropInPaymentService : DropInService() {
    init {
        AdyenDropInPayment.dropInService = this
    }

    override fun onPaymentsCallRequested(paymentComponentState: PaymentComponentState<*>, paymentComponentJson: JSONObject) {
        AdyenDropInPayment.INSTANCE?.triggerPaymentsCall(paymentComponentState, paymentComponentJson)
    }

    override fun onDetailsCallRequested(actionComponentData: ActionComponentData, actionComponentJson: JSONObject) {
        AdyenDropInPayment.INSTANCE?.triggerDetailsCall(actionComponentData, actionComponentJson)
    }

    fun handleResult(result: DropInServiceResult) {
        sendResult(result)
    }
}
