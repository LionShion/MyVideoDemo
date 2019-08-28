package com.zhaoss.weixinrecorded.activity;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;
import com.libyuv.LibyuvUtil;
import com.zhaoss.weixinrecorded.R;
import com.zhaoss.weixinrecorded.util.CameraHelp;
import com.zhaoss.weixinrecorded.util.MyVideoEditor;
import com.zhaoss.weixinrecorded.util.RecordUtil;
import com.zhaoss.weixinrecorded.util.RxJavaUtil;
import com.zhaoss.weixinrecorded.view.LineProgressView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * 仿微信录制视频
 * 基于ffmpeg视频编译
 *
 * @author zhaoshuang
 * @blame Android Team
 * @since 19/6/18
 */
public class RecordedActivity extends BaseActivity {

    public static final String INTENT_PATH = "intent_path";
    public static final String INTENT_DATA_TYPE = "result_data_type";

    public static final int RESULT_TYPE_VIDEO = 1;
    public static final int RESULT_TYPE_PHOTO = 2;

    public static final int REQUEST_CODE_KEY = 100;

    /**
     * 最大录制时间
     */
    public static final float MAX_VIDEO_TIME = 5f * 1000;

    private SurfaceView surfaceView;
    private LineProgressView lineProgressView;
    private TextView editorTextView;

    /**
     * 分段视频地址
     */
    private ArrayList<String> segmentList = new ArrayList<>();
    /**
     * 分段音频地址
     */
    private ArrayList<String> aacList = new ArrayList<>();
    /**
     * 分段录制时间
     */
    private ArrayList<Long> timeList = new ArrayList<>();

    private CameraHelp mCameraHelp = new CameraHelp();
    private SurfaceHolder mSurfaceHolder;
    private MyVideoEditor mVideoEditor = new MyVideoEditor();
    private RecordUtil recordUtil;

    /**
     * 总编译次数
     */
    private int executeCount;
    /**
     * 编译进度
     */
    private float executeProgress;

    private String audioPath;
    private RecordUtil.OnPreviewFrameListener mOnPreviewFrameListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_recorded);

        LanSoEditor.initSDK(this, null);
        LanSongFileUtil.setFileDir("/sdcard/sel d'alkinylocomplexe/" + System.currentTimeMillis() + "/");
        LibyuvUtil.loadLibrary();

        initUI();
        initMediaRecorder();

        //必须等待文件创建好之后做操作，不然获取不到文件的路径this path is no found!!!
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //todo
                startRecord();
            }
        }, 100);
    }

    /**
     * 初始化UI
     */
    private void initUI() {

        surfaceView = findViewById(R.id.surfaceView);
        lineProgressView = findViewById(R.id.lineProgressView);
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                int width = surfaceView.getWidth();
                int height = surfaceView.getHeight();
                float viewRatio = width * 1f / height;
                float videoRatio = 9f / 16f;
                ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
                if (viewRatio > videoRatio) {
                    layoutParams.height = (int) (width / viewRatio);
                } else {
                    layoutParams.width = (int) (height * viewRatio);
                }
                surfaceView.setLayoutParams(layoutParams);
            }
        });
    }

    private void initMediaRecorder() {
        mCameraHelp.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mOnPreviewFrameListener != null) {
                    mOnPreviewFrameListener.onPreviewFrame(data);
                }
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolder = holder;
                mCameraHelp.openCamera(mContext, Camera.CameraInfo.CAMERA_FACING_FRONT, mSurfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraHelp.release();
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraHelp.callFocusMode();
            }
        });

        mVideoEditor.setOnProgessListener(new onVideoEditorProgressListener() {
            @Override
            public void onProgress(VideoEditor v, int percent) {
                if (percent == 100) {
                    executeProgress++;
                }
                int pro = (int) (executeProgress / executeCount * 100);
                editorTextView.setText("视频编辑中" + pro + "%");
            }
        });
    }

    /**
     * 合成视频
     * TODO 根据自己需求是否需要合成音视频
     */
    public void finishVideo() {
        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<String>() {
            @Override
            public String doInBackground() throws Exception {
                //h264转ts
                ArrayList<String> tsList = new ArrayList<>();
                for (int x = 0; x < segmentList.size(); x++) {
                    String tsPath = LanSongFileUtil.DEFAULT_DIR + System.currentTimeMillis() + ".ts";
                    mVideoEditor.h264ToTs(segmentList.get(x), tsPath);
                    tsList.add(tsPath);
                }
                String s = syntPcm();
                //合成音频
                String aacPath = mVideoEditor.executePcmEncodeAac(s, RecordUtil.sampleRateInHz, RecordUtil.channelCount);
                //合成视频
                String mp4Path = mVideoEditor.executeConvertTsToMp4(tsList.toArray(new String[]{}));
                //音视频混合
                mp4Path = mVideoEditor.executeVideoMergeAudio(mp4Path, aacPath);
                return mp4Path;
            }

            @Override
            public void onFinish(String result) {
                closeProgressDialog();

                Toast.makeText(RecordedActivity.this,"保存成功",Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(mContext, EditVideoActivity.class);
//                intent.putExtra(INTENT_PATH, result);
//                startActivityForResult(intent, REQUEST_CODE_KEY);
        }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                closeProgressDialog();
                Toast.makeText(getApplicationContext(), "视频编辑失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 获取pcm地址
     *
     * @return 地址
     * @throws Exception
     */
    private String syntPcm() throws Exception {

        String pcmPath = LanSongFileUtil.DEFAULT_DIR + System.currentTimeMillis() + ".pcm";
        File file = new File(pcmPath);
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        for (int x = 0; x < aacList.size(); x++) {
            FileInputStream in = new FileInputStream(aacList.get(x));
            byte[] buf = new byte[4096];
            int len = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                out.flush();
            }
            in.close();
        }
        out.close();
        return pcmPath;
    }


    private long videoDuration;
    private long recordTime;
    private String videoPath;

    /**
     * 开始录制
     */
    private void startRecord() {

        RxJavaUtil.run(new RxJavaUtil.OnRxAndroidListener<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {
                videoPath = LanSongFileUtil.DEFAULT_DIR + System.currentTimeMillis() + ".h264";
                audioPath = LanSongFileUtil.DEFAULT_DIR + System.currentTimeMillis() + ".pcm";
                final boolean isFrontCamera = mCameraHelp.getCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT;
                final int rotation;
                if (isFrontCamera) {
                    rotation = 270;
                } else {
                    rotation = 90;
                }
                recordUtil = new RecordUtil(videoPath, audioPath, mCameraHelp.getWidth(), mCameraHelp.getHeight(), rotation, isFrontCamera);
                return true;
            }

            @Override
            public void onFinish(Boolean result) {
                if (lineProgressView.getProgress() < 1.0) {
                    mOnPreviewFrameListener = recordUtil.start();
                    videoDuration = 0;
                    lineProgressView.setSplit();
                    recordTime = System.currentTimeMillis();
                    runLoopPro();
                } else {//通过进度条来判断是否完成录制
                    recordUtil.release();
                    recordUtil = null;
                }

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    /**
     * 录制完成操作
     */
    private void runLoopPro() {

        RxJavaUtil.loop(20, new RxJavaUtil.OnRxLoopListener() {
            @Override
            public Boolean takeWhile() {
                return recordUtil != null && recordUtil.isRecording();
            }

            @Override
            public void onExecute() {
                long currentTime = System.currentTimeMillis();
                videoDuration += currentTime - recordTime;
                recordTime = currentTime;
                long countTime = videoDuration;
                for (long time : timeList) {
                    countTime += time;
                }
                if (countTime <= MAX_VIDEO_TIME) {
                    lineProgressView.setProgress(countTime / MAX_VIDEO_TIME);
                } else {
                    upEvent();
                }
            }

            @Override
            public void onFinish() {
                segmentList.add(videoPath);
                aacList.add(audioPath);
                timeList.add(videoDuration);

                //录制完成直接编辑
                editorTextView = showProgressDialog();
//                executeCount = segmentList.size() + 4;
                //TODO 根据自己操作来实现是否合成音视频
                finishVideo();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                lineProgressView.removeSplit();
            }
        });
    }

    private void upEvent() {
        if (recordUtil != null) {
            recordUtil.stop();
            recordUtil = null;
        }
    }


    /**
     * 清除录制信息
     */
    private void cleanRecord() {

        lineProgressView.cleanSplit();
        segmentList.clear();
        aacList.clear();
        timeList.clear();

        executeCount = 0;
        executeProgress = 0;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cleanRecord();
        if (mCameraHelp != null) {
            mCameraHelp.release();
        }
        if (recordUtil != null) {
            recordUtil.stop();
        }
    }

    /**
     * 把视频路径回传
     * @param requestCode 请求code
     * @param resultCode 结果code
     * @param data 数据
     */
   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_KEY) {
                Intent intent = new Intent();
                intent.putExtra(INTENT_PATH, data.getStringExtra(INTENT_PATH));
                intent.putExtra(INTENT_DATA_TYPE, RESULT_TYPE_VIDEO);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else {
            cleanRecord();
        }
    }*/
}
