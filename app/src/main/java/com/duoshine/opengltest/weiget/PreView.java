package com.duoshine.opengltest.weiget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.duoshine.opengltest.MyApplication;
import com.duoshine.opengltest.R;
import com.duoshine.opengltest.util.CameraUtil;
import com.duoshine.opengltest.water.WaterSignSProgram;
import com.duoshine.opengltest.water.WaterSignature;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by duo_shine on 2022/3/7
 * openGL初始化，相机输出数据，将数据显示glview
 */
public class PreView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, View.OnTouchListener {
    private static final String TAG = "PreView";
    private SurfaceTexture surfaceTexture;
    private float[] mtx = new float[16];

    //opengl画布宽高
    private int mWidth;
    private int mHeight;

    //这是着色器程序 这两段式glsl代码，是OpenGL用于编写采样渲染的程序语言。
    //顶点着色器代码 顶点着色器获取到外部传入的两个坐标，一个是OpenGL世界坐标，用于确定要绘制的形状，另一个是要采样器用于采样的采样坐标，还有一个矩阵，需要和采样坐标相乘才能获取到surfacetexture正确的采样坐标
    private String vertex = "attribute vec4 vPosition;\n" +
            "    attribute vec4 vCoord;\n" +
            "    uniform mat4 vMatrix;\n" +
            "    varying vec2 aCoord;\n" +
            "    void main(){\n" +
            "        gl_Position = vPosition; \n" +
            "        aCoord = (vMatrix * vCoord).xy;\n" +
            "    }";
    //片元着色器代码 接收到顶点着色器确定的采样坐标，然后使用采样器采样到纹理的像素点颜色值，许许多多像素点的颜色值就构成了图像。
    private String frag = "#extension GL_OES_EGL_image_external:require\n" +
            "    precision mediump float;\n" +
            "    varying vec2 aCoord;\n" +
            "    uniform samplerExternalOES vTexture;\n" +
            "    void main() {\n" +
            "        gl_FragColor = texture2D(vTexture,aCoord);\n" +
            "    }";

    private int[] texture;
    private int mProgramId;
    private int vPosition;
    private int vCoord;
    private int vMatrix;
    private int vTexture;
    private FloatBuffer mGLVertexBuffer;
    private FloatBuffer mGLTextureBuffer;
    private CameraUtil cameraUtil;
    private WaterSignature mWaterSign;
    private int mSignTexId;

    public PreView(Context context) {
        this(context, null);
    }

    public PreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        //相机控制
        cameraUtil = new CameraUtil();

        /**初始化OpenGL的相关信息*/
        setEGLContextClientVersion(2);//设置版本
        setRenderer(this);//设置渲染器
        setRenderMode(RENDERMODE_WHEN_DIRTY);//设置渲染方式按需渲染
        setPreserveEGLContextOnPause(true);//保存Context当pause时
        setCameraDistance(100);//相机距离

        mGLVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBuffer.clear();
        float[] VERTEX = { //OpenGL世界坐标
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };
        mGLVertexBuffer.put(VERTEX);
        mGLTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.clear();
        float[] TEXTURE = {   //目标采样的顶点坐标
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
        };
        mGLTextureBuffer.put(TEXTURE);

        setOnTouchListener(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        initCamera();

        // This renderer required OES_EGL_image_external extension
        final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);    // API >= 8
        // 使用黄色清除界面
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        //设置水印
        if (mWaterSign == null) {
            mWaterSign = new WaterSignature();
        }
        //设置阴影
        mWaterSign.setShaderProgram(new WaterSignSProgram());
        mSignTexId = loadTexture(MyApplication.getContext(), R.mipmap.watermark);
    }

    private int loadTexture(Context context, int resourceId) {
        final int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1, textureObjectIds, 0);
        if (textureObjectIds[0] == 0) {
            Log.e(TAG, "Could not generate a new OpenGL texture object!");
            return 0;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if (bitmap == null) {
            Log.e(TAG, "Resource ID " + resourceId + "could not be decode");
            GLES20.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }
        //告诉OpenGL后面纹理调用应该是应用于哪个纹理对象
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);
        //设置缩小的时候（GL_TEXTURE_MIN_FILTER）使用mipmap三线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        //设置放大的时候（GL_TEXTURE_MAG_FILTER）使用双线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //Android设备y坐标是反向的，正常图显示到设备上是水平颠倒的，解决方案就是设置纹理包装，纹理T坐标（y）设置镜面重复
        //ball读取纹理的时候  t范围坐标取正常值+1
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        //快速生成mipmap贴图
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        //解除纹理操作的绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureObjectIds[0];
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: ");
        mWidth = width;
        mHeight = height;
    }

    /**
     * 输出数据
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame: ");
        //清理屏幕：可以清理成指定的颜色
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        surfaceTexture.updateTexImage();//更新纹理成为最新的数据
        surfaceTexture.getTransformMatrix(mtx);

        GLES20.glViewport(0, 0, mWidth, mHeight);//确定渲染的起始坐标，和宽高

        GLES20.glUseProgram(mProgramId);//使用着色器程序

        mGLVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGLVertexBuffer);//设置顶点数据
        GLES20.glEnableVertexAttribArray(vPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer); //采样目标顶点坐标
        GLES20.glEnableVertexAttribArray(vCoord);

        //变换矩阵
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //激活图层
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);//绑定采样器采样的目标纹理
        GLES20.glUniform1i(vTexture, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4); //开始渲染

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); //解绑纹理

        //添加静态水印
        GLES20.glEnable(GLES20.GL_BLEND);
        //开启GL的混合模式，即图像叠加 这里最重要的是要开启GL的混合模式，不然绘制的水印会覆盖原先的预览画面
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //画水印（非动态） 中间这里是你绘制的预览画面
        GLES20.glViewport(20, 20, 288, 120);
        mWaterSign.drawFrame(mSignTexId);
    }

    private void initCamera() {
        surfaceTexture = new SurfaceTexture(createTextureID());
        surfaceTexture.setOnFrameAvailableListener(this);
        cameraUtil.open(surfaceTexture);
    }

    /**
     * 创建显示的texture
     */
    private int createTextureID() {
        //创建一个纹理ID,这样camera采集到的数据就绑定到这个纹理id上
        texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);

        //创建着色器程序 并且获取着色器程序中的部分属性
        mProgramId = crateProgram(vertex, frag);
        vPosition = GLES20.glGetAttribLocation(mProgramId, "vPosition");
        vCoord = GLES20.glGetAttribLocation(mProgramId, "vCoord");
        vMatrix = GLES20.glGetUniformLocation(mProgramId, "vMatrix");
        vTexture = GLES20.glGetUniformLocation(mProgramId, "vTexture");

        return texture[0];
    }

    //创建着色器程序 返回着色器id
    private int crateProgram(String vsi, String fsi) {
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);//创建一个顶点着色器
        GLES20.glShaderSource(vShader, vsi); //加载顶点着色器代码
        GLES20.glCompileShader(vShader); //编译

        int[] status = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, status, 0);//获取状态
        if (status[0] != GLES20.GL_TRUE) { //判断是否创建成功
            throw new IllegalStateException("顶点着色器创建失败！");
        }

        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);//创建一个顶点着色器
        GLES20.glShaderSource(fShader, fsi);//加载顶点着色器代码
        GLES20.glCompileShader(fShader);
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            throw new IllegalStateException("片元着色器创建失败");
        }

        //创建着色器程序
        int mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);//将着色器塞入程序中
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);//链接
        //获取状态，判断是否成功
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            throw new IllegalStateException("link program:" + GLES20.glGetProgramInfoLog(mProgram));
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);

        return mProgram;
    }

    /**
     * 有新数据
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onFrameAvailable: ");
        //请求渲染器渲染帧，由于是按需渲染，有了新数据再主动要求渲染，就会回调onDrawFrame回调
        this.requestRender();
    }

    public void release() {
        cameraUtil.release();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch: " + event.getAction());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "onTouch: -");
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            cameraUtil.onFocus(new Point(rawX, rawY));
        }
        return false;
    }
}
