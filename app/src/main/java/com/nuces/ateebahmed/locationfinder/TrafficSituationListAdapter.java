package com.nuces.ateebahmed.locationfinder;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by progamer on 10/05/17.
 */

public class TrafficSituationListAdapter extends ArrayAdapter{

    private String[] options;
    private TypedArray icons;
    private Context context;

    public TrafficSituationListAdapter(@NonNull Context context, String[] options, TypedArray ids) {
        super(context, R.layout.text_message_list_item, options);
        this.options = options;
        this.context = context;
        icons = ids;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = LayoutInflater.from(context).inflate(R.layout.text_message_list_item, null, true);

        AppCompatTextView tvOption = (AppCompatTextView) row.findViewById(R.id.tvTextListItem);
        tvOption.setText(options[position]);
        AppCompatImageView ivIcon = (AppCompatImageView) row.findViewById(R.id.ivIconListView);
        ivIcon.setImageResource(icons.getResourceId(position, R.drawable.blocked));

        return row;
    }

    @Override
    public void clear() {
        icons.recycle();
        super.clear();
    }
}
