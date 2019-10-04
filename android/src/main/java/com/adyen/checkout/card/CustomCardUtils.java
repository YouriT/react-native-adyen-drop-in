package com.adyen.checkout.card;

import android.os.Bundle;

import com.adyen.checkout.base.api.ImageLoader;
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod;
import com.adyen.checkout.card.data.CardType;
import com.adyen.checkout.dropin.DropInConfiguration;
import com.adyen.checkout.dropin.ui.component.CardComponentDialogFragment;

import java.util.List;

public class CustomCardUtils {
    static String PAYMENT_METHOD = "PAYMENT_METHOD";
    static String WAS_IN_EXPAND_STATUS = "WAS_IN_EXPAND_STATUS";
    static String DROP_IN_CONFIGURATION = "DROP_IN_CONFIGURATION";

    public static CardListAdapter newCardList(ImageLoader imageLoader, List<CardType> supportedCards) {
        return new CardListAdapter(imageLoader, supportedCards);
    }

    public static CardComponentDialogFragment newCardComponentFragment(PaymentMethod paymentMethod, DropInConfiguration dropInConfiguration) {
        CardComponentDialogFragment fragment = new CardComponentDialogFragment();
        fragment.setPaymentMethod(paymentMethod);
        fragment.setDropInConfiguration(dropInConfiguration);
        Bundle args = new Bundle();
        args.putParcelable(PAYMENT_METHOD, paymentMethod);
        args.putBoolean(WAS_IN_EXPAND_STATUS, false);
        args.putParcelable(DROP_IN_CONFIGURATION, dropInConfiguration);

        fragment.setArguments(args);
        return fragment;
    }

    public static void setFilteredCard(CardListAdapter cardListAdapter, List<CardType> filteredCards) {
        cardListAdapter.setFilteredCard(filteredCards);
        //cardListAdapter.notifyDataSetChanged();
    }

    public static List<CardType> getSupportedFilterCards(CardComponent cardComponent,String cardNumber){
        return cardComponent.getSupportedFilterCards(cardNumber);
    }
}
