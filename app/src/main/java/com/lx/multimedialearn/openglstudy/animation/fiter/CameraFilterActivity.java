package com.lx.multimedialearn.openglstudy.animation.fiter;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;
import com.lx.multimedialearn.utils.ToastUtils;

import java.io.IOException;

/**
 * 相机+滤镜
 * 结合openglstudy-image中对bitmap处理的glsl，增加实时滤镜
 */
public class CameraFilterActivity extends AppCompatActivity {

    private GLSurfaceView mGlSurfaceView;
    private CameraFilterRenderer mRender;
    private SurfaceTexture mSurfaceTexture; //使用SurfaceTexture承载camera回调数据，渲染到Gl上
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private int mTextureID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持gl 2.0！");
        }
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_camera_filter_player);
        mTextureID = GlUtil.createCameraTextureID();
        initCamreaParameters();
        mSurfaceTexture = new SurfaceTexture(mTextureID); //surfacetexture用来承载相机的预览数据，绑定到TextureID上，GLSurfaceView拿到ID进行绘画
        mRender = new CameraFilterRenderer(this, mSurfaceTexture, mTextureID);
        mSurfaceTexture.detachFromGLContext();
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setRenderer(mRender);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camera_filter_activity_menu, menu);
        return true;
    }

    private boolean isHalf = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_image_config:
                isHalf = !isHalf;
                if (isHalf) {
                    item.setTitle("处理一半");
                } else {
                    item.setTitle("全部处理");
                }
                mRender.setIsHalf(isHalf);
                break;
            case R.id.menu_image_origin:
                mRender.setInfo(0, new float[]{0.0f, 0.0f, 0.0f});
                break;
            case R.id.menu_image_gray:
                mRender.setInfo(1, new float[]{0.299f, 0.587f, 0.114f});
                break;
            case R.id.menu_image_cool:
                mRender.setInfo(2, new float[]{0.0f, 0.0f, 0.1f});
                break;
            case R.id.menu_image_warm:
                mRender.setInfo(2, new float[]{0.1f, 0.1f, 0.0f});
                break;
            case R.id.menu_image_blur:
                mRender.setInfo(3, new float[]{0.006f, 0.004f, 0.002f});
                break;
            case R.id.menu_image_magn:
                mRender.setInfo(4, new float[]{0.0f, 0.0f, 0.4f});
                break;
        }
        //设置渲染器
        mGlSurfaceView.requestRender();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGlSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGlSurfaceView.onResume();
    }

    private void initCamreaParameters() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);

        CameraUtils.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                mCamera.addCallbackBuffer(buffers);//这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
            }
        });
    }
}
