package com.reactnative.adyendropin;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.adyen.checkout.base.ComponentError;
import com.adyen.checkout.base.PaymentComponentState;
import com.adyen.checkout.base.model.paymentmethods.PaymentMethod;
import com.adyen.checkout.card.CardComponent;
import com.adyen.checkout.card.CardConfiguration;
import com.adyen.checkout.core.exeption.CheckoutException;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class CardComponentBottomSheet extends BottomSheetDialogFragment implements Observer<PaymentComponentState> {

    public static final String CARD_BOOT_SHEET_TAG = "_custome_card_componet_sheet";
    PaymentMethod paymentMethod;
    CardComponent component;
    CardConfiguration cardConfiguration;
    Button payButton;
    CardComponentCustomView customCardView;
    private Integer dialogInitViewState = BottomSheetBehavior.STATE_COLLAPSED;
    final AdyenDropInPayment adyenDropInPayment;

    public CardComponentBottomSheet(AdyenDropInPayment adyenDropInPayment) {
        this.adyenDropInPayment = adyenDropInPayment;
    }


    void setInitViewState(Integer firstViewState) {
        this.dialogInitViewState = firstViewState;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public CardComponent getComponent() {
        return component;
    }

    public void setComponent(CardComponent component) {
        this.component = component;
    }

    public CardConfiguration getCardConfiguration() {
        return cardConfiguration;
    }

    public void setCardConfiguration(CardConfiguration cardConfiguration) {
        this.cardConfiguration = cardConfiguration;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(component instanceof CardComponent)) {
            throw new CheckoutException("Component is not card");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_component_custom_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        customCardView = (CardComponentCustomView) view;
        customCardView.setCardConfiguration(cardConfiguration);
        customCardView.attach(component, this);

        this.component.observe(this, this);
        this.component.observeErrors(this, new Observer<ComponentError>() {
            @Override
            public void onChanged(ComponentError componentError) {
                adyenDropInPayment.handlePaymentError(componentError);
            }
        });
        this.payButton = customCardView.findViewById(R.id.customPayButton);
        if (customCardView.isConfirmationRequired()) {
            payButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPayment();
                }
            });
            this.setInitViewState(BottomSheetBehavior.STATE_EXPANDED);
            customCardView.requestFocus();
        } else {
            payButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void startPayment() {
        PaymentComponentState componentState = component.getState();
        try {
            if (componentState != null) {
                if (componentState.isValid()) {
                } else {
                    throw new CheckoutException("PaymentComponentState are not valid.");
                }
            } else {
                throw new CheckoutException("PaymentComponentState are null.");
            }
        } catch (CheckoutException e) {
            e.printStackTrace();
            this.adyenDropInPayment.handlePaymentError(new ComponentError(e));
            return;
        }
        this.adyenDropInPayment.handlePaymentSubmit(componentState);
        this.hide();

    }


    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        final CardComponentBottomSheet bottomSheetView = this;
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    //onBackPressed();
                }
                return false;
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                FrameLayout frameLayout = ((BottomSheetDialog) dialog).findViewById(R.id.design_bottom_sheet);

                BottomSheetBehavior behavior = BottomSheetBehavior.from(frameLayout);

                if (bottomSheetView.dialogInitViewState == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setSkipCollapsed(true);
                }
                behavior.setState(bottomSheetView.dialogInitViewState);
            }
        });
        return dialog;
    }

    @Override
    public void onChanged(PaymentComponentState paymentComponentState) {
        payButton.setEnabled(paymentComponentState != null && paymentComponentState.isValid());
        if (payButton.isEnabled()) {
            payButton.setBackgroundColor(getResources().getColor(R.color.primaryColor));
        }
    }

    private void hideFragmentDialog(String tag) {
        getFragmentByTag(tag).dismiss();
    }

    private DialogFragment getFragmentByTag(String tag) {
        FragmentManager fragmentManager = this.getActivity().getSupportFragmentManager();
        DialogFragment fragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        return fragment;
    }

    public void hide() {
        super.dismiss();
    }

    @Override
    public int getTheme() {
        return R.style.AdyenCheckout_BottomSheetDialogTheme;
    }

    public void show(FragmentManager fragmentManager) {
        this.show(fragmentManager, CARD_BOOT_SHEET_TAG);
    }
}
