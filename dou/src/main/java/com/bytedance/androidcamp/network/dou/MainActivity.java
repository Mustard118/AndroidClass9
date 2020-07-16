package com.bytedance.androidcamp.network.dou;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bytedance.androidcamp.network.dou.api.IMiniDouyinService;
import com.bytedance.androidcamp.network.dou.model.GetVideosResponse;
import com.bytedance.androidcamp.network.dou.model.PostVideoResponse;
import com.bytedance.androidcamp.network.dou.model.Video;
import com.bytedance.androidcamp.network.dou.util.ResourceUtils;
import com.bytedance.androidcamp.network.lib.util.ImageHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_PERMISSION = 1;
    private String[] mPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;
    private static final int ACTIVATE_MY_CAMERA=5;
    private static final String TAG = "MainActivityOutput";
    private int SEE_ME;
    private RecyclerView mRv;
    private List<Video> mVideos = new ArrayList<>();
    public Uri mSelectedImage;
    private Uri mSelectedVideo;
    public Button mBtn;
    private Button mBtnRefresh;
    private ImageView imageView;

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(IMiniDouyinService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private IMiniDouyinService miniDouyinService = retrofit.create(IMiniDouyinService.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBtns();
        initRecyclerView();
        fetchFeed(mBtnRefresh);
    }
    private void initBtns() {

        mBtn = findViewById(R.id.btn_select);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(GrantPermissions(mPermissions)){
                    //startActivity(new Intent(MainActivity.this, CameraActivity.class));

                    Intent intent=new Intent();
                    String ImgUri="";
                    String VideoUri="";
                    intent.putExtra("ImgUri",ImgUri);
                    intent.putExtra("VideoUri",VideoUri);
                    intent.setClass(MainActivity.this, CameraActivity.class);
                    startActivityForResult(intent,ACTIVATE_MY_CAMERA);

                }
                else{
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions(mPermissions, REQUEST_PERMISSION);
                }
            }
        });
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = mBtn.getText().toString();
                if (getString(R.string.select_an_image).equals(s)) {
                    //DONE
                    //@TODO 1填充选择图片的功能
                    chooseImage();
                } else if (getString(R.string.select_a_video).equals(s)) {
                    //DONE
                    //@TODO 2填充选择视频的功能
                    chooseVideo();
                } else if (getString(R.string.post_it).equals(s)) {
                    if (mSelectedVideo != null && mSelectedImage != null) {
                        //DONE
                        //@TODO 3调用上传功能
                        postVideo();
                    } else {
                        throw new IllegalArgumentException("error data uri, mSelectedVideo = "
                                + mSelectedVideo
                                + ", mSelectedImage = "
                                + mSelectedImage);
                    }
                }
            }
        });

        CheckBox mCheckBox = findViewById(R.id.cb);
        mBtnRefresh = findViewById(R.id.btn_refresh);

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SEE_ME = 1;
                    Toast.makeText(MainActivity.this, "只看我", Toast.LENGTH_SHORT).show();
                } else {
                    SEE_ME = 0;
                    Toast.makeText(MainActivity.this, "所有人", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;
        public TextView textView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            textView = itemView.findViewById(R.id.tv);
        }

        public void bind(final Activity activity, final Video video) {
            ImageHelper.displayWebImage(video.imageUrl, img);
            textView.setText(video.userName);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VideoActivity.launch(activity, video.videoUrl);
                }
            });
        }
    }

    private void initRecyclerView() {
        mRv = findViewById(R.id.rv);
        mRv.setLayoutManager(new GridLayoutManager(this,2));
        mRv.setAdapter(new RecyclerView.Adapter<MyViewHolder>() {
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                return new MyViewHolder(
                        LayoutInflater.from(MainActivity.this)
                                .inflate(R.layout.video_item_view, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int i) {
                final Video video = mVideos.get(i);
                viewHolder.bind(MainActivity.this, video);
            }

            @Override
            public int getItemCount() {
                return mVideos.size();
            }
        });
    }

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE);
//        ActivityCompat.requestPermissions(MainActivity.this,permissions,PERMISSION_REQUEST_CAMERA_CODE);
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File file = new File(dirPath, "nihao.jpg");
//        imageUri = Uri.fromFile(file);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
    }

    private Uri imageUri,videoUri;
    String[] permissions = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};
    private final static int PERMISSION_REQUEST_CAMERA_CODE = 1;
    private final static int REQUEST_CODE_TAKE_PHOTO = 2;
    private final static int REQUEST_CODE_RECORD = 3;

    public void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO);
//        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        File file = new File(dirPath, "nihao.mp4");
//        videoUri = Uri.fromFile(file);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
//        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);
//        if(intent.resolveActivity(getPackageManager()) != null){
//            startActivityForResult(intent,REQUEST_CODE_RECORD);
//        }
    }

    public static Uri getUriForFile(Context context,String path) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context.getApplicationContext(), context.getApplicationContext().getPackageName() + ".fileprovider", new File(path));
        } else {
            return Uri.fromFile(new File(path));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = ["
                + requestCode
                + "], resultCode = ["
                + resultCode
                + "], data = ["
                + data
                + "]");

        if (resultCode == RESULT_OK && null != data) {
            if(requestCode==ACTIVATE_MY_CAMERA){
                Bundle b = data.getExtras();
                String UriString=b.getString("ImgUri");
                mSelectedImage = Uri.fromFile(new File(UriString));
                Log.d(TAG,mSelectedImage.toString());
                UriString=b.getString("VideoUri");
                mSelectedVideo=Uri.fromFile(new File(UriString));
                Log.d(TAG,mSelectedVideo.toString());
                mBtn.setText(R.string.post_it);
            }
            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage.toString());
                mBtn.setText(R.string.select_a_video);
            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo.toString());
                mBtn.setText(R.string.post_it);
            }
        }
    }

    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        File f = new File(ResourceUtils.getRealPath(MainActivity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }

    private void postVideo() {
        mBtn.setText(R.string.posting);
        mBtn.setEnabled(false);
        MultipartBody.Part coverImagePart = getMultipartFromUri("cover_image", mSelectedImage);
        MultipartBody.Part videoPart = getMultipartFromUri("video", mSelectedVideo);
        //DONE
        //@TODO 4下面的id和名字替换成自己的
        miniDouyinService.postVideo("0", "Tester", coverImagePart, videoPart).enqueue(
                new Callback<PostVideoResponse>() {
                    @Override
                    public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                        if (response.body() != null) {
                            Toast.makeText(MainActivity.this, response.body().toString(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                        mBtn.setText(R.string.select_an_image);
                        mBtn.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Call<PostVideoResponse> call, Throwable throwable) {
                        mBtn.setText(R.string.select_an_image);
                        mBtn.setEnabled(true);
                        Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void fetchFeed(View view) {
        mBtnRefresh.setText(R.string.requesting);
        mBtnRefresh.setEnabled(false);
        miniDouyinService.getVideos().enqueue(new Callback<GetVideosResponse>() {
            @Override
            public void onResponse(Call<GetVideosResponse> call, Response<GetVideosResponse> response) {
                if (response.body() != null && response.body().videos != null) {
                    mVideos = response.body().videos;
                    //DONE
                    //@TODO  5服务端没有做去重，拿到列表后，可以在端侧根据自己的id，做列表筛选。
                    if(SEE_ME == 1)
                        for(int i=0 , len = mVideos.size(); i < len; i++)
                            if(!("18911265161".equals(mVideos.get(i).studentId))) {
                                mVideos.remove(i);
                                i--;
                                len--;
                            }
                    mRv.getAdapter().notifyDataSetChanged();
                }
                mBtnRefresh.setText(R.string.refresh_feed);
                mBtnRefresh.setEnabled(true);
            }

            @Override
            public void onFailure(Call<GetVideosResponse> call, Throwable throwable) {
                mBtnRefresh.setText(R.string.refresh_feed);
                mBtnRefresh.setEnabled(true);
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private boolean GrantPermissions(String[] permissions){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        for(String permission : permissions){
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
}
