package com.vikas.dtu.safetyfirst2.mDiscussion;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.commonsware.cwac.richtextutils.SpannableStringGenerator;
import com.squareup.picasso.Picasso;
import com.vikas.dtu.safetyfirst2.BaseActivity;
import com.vikas.dtu.safetyfirst2.R;
import com.vikas.dtu.safetyfirst2.mData.Comment;
import com.vikas.dtu.safetyfirst2.mData.Post;
import com.vikas.dtu.safetyfirst2.mData.User;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vikas.dtu.safetyfirst2.mWebview.WebViewActivity;
import com.vikas.dtu.safetyfirst2.model.PostNotify;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import io.realm.Realm;

public class PostDetailActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PostDetailActivity";

    public static final String EXTRA_POST_KEY = "post_key";

    //URL_REGEX STRINGS FOR HYPERLINK IN COMMENT
    //Extra String(Not used anywhere)
    public static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
    //Original String
    public static final String URL_REGEX2 = "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)";

    private DatabaseReference mPostReference, mPostAttachmentsReference, mUserPostReference;
    private DatabaseReference mCommentsReference, mCommentAttachmentsReference;
    private ValueEventListener mPostListener;
    private String mPostKey;
    private CommentAdapter mAdapter;

    private ImageView mAuthorImage;
    private ImageView mImageView;
    private TextView mAuthorView;
    private TextView mTitleView;
    private TextView mBodyView;
    // Removed mCommentField Since clicking on Post answer will open NewCommentActivity
    private Button mCommentButton;
    private RecyclerView mCommentsRecycler;

    private ImageButton mImageButton;
    // private Button mVideoButton;
    private ImageButton mFileButton;
    private ImageButton mLinkButton;

    private String mImageUrl;
    private String mVideoUrl;
    private String mFileUrl;
    private String mLinkUrl;
    TextView tv_loading;
    String dest_file_path = "test.pdf";
    int downloadedSize = 0, totalsize;
    float per = 0;
    boolean postLoaded = false;
    private Menu mMenu;

    private LinearLayoutManager mManager;

    private LinearLayoutManager mImageManager;
    private ArrayList<String> imageList;
    private RecyclerView mImageRecycler;

    private Post post;
    //  private int clickcount =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up round_blue_dark
        //  ab.setDisplayHomeAsUpEnabled(true);


        // Get post key from intent
        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
        if (mPostKey == null) {
            throw new IllegalArgumentException("Must pass EXTRA_POST_KEY");
        }

        // Initialize Database
        mPostReference = FirebaseDatabase.getInstance().getReference()
                .child("posts").child(mPostKey);
        mCommentsReference = FirebaseDatabase.getInstance().getReference()
                .child("post-comments").child(mPostKey);
        mPostAttachmentsReference = FirebaseDatabase.getInstance().getReference().child("post-attachments").child(mPostKey);
        mUserPostReference = FirebaseDatabase.getInstance().getReference().child("user-posts").child(getUid()).child(mPostKey);
        mCommentAttachmentsReference = FirebaseDatabase.getInstance().getReference().child("comment-attachments").child(mPostKey);

        // Initialize Views
        mAuthorImage = (ImageView) findViewById(R.id.post_author_photo);
        mAuthorView = (TextView) findViewById(R.id.post_author);
        mTitleView = (TextView) findViewById(R.id.post_title);
        mBodyView = (TextView) findViewById(R.id.post_body);
        mImageView = (ImageView) findViewById(R.id.post_image);

        mImageButton = (ImageButton) findViewById(R.id.image_btn);
        mFileButton = (ImageButton) findViewById(R.id.file_btn);
        // mVideoButton = (Button) findViewById(R.id.video_btn);
        mLinkButton = (ImageButton) findViewById(R.id.link_btn);

        mCommentButton = (Button) findViewById(R.id.button_post_comment);
        mCommentsRecycler = (RecyclerView) findViewById(R.id.recycler_comments);
        mImageRecycler = (RecyclerView) findViewById(R.id.recycler_images);

        mCommentButton.setOnClickListener(this);
        mImageButton.setOnClickListener(this);
        mFileButton.setOnClickListener(this);
        // mVideoButton.setOnClickListener(this);
        mLinkButton.setOnClickListener(this);
        // mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mCommentsRecycler.setLayoutManager(mManager);

        mImageManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mImageRecycler.setLayoutManager(mImageManager);
    }


    @Override
    public void onStart() {
        super.onStart();

        // Add value event listener to the post
        // [START post_value_event_listener]
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                post = dataSnapshot.getValue(Post.class);
                // [START_EXCLUDE]
                if (post.getPhotoUrl() == null) {
                    mAuthorImage.setImageDrawable(ContextCompat.getDrawable(getBaseContext(),
                            R.drawable.ic_action_account_circle_40));
                } else {
                    Glide.with(getBaseContext())
                            .load(post.getPhotoUrl())
                            .into(mAuthorImage);
                }

                if (post.getImage() != null) {
                    Glide.with(getBaseContext())
                            .load(post.getImage())
                            .into(mImageView);
                    mImageView.setVisibility(View.VISIBLE);
                } else {
                    mImageView.setVisibility(View.GONE);
                }
                mAuthorView.setText(post.author);
                mTitleView.setText(post.title);
                // mBodyView.setText(post.body); //Replaced by hyperlink text method in line below.
                setHyperlinkText(mBodyView, post.body);

                postLoaded = true;
                onCreateOptionsMenu(mMenu);

                if(post.imageList != null && post.imageList.size() > 1) {
                    //Toast.makeText(PostDetailActivity.this, "Has Image List", Toast.LENGTH_LONG);
                    imageList = post.imageList;
                    mImageView.setVisibility(View.GONE);
                    mImageRecycler.setVisibility(View.VISIBLE);
                    mImageRecycler.setAdapter(new ImagesAdapter(PostDetailActivity.this, imageList));
                }


                //TODO get attachment urls from post if they exist
                // [END_EXCLUDE]
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // [START_EXCLUDE]
                Toast.makeText(PostDetailActivity.this, "Failed to load post.",
                        Toast.LENGTH_SHORT).show();
                // [END_EXCLUDE]
            }
        };
        mPostReference.addValueEventListener(postListener);
        // [END post_value_event_listener]

        // Keep copy of post listener so we can remove it when app stops
        mPostListener = postListener;

        // Listen for comments
        mAdapter = new CommentAdapter(this, mCommentsReference);
        mCommentsRecycler.setAdapter(mAdapter);
    }

    private static void setHyperlinkText(TextView textview, String input) {
        Spanned output;
        String preText, postText;

        Pattern p = Pattern.compile(URL_REGEX2, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        String link = "";
        String[] parts = input.split("\\s");
        preText = "";
        postText = "";
        int flag = 0;
        for (String item : parts) {
            if (!p.matcher(item).matches() && flag == 0) {
                preText += item;
                preText += " ";
            }
            if (p.matcher(item).matches()) {
                link = item;
                //Log.d("Here",item);
                flag = 1;
            }
            if (!p.matcher(item).matches() && flag == 1) {
                postText += " ";
                postText += item;
            }
        }
        if (flag == 1) {
            //Exceptions:
            // 1.) For links starting with "//"
            if (link.startsWith("//")) {
                link = "http:" + link;
            }
            //2.) For links not starting with http or https
            else if (!(link.startsWith("http://")) && !(link.startsWith("https://"))) {
                link = "http://" + link;
            }
            output = Html.fromHtml(preText + "<a href = " + link + ">" + link + "</a>" + postText);
            textview.setMovementMethod(LinkMovementMethod.getInstance());
            textview.setText(output);
        } else {
            SpannableStringGenerator toDisplay = new SpannableStringGenerator();
            try {
                textview.setText(toDisplay.fromXhtml(input));
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove post value event listener
        if (mPostListener != null) {
            mPostReference.removeEventListener(mPostListener);
        }

        // Clean up comments listener
        mAdapter.cleanupListener();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_post_comment:
                Intent intent = new Intent(this, NewCommentActivity.class);  // New Activity to write Comment
                intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, mPostKey);
                startActivity(intent);
                break;
            case R.id.image_btn:
                showImage();
                //  Toast.makeText(this, "image btn clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.file_btn:
                showFile();
                //    Toast.makeText(this, "file btn clicked", Toast.LENGTH_SHORT).show();
                break;
         /*   case R.id.video_btn:
                showVideo();
                Toast.makeText(this, "video btn clicked", Toast.LENGTH_SHORT).show();
                break;*/
            case R.id.link_btn:
                showLink();
                //   Toast.makeText(this, "link btn clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private static class CommentViewHolder extends RecyclerView.ViewHolder {

        public TextView authorView;
        public TextView bodyView;
        //TODO add upvotes and downvotes round_blue_dark

        public ImageView commentImage;

        public CommentViewHolder(View itemView) {
            super(itemView);

            authorView = (TextView) itemView.findViewById(R.id.comment_author);
            bodyView = (TextView) itemView.findViewById(R.id.comment_body);
            commentImage = (ImageView) itemView.findViewById(R.id.comment_image);
        }
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mCommentIds = new ArrayList<>();
        private List<Comment> mComments = new ArrayList<>();

        public CommentAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                    // A new comment has been added, add it to the displayed list
                    Comment comment = dataSnapshot.getValue(Comment.class);

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mCommentIds.add(dataSnapshot.getKey());
                    mComments.add(comment);
                    notifyItemInserted(mComments.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    Comment newComment = dataSnapshot.getValue(Comment.class);
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int commentIndex = mCommentIds.indexOf(commentKey);
                    if (commentIndex > -1) {
                        // Replace with the new data
                        mComments.set(commentIndex, newComment);

                        // Update the RecyclerView
                        notifyItemChanged(commentIndex);
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + commentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so remove it.
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int commentIndex = mCommentIds.indexOf(commentKey);
                    if (commentIndex > -1) {
                        // Remove data from the list
                        mCommentIds.remove(commentIndex);
                        mComments.remove(commentIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(commentIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                    // A comment has changed position, use the key to determine if we are
                    // displaying this comment and if so move it.
                    Comment movedComment = dataSnapshot.getValue(Comment.class);
                    String commentKey = dataSnapshot.getKey();

                    // ...
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                    Toast.makeText(mContext, "Failed to load comments.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position) {
            Comment comment = mComments.get(position);
            holder.authorView.setText(comment.author);
            //  holder.bodyView.setText(comment.text);
            if (comment.xmlText == null)
                setHyperlinkText(holder.bodyView, comment.text);  //For older comments with simple text
            else
                setHyperlinkText(holder.bodyView, comment.xmlText);

            // Display Image in Comment
            if(comment.image != null){
                holder.commentImage.setVisibility(View.VISIBLE);
                Glide.with(mContext).load(comment.image).into(holder.commentImage);
            }

        }

//        private void setHyperlinkText(CommentViewHolder holder, String input) {
//            Spanned output;
//            String preText, postText;
//
//            Pattern p = Pattern.compile(URL_REGEX2, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
//            String link = "";
//            String[] parts = input.split("\\s+");
//            preText = "";
//            postText = "";
//            int flag = 0;
//            for(String item : parts){
//                if(!p.matcher(item).matches()&&flag==0){
//                    preText+= item;
//                    preText+=" ";
//                }
//                if(p.matcher(item).matches()){
//                    link = item;
//                    //Log.d("Here",item);
//                    flag = 1;
//                }
//                if(!p.matcher(item).matches()&&flag==1){
//                    postText+=" ";
//                    postText+= item;
//                }
//            }
//            if(flag==1){
//                //Exceptions:
//                // 1.) For links starting with "//"
//                if(link.startsWith("//")){
//                    link = "http:" + link;
//                }
//                //2.) For links not starting with http or https
//                else if(!(link.startsWith("http://"))&&!(link.startsWith("https://"))){
//                    link = "http://" + link;
//                }
//                output = Html.fromHtml(preText + "<a href = " + link + ">"+ link + "</a>" + postText);
//                holder.bodyView.setMovementMethod(LinkMovementMethod.getInstance());
//                holder.bodyView.setText(output);
//            }
//            else {
//                holder.bodyView.setText(input);
//            }
//
//        }

        @Override
        public int getItemCount() {
            return mComments.size();
        }

        public void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }


    //Called by Link Button from Layout XML
    public void showLink() {
        mPostReference.child("postLink").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String postLink = (String) dataSnapshot.getValue();
                if (postLink != null) {
                    Intent intent = new Intent(PostDetailActivity.this, WebViewActivity.class);
                    intent.putExtra("Url", postLink);
                    startActivity(intent);
                } else {
                    Toast.makeText(PostDetailActivity.this, "No Valid Link", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    //Called by Video Button from Layout XML
    public void showVideo() {
        mPostAttachmentsReference.child("VIDEO_ATTACH").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String url = (String) dataSnapshot.getValue();
                if (url != null) {
                    Intent intentPlayVideo = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intentPlayVideo.setDataAndType(Uri.parse(url), "video/*");
                    startActivity(intentPlayVideo);
                } else {
                    Toast.makeText(PostDetailActivity.this, "No Video Attached", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Called by Image Button from Layout XML
    public void showImage() {
        mPostAttachmentsReference.child("IMAGE_ATTACH").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imageLink = (String) dataSnapshot.getValue();
                if (imageLink != null) {
                    Dialog imageDialog = new Dialog(PostDetailActivity.this);
                    imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    imageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    ImageView image = new ImageView(PostDetailActivity.this);
                    Picasso.with(PostDetailActivity.this).load(imageLink).into(image);
                    Log.d(TAG, (String) dataSnapshot.getValue());
                    imageDialog.addContentView(image, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    imageDialog.show();
                } else {
                    Toast.makeText(PostDetailActivity.this, "No Image Attached", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Called by File Button from Layout XML
    public void showFile() {
        mPostAttachmentsReference.child("FILE_ATTACH").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String url = (String) dataSnapshot.getValue();
                //  Log.d(TAG, url);
                if (url != null) {
                    tv_loading = new TextView(PostDetailActivity.this);
                    setContentView(tv_loading);
                    tv_loading.setGravity(Gravity.CENTER);
                    tv_loading.setTypeface(null, Typeface.BOLD);
                    downloadAndOpenPDF(url);

                } else {
                    Toast.makeText(PostDetailActivity.this, "No File Attached", Toast.LENGTH_SHORT).show();
                }
            }

            void downloadAndOpenPDF(final String url) {
                new Thread(new Runnable() {
                    public void run() {
                        Uri path = Uri.fromFile(downloadFile(url));
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(path, "application/pdf");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } catch (ActivityNotFoundException e) {
                            tv_loading
                                    .setError("PDF Reader application is not installed in your device");
                        }
                    }
                }).start();

            }

            File downloadFile(String dwnload_file_path) {
                File file = null;
                try {

                    URL url = new URL(dwnload_file_path);
                    HttpURLConnection urlConnection = (HttpURLConnection) url
                            .openConnection();

                    //  urlConnection.setRequestMethod("GET");
                    //  urlConnection.setDoOutput(true);

                    // connect
                    urlConnection.connect();

                    // set the path where we want to save the file
                    File SDCardRoot = Environment.getExternalStorageDirectory();
                    // create a new file, to save the downloaded file
                    file = new File(SDCardRoot, dest_file_path);

                    FileOutputStream fileOutput = new FileOutputStream(file);

                    // Stream used for reading the data from the internet
                    InputStream inputStream = urlConnection.getInputStream();

                    // this is the total size of the file which we are
                    // downloading
                    totalsize = urlConnection.getContentLength();
                    setText("Starting PDF download...");

                    // create a buffer...
                    byte[] buffer = new byte[1024 * 1024];
                    int bufferLength = 0;

                    while ((bufferLength = inputStream.read(buffer)) > 0) {
                        fileOutput.write(buffer, 0, bufferLength);
                        downloadedSize += bufferLength;
                        per = ((float) downloadedSize / totalsize) * 100;
                        setText("Total PDF File size  : "
                                + (totalsize / 1024)
                                + " KB\n\nDownloading PDF " + (int) per
                                + "% complete");
                    }
                    // close the output stream when complete //
                    fileOutput.close();
                    setText("Download Complete. Open PDF Application installed in the device.");

                } catch (final MalformedURLException e) {
                    setTextError("Some error occured. Press back and try again.",
                            Color.RED);
                } catch (final IOException e) {
                    setTextError("Some error occured. Press back and try again.",
                            Color.RED);
                } catch (final Exception e) {
                    setTextError(
                            "Failed to download image. Please check your internet connection.",
                            Color.RED);
                }
                return file;
            }

            void setTextError(final String message, final int color) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv_loading.setTextColor(color);
                        tv_loading.setText(message);
                    }
                });

            }

            void setText(final String txt) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv_loading.setText(txt);
                    }
                });
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (postLoaded) {
            if (getUid().equals(post.uid) && (menu != null)) {
                getMenuInflater().inflate(R.menu.post_detail, menu);
                postLoaded = false;
            }
        } else {
            mMenu = menu;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_post:
                // delete post remotely from [  nodes to remove from   posts, user-posts, post-attachments, post-comments ]
                final AlertDialog.Builder post_Del_Alert = new AlertDialog.Builder(this);
                post_Del_Alert.setTitle("Warning").setMessage("Are you sure you want to delete this post?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            if (mPostListener != null) {
                                mPostReference.removeEventListener(mPostListener);
                            }
                            mAdapter.cleanupListener();

                            mPostReference.removeValue();
                            mCommentsReference.removeValue();
                            mPostAttachmentsReference.removeValue();
                            mUserPostReference.removeValue();
                            mCommentAttachmentsReference.removeValue();

                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            if (realm.where(PostNotify.class).equalTo("postKey", mPostKey).findFirst() != null) {
                                realm.where(PostNotify.class).equalTo("postKey", mPostKey).findFirst().deleteFromRealm();
                            }
                            realm.commitTransaction();

                            FirebaseDatabase.getInstance().getReference().child("post-notify").child(mPostKey).removeValue();
                            //Todo:  delete stuff from storage too
                            finish();
                        }
                    });
                post_Del_Alert.create().show();

                break;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        ActivityCompat.finishAfterTransition(this);
    }

    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        public ImageView postImage;

        public ImageViewHolder(View itemView) {
            super(itemView);
            postImage = (ImageView) itemView.findViewById(R.id.post_image);
        }
    }

    private static class ImagesAdapter extends RecyclerView.Adapter<ImageViewHolder> {

        private Context mContext;
        private ArrayList<String> imageList;

        public ImagesAdapter(final Context context, ArrayList<String> imageList) {
            mContext = context;
            this.imageList = imageList;
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.post_detail_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            Glide.with(mContext).load(imageList.get(position)).into(holder.postImage);
        }

        @Override
        public int getItemCount() {
            return imageList.size();
        }
    }
}
