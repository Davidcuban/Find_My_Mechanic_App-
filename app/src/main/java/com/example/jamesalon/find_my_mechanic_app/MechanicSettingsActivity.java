package com.example.jamesalon.find_my_mechanic_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MechanicSettingsActivity extends AppCompatActivity {
    private EditText mNameField, mPhoneField;
    private Button mConfirm, mBack;
    private FirebaseAuth mAuth;
    private DatabaseReference mMechanicDatabase;
    private String userId;
    private String mName;
    private String mPhone;
    private String mProfileImageUrl;
    private ImageView mProfileImage;
    private Uri resultUri;
    private RadioGroup mRadioGroup;
    private String mService;
    private int Galler_Intent =1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_settings);
        mNameField = (EditText) findViewById(R.id.name);
        mPhoneField = (EditText) findViewById(R.id.phone);

        mProfileImage = (ImageView) findViewById(R.id.profileimage);

        mConfirm = (Button) findViewById(R.id.confirm);
        mBack = (Button) findViewById(R.id.back);
        mRadioGroup =(RadioGroup)findViewById(R.id.radiogroup);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mMechanicDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Mechanics").child(userId);
        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, Galler_Intent);
            }
        });


        mConfirm.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });


        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }


    private void getUserInfo() {

        mMechanicDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if (map.get("phone") != null) {
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if (map.get("service") != null) {
                        mService = map.get("service").toString();
                        switch (mService){
                            case "Engine":
                                mRadioGroup.check(R.id.engine);
                                break;
                            case "Tyres":
                                mRadioGroup.check(R.id.tyres);
                                break;
                            case "Electrical":
                                mRadioGroup.check(R.id.electric);
                                break;
                        }
                    }
                    if (map.get("profileImageUrl") != null) {
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void saveUserInformation() {
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();

        int selectedId =mRadioGroup.getCheckedRadioButtonId();

        final RadioButton radioButton = (RadioButton)findViewById(selectedId);

        if(radioButton.getText()==null){
            return;
        }

        mService = radioButton.getText().toString();


        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("service", mService);
        mMechanicDatabase.updateChildren(userInfo);

        if(resultUri != null) {

            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    Map newImage = new HashMap();
                     newImage.put("profileImageUrl", downloadUrl.toString());
                    mMechanicDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        }else{
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}
