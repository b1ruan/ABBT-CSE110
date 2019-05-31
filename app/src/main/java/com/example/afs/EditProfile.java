package com.example.afs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.lang.Double;
import java.util.Map;
import java.util.UUID;

public class EditProfile extends AppCompatActivity {
    private ImageButton save;
    private EditText ageText;
    private EditText heightText;
    private EditText weightText;
    private EditText usernameText;
    private ImageView photo;
    private Button changePhoto;
    private DatabaseReference db;
    private FirebaseAuth mAuth;
    private FirebaseUser curUser;
    private String userID;
    private Uri filePath;
    FirebaseStorage storage;
    StorageReference storageReference;

    private final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);

        usernameText = (EditText) findViewById(R.id.Username);
        ageText = (EditText) findViewById(R.id.edit_age);
        heightText = (EditText) findViewById(R.id.edit_height);
        weightText = (EditText) findViewById(R.id.edit_weight);

        //initialize database
        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null)
        {
            curUser = mAuth.getCurrentUser();
        }

        userID = curUser.getUid();

        //profile photo part
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        photo = (ImageView) findViewById(R.id.photo);
        changePhoto = (Button) findViewById(R.id.profile_photo);
        changePhoto.setBackgroundColor(Color.TRANSPARENT);
        changePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chooseImage();
            }
        });


        usernameText.setText(getIntent().getStringExtra("name"));
        ageText.setText(getIntent().getStringExtra("age"));
        heightText.setText(getIntent().getStringExtra("height"));
        weightText.setText(getIntent().getStringExtra("weight"));


        save = (ImageButton) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                enterProfile();
                uploadImage();
            }
        });

    }

    private void enterProfile() {
        int age = 0;
        double height = 0;
        double weight = 0;

        String username = usernameText.getText().toString();

        String ageStr = ageText.getText().toString();
        try {
            age = Integer.parseInt(ageStr);
        }catch (NumberFormatException e){
            System.out.println("Not a number");
        }

        String heightStr = heightText.getText().toString();
        try {
            height = Double.parseDouble(heightStr);
        }catch (NumberFormatException e){
            System.out.println("Not a number");
        }


        String weightStr = weightText.getText().toString();
        try {
            weight = Double.parseDouble(weightStr);
        }catch (NumberFormatException e){
            System.out.println("Not a number");
        }

        FirebaseUser fUser = mAuth.getCurrentUser();
        UserInfo user = new UserInfo(username, weight, height, Gender.UNKNOWN, age, fUser.getEmail());
        Map<String, Object> userInfoMap = user.toMap();
        db.child("Users").child(userID).updateChildren(userInfoMap);


        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            db.child("Users").child(userID).child("Photo_Path")
                    .setValue("gs://abbt-a95ad.appspot.com/images/" + userID);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                photo.setImageBitmap(bitmap);
                Profile.photo.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void uploadImage() {

        if(filePath != null)
        {
            //final ProgressDialog progressDialog = new ProgressDialog(this);
            //progressDialog.setTitle("Uploading...");
            //progressDialog.show();

            StorageReference ref = storageReference.child("images/"+ userID);
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //progressDialog.dismiss();
                            //Toast.makeText(EditProfile.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //progressDialog.dismiss();
                            //Toast.makeText(EditProfile.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            //progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }
}

