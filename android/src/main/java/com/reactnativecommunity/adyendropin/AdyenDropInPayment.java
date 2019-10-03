package com.reactnativecommunity.adyendropin;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.adyen.checkout.base.ComponentError;
import com.adyen.checkout.base.PaymentComponentState;
import com.adyen.checkout.base.model.PaymentMethodsApiResponse;
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod;
import com.adyen.checkout.base.model.paymentmethods.RecurringDetail;
import com.adyen.checkout.base.model.payments.request.PaymentMethodDetails;
import com.adyen.checkout.card.CardComponent;
import com.adyen.checkout.card.CardConfiguration;
import com.adyen.checkout.core.api.Environment;
import com.adyen.checkout.dropin.DropIn;
import com.adyen.checkout.dropin.DropInConfiguration;
import com.adyen.checkout.dropin.service.CallResult;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativeadyendropin.AdyenDropInPaymentService;
import com.reactnativeadyendropin.CardComponentBottomSheet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AdyenDropInPayment extends ReactContextBaseJavaModule {
    CardConfiguration cardConfiguration;
    DropInConfiguration dropInConfiguration;
    String publicKey;
    Environment environment;
    String envName;


    public AdyenDropInPayment(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void configPayment(String publicKey, String env) {
        this.publicKey = publicKey;
        this.envName = env;
        if (env == null || env.trim().length() <= 0) {
            environment = Environment.TEST;
        } else {
            if (env.equalsIgnoreCase("test")) {
                environment = Environment.TEST;
            } else {
                environment = Environment.EUROPE;
            }
        }

    }

    @ReactMethod
    public void paymentMethods(String paymentMethodsJson) {
        CardConfiguration cardConfiguration =
                new CardConfiguration.Builder(Locale.getDefault(), environment, publicKey)
                        .build();
        this.cardConfiguration = cardConfiguration;
        Intent resultIntent = new Intent(this.getCurrentActivity(), this.getCurrentActivity().getClass());
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.dropInConfiguration = new DropInConfiguration.Builder(this.getCurrentActivity(), resultIntent, AdyenDropInPaymentService.class).addCardConfiguration(cardConfiguration).build();
        JSONObject jsonObject = null;
        try {

            jsonObject = new JSONObject(paymentMethodsJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PaymentMethodsApiResponse paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject);
        final AdyenDropInPayment adyenDropInPayment = this;
        this.getCurrentActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                DropIn.startPayment(adyenDropInPayment.getCurrentActivity(), paymentMethodsApiResponse, dropInConfiguration);
            }
        });

    }


    @ReactMethod
    public void cardPaymentMethod(String paymentMethodsJson, String name, Boolean showHolderField, Boolean showStoreField) {

        final AdyenDropInPayment adyenDropInPayment = this;
//        ViewStub viewStub= this.getCurrentActivity().findViewById(R.id.adyenCardViewViewStub);
//        final CardView cardView = (CardView) viewStub.inflate();
        //final CardView cardView  = this.getCurrentActivity().findViewById(R.id.adyenCardView) ;
        JSONObject jsonObject = null;

        try {

            jsonObject = new JSONObject(paymentMethodsJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        CardConfiguration cardConfiguration =
                new CardConfiguration.Builder(Locale.getDefault(), environment, publicKey).setHolderNameRequire(showHolderField).setShowStorePaymentField(showStoreField)
                        .build();
        this.cardConfiguration = cardConfiguration;
        Intent resultIntent = new Intent(this.getCurrentActivity(), this.getCurrentActivity().getClass());
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.dropInConfiguration = new DropInConfiguration.Builder(this.getCurrentActivity(), resultIntent, AdyenDropInPaymentService.class).addCardConfiguration(cardConfiguration).build();

        PaymentMethodsApiResponse paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject);

        // When you're ready to accept live payments, change the value to one of our live environments.

        this.cardConfiguration = cardConfiguration;
        final PaymentMethod paymentMethod = adyenDropInPayment.getCardPaymentMethod(paymentMethodsApiResponse, name);

//        final CardView cardView = new CardView(adyenDropInPayment.getCurrentActivity());
//        cardView.setVisibility(View.VISIBLE);
//        cardView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        this.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CardComponent cardComponent =new CardComponent(paymentMethod,cardConfiguration);

                CardComponentBottomSheet cardComponentDialogFragment = new CardComponentBottomSheet(adyenDropInPayment);
                cardComponentDialogFragment.setPaymentMethod(paymentMethod);
                cardComponentDialogFragment.setCardConfiguration(cardConfiguration);
                cardComponentDialogFragment.setComponent(cardComponent);
                cardComponentDialogFragment.setCancelable(true);
                cardComponentDialogFragment.setShowsDialog(true);

//                Intent intent=new Intent(adyenDropInPayment.getCurrentActivity(), CardComponentActivity.class);
//                intent.putExtra("cardConfiguration",cardConfiguration);
//                intent.putExtra("paymentMethod",paymentMethod);
                //adyenDropInPayment.getCurrentActivity().startActivity(intent);
//                CardComponentBottomSheet bottomSheet= CardComponentBottomSheet.newInstance(1);
//                cardView.attach(cardComponent, bottomSheet);
                cardComponentDialogFragment.show(((FragmentActivity) adyenDropInPayment.getCurrentActivity()).getSupportFragmentManager());

            }
        });

    }


    @ReactMethod
    public void storedCardPaymentMethod(String paymentMethodsJson, Integer index) {

        JSONObject jsonObject = null;

        try {

            jsonObject = new JSONObject(paymentMethodsJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PaymentMethodsApiResponse paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject);

        CardConfiguration cardConfiguration =
                new CardConfiguration.Builder(Locale.getDefault(), environment, publicKey)
                        .build();
        this.cardConfiguration = cardConfiguration;
        final AdyenDropInPayment adyenDropInPayment = this;
        RecurringDetail paymentMethod =  this.getStoredCardPaymentMethod(paymentMethodsApiResponse, index);
        this.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CardComponent cardComponent =new CardComponent(paymentMethod,cardConfiguration);
                CardComponentBottomSheet cardComponentDialogFragment = new CardComponentBottomSheet(adyenDropInPayment);
                cardComponentDialogFragment.setPaymentMethod(paymentMethod);
                cardComponentDialogFragment.setCardConfiguration(cardConfiguration);
                cardComponentDialogFragment.setComponent(cardComponent);
                cardComponentDialogFragment.setCancelable(true);
                cardComponentDialogFragment.setShowsDialog(true);

//                Intent intent=new Intent(adyenDropInPayment.getCurrentActivity(), CardComponentActivity.class);
//                intent.putExtra("cardConfiguration",cardConfiguration);
//                intent.putExtra("paymentMethod",paymentMethod);
                //adyenDropInPayment.getCurrentActivity().startActivity(intent);
//                CardComponentBottomSheet bottomSheet= CardComponentBottomSheet.newInstance(1);
//                cardView.attach(cardComponent, bottomSheet);
                cardComponentDialogFragment.show(((FragmentActivity) adyenDropInPayment.getCurrentActivity()).getSupportFragmentManager());

            }
        });
    }

    @ReactMethod
    public void handleAction(String actionJson) {
        AdyenDropInPaymentService dropInService = this.getDropInService();
        CallResult callResult = new CallResult(CallResult.ResultType.ACTION, actionJson);
        dropInService.handleAsyncCallback(callResult);
    }


    @NonNull
    @Override
    public String getName() {
        return AdyenDropInPayment.class.getSimpleName();
    }

    AdyenDropInPaymentService getDropInService() {
        return null;
    }

    public void handlePaymentSubmit(PaymentComponentState paymentComponentState) {
        if (paymentComponentState.isValid()) {
            WritableMap eventData = new WritableNativeMap();
            WritableMap data = new WritableNativeMap();
            PaymentMethodDetails paymentMethodDetails = paymentComponentState.getData().getPaymentMethod();
            JSONObject jsonObject = PaymentMethodDetails.SERIALIZER.serialize(paymentMethodDetails);
            try {
                WritableMap paymentMethodMap=convertJsonToMap(jsonObject);
                data.putMap("paymentMethod", paymentMethodMap);
                data.putBoolean("storePaymentMethod", paymentComponentState.getData().isStorePaymentMethodEnable());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            eventData.putBoolean("isDropIn", false);
            eventData.putString("env", this.envName);
            eventData.putMap("data", data);
            this.sendEvent(this.getReactApplicationContext(), "onPaymentSubmit", eventData);
        }

    }

    public static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    public static ReadableArray convertJsonToArray(JSONArray array) throws JSONException {
        WritableNativeArray result = new WritableNativeArray();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONObject) {
                result.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                result.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                result.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                result.pushInt((Integer) value);
            } else if (value instanceof Double) {
                result.pushDouble((Double) value);
            } else if (value instanceof String) {
                result.pushString((String) value);
            } else {
                result.pushString(value.toString());
            }
        }

        return result;

    }

    void handlePaymentError(ComponentError componentError) {
        WritableMap resultData = new WritableNativeMap();
        resultData.putBoolean("isDropIn", false);
        resultData.putString("env", this.envName);
        resultData.putString("msg", componentError.getErrorMessage());
        resultData.putString("error", componentError.getException().getMessage());
        this.sendEvent(this.getReactApplicationContext(), "onPaymentFail", resultData);
    }

    PaymentMethod getCardPaymentMethod(PaymentMethodsApiResponse
                                               paymentMethodsApiResponse, String name) {
        List<PaymentMethod> paymentMethodList = paymentMethodsApiResponse.getPaymentMethods();
        if (name == null || name.trim().length() <= 0) {
            name = "Credit Card";
        }
        for (PaymentMethod paymentMethod : paymentMethodList) {
            if (paymentMethod.getName().equalsIgnoreCase(name)) {
                return paymentMethod;
            }
        }
        return null;
    }

    RecurringDetail getStoredCardPaymentMethod(PaymentMethodsApiResponse
                                                     paymentMethodsApiResponse, Integer index) {
        List<RecurringDetail> recurringDetailList = paymentMethodsApiResponse.getStoredPaymentMethods();
        if (recurringDetailList == null || recurringDetailList.size() <= 0) {
            return null;
        }
        if (recurringDetailList.size() == 1) {
            return recurringDetailList.get(0);
        }
        return recurringDetailList.get(index);
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
