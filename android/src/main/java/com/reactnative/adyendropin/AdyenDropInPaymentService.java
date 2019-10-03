package com.reactnative.adyendropin;

import android.content.Intent;

import com.adyen.checkout.dropin.service.CallResult;
import com.adyen.checkout.dropin.service.DropInService;

import org.json.JSONObject;

public class AdyenDropInPaymentService extends DropInService {
    @Override
    public CallResult makeDetailsCall(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new CallResult(CallResult.ResultType.FINISHED, "");
        }
        return new CallResult(CallResult.ResultType.FINISHED, jsonObject.toString());
    }

    @Override
    public CallResult makePaymentsCall(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new CallResult(CallResult.ResultType.FINISHED, "");
        }
        return new CallResult(CallResult.ResultType.FINISHED, jsonObject.toString());
    }

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

    }

    public void handleAsyncCallback(CallResult callResult) {
        super.asyncCallback(callResult);
    }
}
