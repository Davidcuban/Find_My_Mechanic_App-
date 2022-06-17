package com.example.jamesalon.find_my_mechanic_app.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.jamesalon.find_my_mechanic_app.HistorySingleActivity;
import com.example.jamesalon.find_my_mechanic_app.R;

/**
 * Created by James Alon on 02-Mar-20.
 */

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {
    TextView historyId;
    TextView time;

    public HistoryViewHolders(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        historyId = (TextView)itemView.findViewById(R.id.historyId);
        time = (TextView)itemView.findViewById(R.id.time);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(),HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("historyId",historyId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);

    }
}
