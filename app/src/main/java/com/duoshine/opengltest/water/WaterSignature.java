package com.duoshine.opengltest.water;

import android.opengl.GLES20;
import android.opengl.Matrix;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class WaterSignature {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public static final int SIZE_OF_FLOAT = 4;
    /**
     * 一个“完整”的正方形，从两维延伸到-1到1。
     * 当 模型/视图/投影矩阵是都为单位矩阵的时候，这将完全覆盖视口。
     * 纹理坐标相对于矩形是y反的。
     * (This seems to work out right with external textures from SurfaceTexture.)
     */
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 1.0f,     //0 bottom left     //0.0f, 0.0f, // 0 bottom left
            1.0f, 1.0f,     //1 bottom right    //1.0f, 0.0f, // 1 bottom right
            0.0f, 0.0f,     //2 top left        //0.0f, 1.0f, // 2 top left
            1.0f, 0.0f,     //3 top right       //1.0f, 1.0f, // 3 top right
    };

    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mCoordsPerVertex;
    private int mCoordsPerTexture;
    private int mVertexCount;
    private int mVertexStride;
    private int mTexCoordStride;
    private int hProgram;

    public float[] mProjectionMatrix = new float[16];// 投影矩阵
    public float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    public float[] mModelMatrix = new float[16];// 模型变换矩阵
    public float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵

    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    public WaterSignature() {
        mVertexArray = createFloatBuffer(FULL_RECTANGLE_COORDS);
        mTexCoordArray = createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
        mCoordsPerVertex = 2;
        mCoordsPerTexture = 2;
        mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex; // 4
        mTexCoordStride = 2 * SIZE_OF_FLOAT;
        mVertexStride = 2 * SIZE_OF_FLOAT;

        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);

        hProgram = crateProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        GLES20.glUseProgram(hProgram);
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

    private FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZE_OF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    private WaterSignSProgram mProgram;

    public void setShaderProgram(WaterSignSProgram mProgram) {
        this.mProgram = mProgram;
    }

    public void drawFrame(int mTextureId) {
        GLES20.glUseProgram(hProgram);
        // 设置纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(mProgram.sTextureLoc, 0);
//        GlUtil.checkGlError("GL_TEXTURE_2D sTexture");
        // 设置 model / view / projection 矩阵
        GLES20.glUniformMatrix4fv(mProgram.uMVPMatrixLoc, 1, false, getFinalMatrix(), 0);
//        GlUtil.checkGlError("glUniformMatrix4fv uMVPMatrixLoc");
        // 使用简单的VAO 设置顶点坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glVertexAttribPointer(mProgram.aPositionLoc, mCoordsPerVertex,
                GLES20.GL_FLOAT, false, mVertexStride, mVertexArray);
//        GlUtil.checkGlError("VAO aPositionLoc");
        // 使用简单的VAO 设置纹理坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glVertexAttribPointer(mProgram.aTextureCoordLoc, mCoordsPerTexture,
                GLES20.GL_FLOAT, false, mTexCoordStride, mTexCoordArray);
//        GlUtil.checkGlError("VAO aTextureCoordLoc");
        // GL_TRIANGLE_STRIP三角形带，这就为啥只需要指出4个坐标点，就能画出两个三角形了。
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);
        // Done -- 解绑~
        GLES20.glDisableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glDisableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    /**
     * terminatinng, this should be called in GL context
     */
    public void release() {
        if (hProgram >= 0)
            GLES20.glDeleteProgram(hProgram);
        hProgram = -1;
    }

    /**
     * 删除texture
     */
    public static void deleteTex(final int hTex) {
//        LogUtil.v("WaterSignature", "deleteTex:");
        final int[] tex = new int[]{hTex};
        GLES20.glDeleteTextures(1, tex, 0);
    }

}

