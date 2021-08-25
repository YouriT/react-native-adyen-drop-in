package com.reactnative.adyendropin

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType as RT
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.adyen.checkout.components.ActionComponentData
import com.adyen.checkout.components.PaymentComponentState
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.adyen3ds2.Adyen3DS2Configuration
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.dropin.service.DropInServiceResult
import com.adyen.checkout.redirect.RedirectComponent

class AdyenDropInPayment(val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {
    companion object {
        lateinit var INSTANCE: AdyenDropInPayment
        lateinit var dropInService: AdyenDropInPaymentService
    }

    init {
        INSTANCE = this
        reactContext.addActivityEventListener(this)
    }

    fun parseConfig(configuration: ReadableMap): DropInConfiguration {
        val config = Util.convertMapToJson(configuration)
        val clientKey = config.getString("clientKey")

        val environment = config.getString("environment")
        val env = when (environment) {
            "liveEurope" -> Environment.EUROPE
            "liveAustralia" -> Environment.AUSTRALIA
            "liveUnitedStates" -> Environment.UNITED_STATES
            else -> Environment.TEST
        }

        val locale = try {
            val shopperLocale = config.getString("shopperLocale").split("_")
            Locale(shopperLocale[0], shopperLocale[1])
        } catch (t: Throwable) {
            Locale.getDefault()
        }

        val amountJson = config.getJSONObject("amount")
        val amount = Amount.SERIALIZER.deserialize(amountJson)

        val cardConfigBuilder = CardConfiguration.Builder(locale, env, clientKey)
        try {
            val showsHolderNameField = config.getJSONObject("card").getBoolean("showsHolderNameField")
            cardConfigBuilder.setHolderNameRequired(showsHolderNameField)
        } catch (t: Throwable) {}
        try {
            val showsStorePaymentMethodField = config.getJSONObject("card").getBoolean("showsStorePaymentMethodField")
            cardConfigBuilder.setShowStorePaymentField(showsStorePaymentMethodField)
        } catch (t: Throwable) {}
        try {
            val shopperReference = config.getJSONObject("card").getString("shopperReference")
            cardConfigBuilder.setShopperReference(shopperReference)
        } catch (t: Throwable) {}

        val adyen3DS2ConfigBuilder = Adyen3DS2Configuration.Builder(locale, env, clientKey)

        val googlePayConfigBuilder = GooglePayConfiguration.Builder(locale, env, clientKey)
        googlePayConfigBuilder.setAmount(amount)
        try {
            googlePayConfigBuilder.setCountryCode(config.getString("countryCode"))
        } catch (t: Throwable) {}

        val builder = DropInConfiguration.Builder(reactContext, AdyenDropInPaymentService::class.java, clientKey)
        builder.setEnvironment(env)
        builder.setShopperLocale(locale)
        builder.setAmount(amount)
        builder.addCardConfiguration(cardConfigBuilder.build())
        builder.add3ds2ActionConfiguration(adyen3DS2ConfigBuilder.build())
        builder.addGooglePayConfiguration(googlePayConfigBuilder.build())
        return builder.build()
    }

    @ReactMethod
    fun startPayment(paymentMethodsJson: ReadableMap, config: ReadableMap) {
        val dropInConfiguration = parseConfig(config)
        val jsonObject = Util.convertMapToJson(paymentMethodsJson)
        val paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject)
        val adyenDropInPayment = this
        getCurrentActivity()?.runOnUiThread {
            DropIn.startPayment(adyenDropInPayment.getCurrentActivity()!!, paymentMethodsApiResponse, dropInConfiguration)
        }
    }

    @ReactMethod
    fun handleResponse(response: ReadableMap?) {
        val serviceResult = if (response != null) {
            val action = response.getMap("action")
            if (action != null) {
                val actionJson = Util.convertMapToJson(action).toString()
                DropInServiceResult.Action(actionJson)
            } else {
                DropInServiceResult.Finished(response.getString("resultCode") ?: "EMPTY")
            }
        } else {
            DropInServiceResult.Error(reason = "IOException")
        }
        dropInService?.handleResult(serviceResult)
    }

    fun triggerPaymentsCall(paymentComponentState: PaymentComponentState<*>, paymentComponentJson: JSONObject) {
        val eventData = Util.convertJsonToMap(paymentComponentJson)
        eventData.putString("returnUrl", RedirectComponent.getReturnUrl(reactContext))
        sendEvent("payment", eventData)
    }

    fun triggerDetailsCall(actionComponentData: ActionComponentData, actionComponentJson: JSONObject) {
        sendEvent("payment-details", Util.convertJsonToMap(actionComponentJson))
    }

    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        val dropInResult = DropIn.handleActivityResult(requestCode, resultCode, data) ?: return
        val resultData = WritableNativeMap()
        when (dropInResult) {
            is DropInResult.CancelledByUser -> {
                resultData.putString("type", "canceled-by-user")
            }
            is DropInResult.Error -> {
                resultData.putString("type", "error")
                resultData.putString("message", dropInResult.reason)
            }
            is DropInResult.Finished -> {
                resultData.putString("type", "finished")
                resultData.putString("result", dropInResult.result)
            }
        }
        this.sendEvent("result", resultData)
    }

    override fun onNewIntent(intent: Intent) = Unit

    override fun getName(): String = this::class.java.simpleName

    protected fun sendEvent(eventName: String, data: WritableMap?) {
        val emitterClass = DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
        reactContext.getJSModule(emitterClass).emit(eventName, data)
    }

    object Util {
        fun convertJsonToMap(jsonObject: JSONObject): WritableMap {
            val map = WritableNativeMap()
            val iterator = jsonObject.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val value = jsonObject.get(key)
                when (value) {
                    is JSONObject -> map.putMap(key, convertJsonToMap(value))
                    is JSONArray -> map.putArray(key, convertJsonToArray(value))
                    is Boolean -> map.putBoolean(key, value)
                    is Int -> map.putInt(key, value)
                    is Double -> map.putDouble(key, value)
                    is String -> map.putString(key, value)
                    else -> map.putString(key, value.toString())
                }
            }
            return map
        }

        fun convertJsonToArray(array: JSONArray): WritableArray {
            val result = WritableNativeArray()
            for (i in 0 until array.length()) {
                val value = array.get(i)
                when (value) {
                    is JSONObject -> result.pushMap(convertJsonToMap(value))
                    is JSONArray -> result.pushArray(convertJsonToArray(value))
                    is Boolean -> result.pushBoolean(value)
                    is Int -> result.pushInt(value)
                    is Double -> result.pushDouble(value)
                    is String -> result.pushString(value)
                    else -> result.pushString(value.toString())
                }
            }
            return result
        }

        fun convertMapToJson(readableMap: ReadableMap): JSONObject {
            val json = JSONObject()
            val iterator = readableMap.keySetIterator()
            while (iterator.hasNextKey()) {
                val key = iterator.nextKey()
                when (readableMap.getType(key)) {
                    RT.Null -> json.put(key, JSONObject.NULL)
                    RT.Boolean -> json.put(key, readableMap.getBoolean(key))
                    RT.Number -> json.put(key, readableMap.getDouble(key))
                    RT.String -> json.put(key, readableMap.getString(key))
                    RT.Map -> json.put(key, convertMapToJson(readableMap.getMap(key)!!))
                    RT.Array -> json.put(key, convertArrayToJson(readableMap.getArray(key)!!))
                }
            }
            return json
        }

        fun convertArrayToJson(readableArray: ReadableArray): JSONArray {
            val json = JSONArray()
            for (i in 0 until readableArray.size()) {
                when (readableArray.getType(i)) {
                    RT.Null -> json.put(JSONObject.NULL)
                    RT.Boolean -> json.put(readableArray.getBoolean(i))
                    RT.Number -> json.put(readableArray.getDouble(i))
                    RT.String -> json.put(readableArray.getString(i))
                    RT.Map -> json.put(convertMapToJson(readableArray.getMap(i)))
                    RT.Array -> json.put(convertArrayToJson(readableArray.getArray(i)))
                }
            }
            return json
        }
    }
}
