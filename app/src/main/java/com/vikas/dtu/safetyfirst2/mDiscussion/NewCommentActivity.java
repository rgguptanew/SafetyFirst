package com.vikas.dtu.safetyfirst2.mDiscussion;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.commonsware.cwac.richedit.Effect;
import com.commonsware.cwac.richedit.RichEditText;
import com.commonsware.cwac.richedit.StyleEffect;
import com.commonsware.cwac.richedit.TypefaceEffect;
import com.commonsware.cwac.richtextutils.Selection;
import com.commonsware.cwac.richtextutils.SpannedXhtmlGenerator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.vikas.dtu.safetyfirst2.BaseActivity;
import com.vikas.dtu.safetyfirst2.R;
import com.vikas.dtu.safetyfirst2.mData.Comment;
import com.vikas.dtu.safetyfirst2.mData.User;
import com.vikas.dtu.safetyfirst2.model.PostNotify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class NewCommentActivity extends BaseActivity implements View.OnClickListener{

    private RichEditText mCommentField;
    private Button mCommentButton;

    private DatabaseReference mDatabase;
    private DatabaseReference mCommentsReference;
    private String mPostKey;
    private FirebaseStorage storage;
    private DatabaseReference mAttachmentsReference;

    private Button mBoldButton;
    private Button mItalicButton;
    private Button mUnderlineButton;

    private String key = null;
    private String path;
    private Uri uriSavedImage;
    String imagePath = null, pdfPath = null, attachLink = null;
    String downloadImageURL = null, downloadVideoURL = null, downloadPdfURL = null;

    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_IMAGE = 2;
    private static final int SELECT_PDF = 3;
    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 5;
    private int FILE_ATTACH = 2, IMAGE_ATTACH = 3, VIDEO_ATTACH = 4;

    private String URL = "gs://safetyfirst-aec72.appspot.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment2);

        mCommentField = (RichEditText) findViewById(R.id.editor);
        mCommentButton = (Button) findViewById(R.id.button_post_comment);
        mBoldButton = (Button) findViewById(R.id.bold_button);
        mItalicButton = (Button) findViewById(R.id.italic_button);
        mUnderlineButton = (Button) findViewById(R.id.underline_button);

        mBoldButton.setOnClickListener(this);
        mItalicButton.setOnClickListener(this);
        mUnderlineButton.setOnClickListener(this);
        mCommentButton.setOnClickListener(this);

        mPostKey = getIntent().getStringExtra(PostDetailActivity.EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }
        storage = FirebaseStorage.getInstance();
        mDatabase =  FirebaseDatabase.getInstance().getReference();
        mCommentsReference = mDatabase.child("post-comments").child(mPostKey);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Answer");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        // return true if you handled the button click, otherwise return false.
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_post_comment:
                if (!mCommentField.getText().toString().trim().equals("")) {
                    key = mCommentsReference.push().getKey();
                    mAttachmentsReference = FirebaseDatabase.getInstance().getReference().child("comment-attachments").child(mPostKey).child(key);
                    postComment();
                    if (imagePath != null) uploadImage();
                    if (pdfPath != null) uploadPDF();
                } else
                    Toast.makeText(this, "Write valid answer.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bold_button:
                mCommentField.applyEffect(RichEditText.BOLD, !mCommentField.hasEffect(RichEditText.BOLD));
                break;
            case R.id.italic_button:
                mCommentField.applyEffect(RichEditText.ITALIC, !mCommentField.hasEffect(RichEditText.ITALIC));
                break;
            case R.id.underline_button:
                mCommentField.applyEffect(RichEditText.UNDERLINE, !mCommentField.hasEffect(RichEditText.UNDERLINE));
                break;

        }
    }

    public void uploadImage() {
        Uri file = Uri.fromFile(new File(imagePath));
        /// upload destination. change according to your needs
        UploadTask uploadTask = storage.getReferenceFromUrl(URL).child(getUid() + "/image/" + file.getLastPathSegment()).putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                downloadImageURL = String.valueOf(downloadUrl);
                assert downloadUrl != null;
                Toast.makeText(getBaseContext(), "Image Uploaded", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("DOWNLOAD_URI", downloadUrl);
                setResult(RESULT_OK, intent);

                Map<String, Object> imageAttach = new HashMap<>();
                imageAttach.put("/post-comments/" + mPostKey + "/" + key + "/image/", downloadImageURL);
                mDatabase.updateChildren(imageAttach);

                pushNode(IMAGE_ATTACH, downloadImageURL);

                finish();

            }
        });
    }

    private void postComment() {
        mCommentButton.setClickable(false);
        mCommentButton.setBackgroundColor(getResources().getColor(R.color.grey));
        final String uid = getUid();
        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user information
                        User user = dataSnapshot.getValue(User.class);
                        String authorName = user.username;

                        // Create new comment object
                        SpannedXhtmlGenerator htmlText = new SpannedXhtmlGenerator();
                        String xmlText = htmlText.toXhtml(mCommentField.getText());
                        String commentText = mCommentField.getText().toString();

                        writeNewComment(uid, authorName, commentText, xmlText, downloadImageURL, downloadPdfURL);

                        // Clear the field
                        mCommentField.setText(null);

                        // Add post-key to local DB to check for notification //
                        final DatabaseReference postNotifyRef = mDatabase.child("post-notify");
                        final Realm realm = Realm.getDefaultInstance();
                        PostNotify postNotify = realm.where(PostNotify.class).equalTo("postKey", mPostKey).findFirst();
                        if (postNotify == null) {
                            realm.beginTransaction();
                            postNotify = realm.createObject(PostNotify.class);
                            postNotify.setUid(getUid());
                            postNotify.setPostKey(mPostKey);
                            final PostNotify finalPostNotify = postNotify;
                            postNotifyRef.child(mPostKey).child("num_of_comments").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    finalPostNotify.setNumComments(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                                    realm.commitTransaction();
                                    postNotifyRef.child(mPostKey).child("num_of_comments").setValue(finalPostNotify.getNumComments());
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                            /* For Star click
                            postNotifyRef.child(mPostKey).child("num_of_stars").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    finalPostNotify.setNumStars(Integer.parseInt(dataSnapshot.getValue().toString()));
                                    postNotifyRef.child(mPostKey).child("num_of_stars").setValue(finalPostNotify.getNumStars());
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                            */
                        } else {
                            realm.beginTransaction();
                            final PostNotify finalPostNotify = realm.where(PostNotify.class).equalTo("postKey",mPostKey).findFirst();

                            postNotifyRef.child(mPostKey).child("num_of_comments").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    finalPostNotify.setNumComments(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                                    realm.commitTransaction();
                                    postNotifyRef.child(mPostKey).child("num_of_comments").setValue(finalPostNotify.getNumComments());
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                        //  END Realm Stuff //

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        mCommentButton.setClickable(true);

        if (imagePath != null || pdfPath != null)
            return;

        finish();
        //  mCommentButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    private void writeNewComment(String uid, String authorName, String commentText, String xmlText, String downloadImageURL, String downloadPdfURL) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        Comment comment = new Comment(uid, authorName, commentText, xmlText, downloadImageURL, downloadPdfURL);

        Map<String, Object> commentValues = comment.toMap();


        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/post-comments/" + mPostKey + "/" + key, commentValues);

        mDatabase.updateChildren(childUpdates);
    }

    public void uploadPDF() {
        Uri file = Uri.fromFile(new File(pdfPath));
        /// upload destination. change according to your needs
        UploadTask uploadTask = storage.getReferenceFromUrl(URL).child(getUid() + "/pdf/" + file.getLastPathSegment()).putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                assert downloadUrl != null;
                Toast.makeText(getBaseContext(), "PDF uploaded", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("DOWNLOAD_URI", downloadUrl);
                setResult(RESULT_OK, intent);
                pushNode(FILE_ATTACH, String.valueOf(downloadUrl));
                finish();
            }
        });
    }

    public void pushNode(int ID, String AttachUrl) {
        String type;

        if (ID == FILE_ATTACH) {
            type = "FILE_ATTACH";
        } else if (ID == IMAGE_ATTACH) {
            type = "IMAGE_ATTACH";
        } else type = "null";

        mAttachmentsReference.child(type).setValue(AttachUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) {
                String filename;
                filename = compressImage(String.valueOf(uriSavedImage));
                imagePath = filename;
                //    Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();
            } else if (requestCode == SELECT_IMAGE) {
                Uri selectedImageUri = data.getData();
                Cursor cursor = getContentResolver().query(selectedImageUri, null, null, null, null);
                if (cursor == null) {
                    Log.d("response", "NUll");
                    imagePath = selectedImageUri.getPath();
                } else {
                    Log.d("response", "Not nUll");
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    imagePath = cursor.getString(index);
                }
                Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();
            } else if (requestCode == SELECT_PDF) {
                pdfPath = data.getData().getPath();
                Toast.makeText(this, pdfPath, Toast.LENGTH_SHORT).show();
            }


        }
    }

    private void startAction() {
        final CharSequence[] items = {"Take Photo", "Choose From Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(NewCommentActivity.this);
        builder.setTitle("Select An Option");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    path = System.currentTimeMillis() + "_compressed_" + ".jpg";
                    uriSavedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                            path));
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Choose From Gallery")) {
                    //    Toast.makeText(getBaseContext(), "choosing from gallery", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"),
                            SELECT_IMAGE);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public void uploadImage(View view) {
        //  startAction();

        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startAction();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startAction();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "External Storage and Camera permission is required to read images from device", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA},
                        ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
            }

        }
    }

    public String compressImage(String imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {

            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            Log.d("response", "Error 4");
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            Log.d("response", "Error 3");
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        assert scaledBitmap != null;
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        ExifInterface exif;

        try {
            exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            Log.d("response", "Error 2");
            e.printStackTrace();
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (FileNotFoundException e) {
            Log.d("response", "Error Main");
            e.printStackTrace();
        }

        return filename;
    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            Log.d("response", "NUll");
            return contentUri.getPath();
        } else {
            Log.d("response", "Not nUll");
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public String getFilename() {
        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + "_compressed_" + ".jpg");
        return String.valueOf(destination);

    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        Log.d("response", "Sample Size");
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public void uploadFile(View view) {
        pickPDF();
    }

    private void pickPDF() {
        Intent pickPdf = new Intent(Intent.ACTION_GET_CONTENT);
        pickPdf.setType("application/pdf");
        startActivityForResult(pickPdf, SELECT_PDF);
    }
}
