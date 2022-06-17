package com.example.jamesalon.find_my_mechanic_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity {
    private String historyId,currentUseId,customerId,mechanicId,userMechanicOrCustomer;
    private TextView userName;
    private TextView userPhone;
    private TextView date;
    private RatingBar mRatingBar;
    private ImageView imageUser;
    private DatabaseReference historyDbIfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        historyId = getIntent().getExtras().getString("historyId");

        userName = (TextView)findViewById(R.id.userName);
        userPhone = (TextView)findViewById(R.id.userphone);
        date = (TextView)findViewById(R.id.date);
        mRatingBar = (RatingBar)findViewById(R.id.ratingBar);

        imageUser = (ImageView)findViewById(R.id.userimage);

        currentUseId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        historyDbIfo = FirebaseDatabase.getInstance().getReference().child("history").child(historyId);
        getHistoryInfomation();
    }

    private void getHistoryInfomation() {
        historyDbIfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child:dataSnapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                            if(!customerId.equals(currentUseId)){
                                userMechanicOrCustomer ="Mechanics";
                                getUserInfomation("Customers",customerId);
                            }
                        }
                        if(child.getKey().equals("mechanic")){
                            mechanicId = child.getValue().toString();
                            if(!mechanicId.equals(currentUseId)){
                                userMechanicOrCustomer ="Customers";
                                getUserInfomation("Mechanics",mechanicId);
                                displayCustomerRElatedObject();
                            }
                        }
                        if(child.getKey().equals("timeStamp")){
                            date.setText(getDate(Long.valueOf(child.getValue().toString())));

                        }
                        if(child.getKey().equals("rating")){
                          mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));

                        }

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRElatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyDbIfo.child("rating").setValue(rating);
                DatabaseReference mechRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(mechanicId).child("rating");
                mechRatingDb.child(historyId).setValue(rating);
            }
        });
    }

    private void getUserInfomation(String otherUserMechanicOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB =FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserMechanicOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String,Object> map =(Map<String,Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        userPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("name").toString()).into(imageUser);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timeStamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp*1000);
         String date = DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();

        return date;
    }
}
