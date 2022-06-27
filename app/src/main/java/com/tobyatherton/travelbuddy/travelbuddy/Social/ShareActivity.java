package com.tobyatherton.travelbuddy.travelbuddy.Social;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.tobyatherton.travelbuddy.travelbuddy.R;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ShareActivity extends AppCompatActivity {

    //https://developer.telerik.com/featured/social-media-integration-android/
    Button shareButton, imageButton;
    EditText textEntry;
    ImageView mImageView;

    private static final int SELECT_PHOTO = 100;

    BitmapDrawable bitmapDrawable;
    Bitmap bitmap1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        shareButton = (Button) findViewById(R.id.share_text_button);
        imageButton = (Button) findViewById(R.id.share_image_button);
        textEntry = (EditText) findViewById(R.id.share_text_entry);
        mImageView = (ImageView) findViewById(R.id.share_picture_thumbnail);

        shareButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
 /*               String userEntry = textEntry.getText().toString();

                Intent textShareIntent = new Intent(Intent.ACTION_SEND);
                textShareIntent.putExtra(Intent.EXTRA_TEXT, userEntry);
                textShareIntent.setType("text/plain");
                startActivity(Intent.createChooser(textShareIntent, "Share text with..."));*/

                //write this code in your share button or function

                String shareText=textEntry.getText().toString();
                Intent shareIntent=new Intent(Intent.ACTION_SEND);
                shareIntent.setType("*/*"); //set share type
                bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();// get the from imageview or use your drawable from drawable folder

                if(bitmapDrawable != null) {
                    bitmap1 = bitmapDrawable.getBitmap();
                    String imgBitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap1, "title", null);
                    Uri imgBitmapUri = Uri.parse(imgBitmapPath);
                    shareIntent.putExtra(Intent.EXTRA_STREAM,imgBitmapUri);
                }

                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent,"Share Journey using:"));

            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

    }

    //http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;

                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    bitmap1 = BitmapFactory.decodeStream(imageStream);
                    mImageView.setImageBitmap(bitmap1);
                }
        }
    }
}
