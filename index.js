import { NativeModules, NativeEventEmitter, Platform, Linking } from 'react-native';
const { AdyenDropInPayment } = NativeModules;
const eventEmitter = new NativeEventEmitter(AdyenDropInPayment);

export default {
  /**
   * Start Drop-in
   * @param {object} paymentMethodJson
   * @param {object} config
   */
  startPayment(paymentMethodJson, config) {
    if (Platform.OS === 'ios' && !this.handleReturnUrl) {
      this.handleReturnUrl = ({ url }) => AdyenDropInPayment.handleReturnUrl(url);
      Linking.addEventListener('url', this.handleReturnUrl);
    }
    return AdyenDropInPayment.startPayment(paymentMethodJson, config);
  },

  /**
   * Handle server response
   * @param {object} response
   */
  handleResponse(response) {
    return AdyenDropInPayment.handleResponse(response);
  },
  addListener(eventName, listener) {
    return eventEmitter.addListener(eventName, listener);
  },
  listenerCount(eventName) {
    if (eventEmitter.listenerCount) {
      return eventEmitter.listenerCount(eventName);
    } else {
      return eventEmitter.listeners(eventName).length;
    }
  },
};
