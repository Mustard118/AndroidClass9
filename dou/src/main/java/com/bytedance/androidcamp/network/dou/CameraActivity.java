package com.bytedance.androidcamp.network.dou;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import android.support.v7.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivityOutput";
    private static final int GET_VIDEO = 100;
    private String ImagePath=null;
    private String VideoPath=null;

    private boolean isRecording = false;
    private String mp4Path;

    private SurfaceView mSurfaceView;
    private ImageView mImageView;
    private VideoView mVideoView;
    private Button mVideoButton;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private SurfaceHolder mHolder;
    private Button Commit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mSurfaceView = findViewById(R.id.surface_view);
        mImageView = findViewById(R.id.iv);
        mVideoView = findViewById(R.id.vv);
        mVideoButton = findViewById(R.id.btn_video);
        Commit=findViewById(R.id.btn_finish);


        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceCallBack());

        initCamera();

        Commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent back=new Intent();
                Bundle bundle=new Bundle();
                bundle.putString("ImgUri",ImagePath);
                bundle.putString("VideoUri",VideoPath);
                Log.d(TAG,"ImgUri"+ImagePath);
                Log.d(TAG,"VideoUri"+VideoPath);
                back.putExtras(bundle);
                setResult(RESULT_OK,back);
                finish();

//                if (VideoPath!=null&&ImagePath!=null){
//                    Intent data=new Intent();
//                    data.putExtra("ImageUri",ImagePath);
//                    data.putExtra("VideoUri",VideoPath);
//                    setResult(RESULT_OK,data);
//                }
//                finish();
            }
        });

        findViewById(R.id.btn_img).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, mPictureCallback);
            }
        });

        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record();
            }
        });
    }

    private void initCamera(){
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.set("orientation", "portrait");
        parameters.set("rotation", 90);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
    }

    private void record(){
        if(isRecording){
            mVideoButton.setText(R.string.start_video);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();

            mVideoView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
            mVideoView.setVideoPath(mp4Path);
            VideoPath=mp4Path;
            Log.d(TAG,"ImgUri"+ImagePath);
            Log.d(TAG,"VideoUri"+VideoPath);
            mVideoView.start();
        }
        else {
            if(prepareVideoRecorder()){
                mVideoButton.setText(R.string.stop_video);
                mMediaRecorder.start();
            }
        }
        isRecording = !isRecording;
    }

    private Bitmap rotateImage(Bitmap bitmap, String path){
        try {
            ExifInterface srcExif = new ExifInterface(path);
            Matrix matrix = new Matrix();
            int angle = 0;
            int orientation = srcExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:{
                    angle = 90;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_180:{
                    angle = 180;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_270:{
                    angle = 270;
                    break;
                }
                default:
                    break;
            }
            matrix.postRotate(angle);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream fos = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + timeStamp + ".jpg";
            File file = new File(filePath);
            try {
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                int targetWidth = mImageView.getWidth();
                int targetHeight = mImageView.getHeight();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(filePath, options);
                int photoWidth = options.outWidth;
                int photoHeight = options.outHeight;
                int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);
                options.inJustDecodeBounds = false;
                options.inSampleSize =scaleFactor;
                Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                Bitmap rotateBitmap = rotateImage(bitmap, filePath);
                ImagePath=filePath;
                Log.d(TAG,"ImgUri"+ImagePath);
                Log.d(TAG,"VideoUri"+VideoPath);
                Log.d(TAG,"ImgUri"+filePath);
                mImageView.setVisibility(View.VISIBLE);
                mVideoView.setVisibility(View.GONE);
                mImageView.setImageBitmap(rotateBitmap);
            } catch (Exception e){
                e.printStackTrace();
            }finally {
                mCamera.startPreview();
                if(fos != null){
                    try {
                        fos.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private class SurfaceCallBack implements SurfaceHolder.Callback{
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(holder.getSurface() == null){
                return;
            }
            mCamera.stopPreview();
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera == null){
            initCamera();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }

    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mp4Path = getOutputMediaPath();
        mMediaRecorder.setOutputFile(mp4Path);
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());
        mMediaRecorder.setOrientationHint(90);
        try {
            mMediaRecorder.prepare();
        }catch (Exception e){
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private String getOutputMediaPath(){
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir, "VIDEO_" + timeStamp + ".mp4");
        if(!mediaFile.exists()){
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }

    private void releaseMediaRecorder(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
    }
}