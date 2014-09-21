package com.bobamason.openglply;

import android.opengl.*;
import android.util.*;
import java.nio.*;

public class LoadingAnimation {
	Triangle triangle;

	private float[] projection = new float[16];

	private float[] viewMatrix = new float[16];

	private float[] modelMatrix = new float[16];

	private float[] mvMatrix = new float[16];

	private float[] mvpMatrix = new float[16];

	private float s = 0.3f;

	private float deg2rad = (float) Math.PI / 180f;

	private static final int numTriangles = 8;

	private static final float angleStep = 360f / numTriangles;

	private float x;

	private float y;

	private float mScale;

	private int num = 0;

	private long lastMillis, currentMillis;

	private static final long duration = 200;

	private float[] c1 = { 0.7f, 0.7f, 0.7f, 1f };

	private float[] c2 = { 1f, 1f, 1f, 1f };

	private float[] color = new float[4];

	public LoadingAnimation() {
		triangle = new Triangle();
		currentMillis = lastMillis = System.currentTimeMillis();
	}

	public void setColor1(float[] c1) {
		this.c1 = c1;
	}

	public void setColor2(float[] c2) {
		this.c2 = c2;
	}

	public void setProjection(float[] projection) {
		this.projection = projection;
	}

	public void draw() {
		Matrix.setLookAtM(viewMatrix, 0, 0.0f, 0.0f, -1.0f, 0f, 0f, 0f, 0f, 1f,
				0f);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		currentMillis = System.currentTimeMillis();

		for (int i = 0; i < numTriangles; i++) {
			x = (float) Math.cos(i * angleStep * deg2rad) * 0.4f;
			y = (float) Math.sin(i * angleStep * deg2rad) * 0.4f;
			Matrix.setIdentityM(modelMatrix, 0);
			Matrix.translateM(modelMatrix, 0, x, y, 1f);
			Matrix.rotateM(modelMatrix, 0, i * angleStep, 0f, 0f, 1f);

			if (currentMillis - lastMillis < duration) {
				if (i == num) {
					mScale = 1f + (0.8f / duration)
							* (int) (currentMillis - lastMillis);
					// Matrix.rotateM(modelMatrix, 0, mScale, 1f, -1f, 0f);
					Matrix.scaleM(modelMatrix, 0, mScale, mScale, mScale);
					color = c2;
				} else {
					color = c1;
				}
			} else {
				color = c1;
				num++;
				num %= numTriangles;
				lastMillis = currentMillis;
			}

			Matrix.scaleM(modelMatrix, 0, s, s, s);
			Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0, projection, 0, mvMatrix, 0);
			triangle.draw(mvpMatrix, color);
		}

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	}

	private class Triangle {
		private FloatBuffer vertexBuffer;

		private final String vertexShaderCode = "uniform mat4 uMVPMatrix;"
				+ "attribute vec4 vPosition;" + "void main(){"
				+ "gl_Position = uMVPMatrix * vPosition;" + "}";

		private final String fragmentShaderCode = "precision mediump float;"
				+ "uniform vec4 vColor;" + "void main(){"
				+ "gl_FragColor = vColor;" + "}";

		static final int C0ORDS_PER_VERTEX = 3;

		final float[] triangleCoords = { -0.5f, 0.5f, 0.0f, // top
				0.5f, -0.5f, 0.0f, // bottom right
				0.5f, 0.5f, 0.0f // bottom left
		};

		int vertexCount = triangleCoords.length / C0ORDS_PER_VERTEX;

		int vertexStride = C0ORDS_PER_VERTEX * 4;

		private int mProgram;

		private int mPositionHandle;

		private int mColorHandle;

		private int mMVPMatrixHandle;

		public Triangle() {
			ByteBuffer bb = ByteBuffer
					.allocateDirect(triangleCoords.length * 4);
			bb.order(ByteOrder.nativeOrder());

			vertexBuffer = bb.asFloatBuffer();
			vertexBuffer.put(triangleCoords);
			vertexBuffer.position(0);

			Log.d("triangle buffer", vertexBuffer.capacity() + "");
			Log.d("triangle stride", vertexStride + "");

			int vertexShader = GLRenderer.loadGLShader(GLES20.GL_VERTEX_SHADER,
					vertexShaderCode);
			int fragmentShader = GLRenderer.loadGLShader(
					GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

			mProgram = GLES20.glCreateProgram();
			GLES20.glAttachShader(mProgram, vertexShader);
			GLES20.glAttachShader(mProgram, fragmentShader);
			GLES20.glLinkProgram(mProgram);
		}

		public void draw(float[] mvpMatrix, float[] color) {
			GLES20.glUseProgram(mProgram);

			mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

			GLES20.glEnableVertexAttribArray(mPositionHandle);

			GLES20.glVertexAttribPointer(mPositionHandle, C0ORDS_PER_VERTEX,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

			mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

			GLES20.glUniform4fv(mColorHandle, 1, color, 0);

			mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram,
					"uMVPMatrix");

			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

			GLES20.glDisableVertexAttribArray(mPositionHandle);
		}
	}
}
