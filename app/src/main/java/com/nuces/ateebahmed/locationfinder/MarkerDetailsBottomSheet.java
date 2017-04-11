package com.nuces.ateebahmed.locationfinder;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

/**
 * Created by progamer on 10/04/17.
 */

public class MarkerDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "MrkerDetailsBottomSheet";
    private String stringMessage;
    private AppCompatTextView txtMessage;
    private AppCompatImageView imgResult;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;

    public MarkerDetailsBottomSheet() {}

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    dismiss();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        };

        return inflater.inflate(R.layout.fragment_bottom_sheet_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtMessage = (AppCompatTextView) view.findViewById(R.id.txtMessage);
        imgResult = (AppCompatImageView) view.findViewById(R.id.imgResult);
        String uri = getArguments().getString("msg");
        setMediaResource(uri);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        View view = View.inflate(getContext(), R.layout.fragment_bottom_sheet_dialog, null);
        dialog.setContentView(view);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View)
                view.getParent()).getLayoutParams();
        BottomSheetBehavior behavior = (BottomSheetBehavior) params.getBehavior();
        if (behavior != null)
            behavior.setBottomSheetCallback(bottomSheetCallback);
    }

    public static MarkerDetailsBottomSheet newInstance(String message) {
        MarkerDetailsBottomSheet markerDetails = new MarkerDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("msg", message);
        markerDetails.setArguments(args);
        return markerDetails;
    }

    public String getTxtMessage() {
        return txtMessage.getText().toString();
    }

    public void setStringMessage(String message) {
        stringMessage = message;
    }

    public void setImgResult(int resourceId) {
        imgResult.setImageResource(resourceId);
    }

    private void setMediaResource(String uri) {
        if (uri.toLowerCase().contains("image")) {
            Picasso.with(getContext()).load(uri).into(imgResult);
            imgResult.setVisibility(View.VISIBLE);
        } else {
            txtMessage.setText(uri);
            txtMessage.setVisibility(View.VISIBLE);
        }
    }
}
