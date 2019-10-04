package com.reactnative.adyendropin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.adyen.checkout.base.ComponentView;
import com.adyen.checkout.base.api.ImageLoader;
import com.adyen.checkout.card.CardComponent;
import com.adyen.checkout.card.CardConfiguration;
import com.adyen.checkout.card.CardListAdapter;
import com.adyen.checkout.card.CardView;
import com.adyen.checkout.card.CustomCardUtils;
import com.adyen.checkout.card.data.CardOutputData;


public class CardComponentCustomView extends LinearLayout implements ComponentView<CardComponent>, Observer<CardOutputData> {
    CardListAdapter cardListAdapter;
    CardComponent cardComponent;
    CardConfiguration cardConfiguration;


    public CardComponentCustomView(Context context) {
        this(context, null);

    }

    public CardComponentCustomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardComponentCustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_card_component_custom, this, true);
    }


    @Override
    public void onChanged(CardOutputData cardOutputData) {
        if (!cardComponent.isStoredPaymentMethod()) {
            CustomCardUtils.setFilteredCard(cardListAdapter, CustomCardUtils.getSupportedFilterCards(cardComponent, cardOutputData.getCardNumberField().getValue()));
        }
    }

    @Override
    public void attach(@NonNull CardComponent cardComponent, @NonNull LifecycleOwner lifecycleOwner) {
        this.cardComponent = cardComponent;
        CardView cardView = this.findViewById(R.id.customCardView);

        cardView.attach(cardComponent, lifecycleOwner);

        TextView header = this.findViewById(R.id.header);
        header.setText(R.string.credit_card);

        //cardComponent.getClass().getMethod("observeOutputData")
        //cardComponent.observeOutputData(lifecycleOwner, this);

        if (!cardComponent.isStoredPaymentMethod()) {
            cardListAdapter = CustomCardUtils.newCardList(ImageLoader.getInstance(getContext(), cardConfiguration.getEnvironment()),
                    cardConfiguration.getSupportedCardTypes());
            RecyclerView recyclerView_cardList = this.findViewById(R.id.recyclerView_cardList);
            recyclerView_cardList.setAdapter(cardListAdapter);
        }
    }

    public CardListAdapter getCardListAdapter() {
        return cardListAdapter;
    }

    public void setCardListAdapter(CardListAdapter cardListAdapter) {
        this.cardListAdapter = cardListAdapter;
    }

    public CardComponent getCardComponent() {
        return cardComponent;
    }

    public void setCardComponent(CardComponent cardComponent) {
        this.cardComponent = cardComponent;
    }

    public CardConfiguration getCardConfiguration() {
        return cardConfiguration;
    }

    public void setCardConfiguration(CardConfiguration cardConfiguration) {
        this.cardConfiguration = cardConfiguration;
    }

    @Override
    public boolean isConfirmationRequired() {
        return true;
    }


}
