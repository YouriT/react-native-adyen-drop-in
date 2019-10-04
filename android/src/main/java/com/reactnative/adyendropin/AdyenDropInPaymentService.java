package com.reactnative.adyendropin;

import android.content.Intent;

import com.adyen.checkout.base.ActionComponentData;
import com.adyen.checkout.base.PaymentComponentState;
import com.adyen.checkout.base.model.payments.request.PaymentComponentData;
import com.adyen.checkout.dropin.service.CallResult;
import com.adyen.checkout.dropin.service.DropInService;

import org.json.JSONObject;

public class AdyenDropInPaymentService extends DropInService {
    @Override
    public CallResult makeDetailsCall(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new CallResult(CallResult.ResultType.FINISHED, "");
        }
        if (AdyenDropInPayment.INSTANCE != null) {
            AdyenDropInPayment.INSTANCE.handlePaymentProvide(ActionComponentData.SERIALIZER.deserialize(jsonObject));
        }
        return new CallResult(CallResult.ResultType.WAIT, jsonObject.toString());
    }

    @Override
    public CallResult makePaymentsCall(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new CallResult(CallResult.ResultType.FINISHED, "");
        }
        PaymentComponentData paymentComponentData = PaymentComponentData.SERIALIZER.deserialize(jsonObject);
        PaymentComponentState paymentComponentState = new PaymentComponentState(paymentComponentData, false);
        if (AdyenDropInPayment.INSTANCE != null) {
            AdyenDropInPayment.INSTANCE.handlePaymentSubmit(paymentComponentState);
        }
        return new CallResult(CallResult.ResultType.WAIT, jsonObject.toString());
    }

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

    }


    public void handleAsyncCallback(CallResult callResult) {
        super.asyncCallback(callResult);
    }
}
