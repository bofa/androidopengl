/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.openglrts;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";
    private SpaceShip mSpaceShip;
    private Tree mTree;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mVPMatrix = new float[16];

    // Declare as volatile because we are updating it from another thread
    public volatile float mAngle;
    public volatile float mCameraRotationX;
    public volatile float mCameraRotationY;

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
        mSpaceShip = new SpaceShip();
        mTree = new Tree();
        
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
    	GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); 
    	
        // Set the camera position (View matrix)
        float dist = 20;

        Matrix.setLookAtM(mVMatrix, 0,
        		(float) (dist*Math.cos(mCameraRotationY)), 8,(float) (dist*Math.sin(mCameraRotationY)), 
        		0f, 0f, 0f,
        		0f, 4.0f, 0.0f	);
        

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
        mTree.draw(mMVPMatrix);

        Matrix.setIdentityM(mTranslationMatrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, 0, 0, 4);
		Matrix.multiplyMM(mVPMatrix, 0, mVMatrix, 0, mTranslationMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVPMatrix, 0);

        mSpaceShip.draw(mMVPMatrix);
        
        Matrix.setIdentityM(mTranslationMatrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, 0, 0, -4);
		Matrix.multiplyMM(mVPMatrix, 0, mVMatrix, 0, mTranslationMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVPMatrix, 0);
        
        mSpaceShip.draw(mMVPMatrix);
        
        
        
        // Draw square
//        mSquare.draw(mMVPMatrix);
//
//        // Create a rotation for the triangle
////        long time = SystemClock.uptimeMillis() % 4000L;
////        float angle = 0.090f * ((int) time);
//        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
//
//        // Combine the rotation matrix with the projection and camera view
//        Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, mMVPMatrix, 0);
//
//        // Draw triangle
//        mTriangle.draw(mMVPMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjMatrix, 0, -ratio*0.5f, ratio*0.5f, -0.5f, 0.5f, 1, 100);
//        Matrix.frustumM(mProjMatrix, 0, -0.5f, 0.5f, -0.5f, 0.5f, 1, 100);
        
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}

class SpaceShip {

    private final String vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" +
        "varying vec4 pos;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  pos = vPosition;" +
        "  gl_Position = uMVPMatrix*vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "varying vec4 pos;" +
        "void main() {" +
        "  gl_FragColor = vec4(vec3(pos),1);" +
        "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    static float spaceCoords[] = { 2.024855f, -0.010977f, 0.000000f,
							      	-3.000248f, 0.014253f, -0.000000f,
								    1.071316f, 0.028810f, -1.000000f,
								    1.071316f, 0.028810f, 1.000000f,
								    0.593598f, 1.469822f, -0.021358f };
        
    private final short drawOrder[] = { 0, 4, 3,
    							       	4, 2, 1,
    							       	0, 2, 4,
    							       	0, 3, 2,
    							       	3, 2, 1,
    							       	3, 4, 1	};
	   
	   
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    public SpaceShip() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        		spaceCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(spaceCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}

class Tree {

    private final String vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" +
        "varying vec4 pos;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  pos = vPosition;" +
        "  gl_Position = uMVPMatrix*vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "varying vec4 pos;" +
        "void main() {" +
        "  gl_FragColor = vec4(vec3(pos),1);" +
        "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    static float spaceCoords[] = { 0.0f, 0.0f, 0.0f,
    	1.000000f, -0.020508f, -1.000000f,
    	1.000000f, -0.020508f, 1.000000f,
    	-1.000000f, -0.020508f, 1.000000f,
    	-1.000000f, -0.020508f, -1.000000f,
    	2.640002f, 7.024941f, -2.639999f,
    	2.639999f, 7.024941f, 2.640002f,
    	-2.640001f, 7.024941f, 2.639999f,
    	-2.640000f, 7.024941f, -2.640001f,
    	1.000000f, 5.834182f, -1.000000f,
    	0.999999f, 5.834182f, 1.000000f,
    	-1.000000f, 5.834182f, 1.000000f,
    	-1.000000f, 5.834182f, -1.000000f,
    	-2.640000f, 7.024941f, -2.640001f,
    	-2.640001f, 7.024941f, 2.639999f,
    	2.639999f, 7.024941f, 2.640002f,
    	2.640002f, 7.024941f, -2.639999f,
    	-2.640000f, 4.992438f, -2.640001f,
    	-2.640001f, 4.992438f, 2.639999f,
    	2.639999f, 4.992438f, 2.640001f,
    	2.640002f, 4.992438f, -2.639999f,
    	1.056001f, 8.024941f, -1.055999f,
    	1.056000f, 8.024941f, 1.056001f,
    	-1.056000f, 8.024941f, 1.056000f,
    	-1.056000f, 8.024941f, -1.056000f };
        
    private final short drawOrder[] = { 1, 2, 3,
    		8, 7, 24,
    		7, 8, 13,
    		10, 9, 19,
    		8, 5, 16,
    		9, 1, 4,
    		1, 9, 10,
    		2, 10, 3,
    		3, 11, 4,
    		9, 12, 20,
    		20, 16, 15,
    		11, 10, 19,
    		12, 11, 18,
    		16, 20, 17,
    		18, 14, 17,
    		6, 7, 14,
    		19, 15, 18,
    		5, 6, 15,
    		21, 24, 23,
    		7, 6, 23,
    		6, 5, 21,
    		5, 8, 24,
    		4, 1, 3,
    		7, 23, 24,
    		14, 7, 13,
    		9, 20, 19,
    		13, 8, 16,
    		12, 9, 4,
    		2, 1, 10,
    		10, 11, 3,
    		11, 12, 4,
    		12, 17, 20,
    		19, 20, 15,
    		18, 11, 19,
    		17, 12, 18,
    		13, 16, 17,
    		14, 13, 17,
    		15, 6, 14,
    		15, 14, 18,
    		16, 5, 15,
    		22, 21, 23,
    		6, 22, 23,
    		22, 6, 21,
    		21, 5, 24	};
	   
	   
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    public Tree() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        		spaceCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(spaceCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
