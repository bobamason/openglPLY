package com.bobamason.openglply;

import android.opengl.*;

import java.nio.*;
import java.util.*;
import java.io.*;
import android.content.*;
import android.os.*;
import android.util.*;

public class PLYModel {

	private final String vertexShaderVertexColor = "uniform mat4 u_MVPMatrix;      \n"
			+ "uniform mat4 u_MVMatrix;       \n"

			+ "attribute vec4 a_Position;     \n"
			+ "attribute vec3 a_Normal;       \n"
			+ "attribute vec3 a_Color;       \n"

			+ "varying vec3 v_Position;       \n"
			+ "varying vec3 v_Color;          \n"
			+ "varying vec3 v_Normal;         \n"
			+ "void main()                                                \n"
			+ "{                                                          \n"

			+ "   v_Position = vec3(u_MVMatrix * a_Position);             \n"
			+ "   v_Color = a_Color;             \n"

			+ "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));      \n"
			+ "   gl_Position = u_MVPMatrix * a_Position;                 \n"
			+ "}                                                          \n";

	private final String fragmentShaderVertexColor = "precision mediump float;       \n"

			+ "uniform vec3 u_LightPos;       \n"
			+ "uniform float u_LightStrength;       \n"
			+ "varying vec3 v_Position;		\n"
			+ "varying vec3 v_Color;          \n"
			+ "varying vec3 v_Normal;         \n"

			+ "void main()                    \n"
			+ "{                              \n"
			+ "   float distance = length(u_LightPos - v_Position) / u_LightStrength;                   \n"
			+ "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"

			+ "   vec3 normal = v_Normal / length(v_Normal);             \n"

			+ "   float diffuse = max(dot(normal, lightVector), 0.1);              \n"
			+ "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));    \n"
			+ "   gl_FragColor = vec4(v_Color, 1.0) * diffuse * 0.95 + vec4(v_Color, 1.0) * 0.05;                                  \n"
			+ "}";

	private final String vertexShaderTexture = "uniform mat4 u_MVPMatrix;      \n"
			+ "uniform mat4 u_MVMatrix;       \n"

			+ "attribute vec4 a_Position;     \n"
			+ "attribute vec3 a_Normal;       \n"
			+ "attribute vec2 a_TexCoordinate;  \n"

			+ "varying vec3 v_Position;       \n"
			+ "varying vec3 v_Normal;         \n"
			+ "varying vec2 v_TexCoordinate;    \n"

			+ "void main()                                                \n"
			+ "{                                                          \n"

			+ "   v_Position = vec3(u_MVMatrix * a_Position);             \n"
			+ "   v_TexCoordinate = a_TexCoordinate; 					   \n"
			+ "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));      \n"
			+ "   gl_Position = u_MVPMatrix * a_Position;                 \n"
			+ "}                                                          \n";

	private final String fragmentShaderTexture = "precision mediump float;       \n"

			+ "uniform vec3 u_LightPos;       \n"
			+ "uniform float u_LightStrength;       \n"
			+ "uniform sampler2D u_Texture;   \n"

			+ "varying vec3 v_Position;		\n"
			+ "varying vec3 v_Normal;         \n"
			+ "varying vec2 v_TexCoordinate;    \n"

			+ "void main()                    \n"
			+ "{                              \n"
			+ "   float distance = length(u_LightPos - v_Position) / u_LightStrength;                   \n"
			+ "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"

			+ "   vec3 normal = v_Normal / length(v_Normal);             \n"

			+ "   float diffuse = max(dot(normal, lightVector), 0.1);              \n"
			+ "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));    \n"
			+ "   gl_FragColor = diffuse * texture2D(u_Texture, v_TexCoordinate);                                  \n"
			+ "}";

	private FloatBuffer vertexBuffer;

	private ShortBuffer indicesBuffer;

	static final int positionDataSize = 3;

	static final int normalDataSize = 3;

	static final int textureDataSize = 2;

	static final int colorDataSize = 3;

	int vertexStride;

	private int mProgram;

	private int mPositionHandle;

	private int mColorHandle;

	private final int mNormalOffset = 3;

	private final int mColorOffset = 6;

	private final int mTextureOffset = 6;

	private int mMVPMatrixHandle;

	private boolean loaded = false;

	private float[] vertices;

	private Context context;

	private float[] minVals = { 0f, 0f, 0f };

	private float[] maxVals = { 0f, 0f, 0f };

	private int mMVMatrixHandle;

	private int mLightPosHandle;

	private int mNormalHandle;

	private float[] mvpMatrix = new float[16];

	private int mLightStrengthHandle;

	private String filename;

	private PLYModel.LoadStatusListener mLoadStatusListener;

	private float[] projectionMatrix = new float[16];

	private float[] mvMatrix = new float[16];

	private float[] modelMatrix = new float[16];

	private float lightStrength = 1f;

	private Vector3 currentTrans = new Vector3();

	private short[] indices;

	private boolean hasTexture;

	private int mTextureUniformHandle;

	private int mTextureCoordinateHandle;

	private int mTextureDataHandle;

	public PLYModel(Context ctx, String filename, LoadStatusListener listener) {

		int vertexShader = GLRenderer.loadGLShader(GLES20.GL_VERTEX_SHADER,
				vertexShaderVertexColor);
		int fragmentShader = GLRenderer.loadGLShader(GLES20.GL_FRAGMENT_SHADER,
				fragmentShaderVertexColor);

		GLRenderer.checkGLError("PLYModel load shaders");

		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragmentShader);

		GLES20.glLinkProgram(mProgram);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		context = ctx;
		hasTexture = false;
		this.filename = filename;
		mLoadStatusListener = listener;
		new LoadModelTask().execute(filename);
		setIdentity();
	}

	public PLYModel(Context ctx, String filename, int texID,
			LoadStatusListener listener) {

		int vertexShader = GLRenderer.loadGLShader(GLES20.GL_VERTEX_SHADER,
				vertexShaderTexture);
		int fragmentShader = GLRenderer.loadGLShader(GLES20.GL_FRAGMENT_SHADER,
				fragmentShaderTexture);

		GLRenderer.checkGLError("PLYModel load shaders");

		mTextureDataHandle = GLRenderer.loadTexture(ctx, texID);

		GLRenderer.checkGLError("PLYModel load texture");

		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragmentShader);

		GLES20.glLinkProgram(mProgram);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		context = ctx;
		hasTexture = true;
		this.filename = filename;
		mLoadStatusListener = listener;
		new LoadModelTask().execute(filename);
		setIdentity();
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setProjectionMatrix(float[] pMatrix) {
		projectionMatrix = pMatrix;
	}

	public void setProgram(int p) {
		mProgram = p;
	}

	public void draw(float[] viewMatrix, float[] lightPos) {
		if (!loaded)
			return;

		GLES20.glUseProgram(mProgram);
		GLRenderer.checkGLError("PLYModel use program");

		if (hasTexture) {
			mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram,
					"u_Texture");
			GLRenderer.checkGLError("PLYModel mTextureUniformHandle");
			mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram,
					"a_TexCoordinate");
			GLRenderer.checkGLError("PLYModel mTextureCoordinateHandle");
			mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram,
					"u_MVPMatrix");
			GLRenderer.checkGLError("PLYModel mMVPMatrixHandle");
			mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram,
					"u_MVMatrix");
			GLRenderer.checkGLError("PLYModel mMVMatrixHandle");
			mLightPosHandle = GLES20.glGetUniformLocation(mProgram,
					"u_LightPos");
			GLRenderer.checkGLError("PLYModel link mLightPosHandle");
			mPositionHandle = GLES20
					.glGetAttribLocation(mProgram, "a_Position");
			GLRenderer.checkGLError("PLYModel mPositionHandle");
			mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
			GLRenderer.checkGLError("PLYModel mNormalHandle");
			mLightStrengthHandle = GLES20.glGetUniformLocation(mProgram,
					"u_LightStrength");
			GLRenderer.checkGLError("PLYModel mLightStrengthHandle");

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

			GLES20.glUniform1i(mTextureUniformHandle, 0);

			vertexBuffer.position(0);
			GLES20.glVertexAttribPointer(mPositionHandle, positionDataSize,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLES20.glEnableVertexAttribArray(mPositionHandle);

			vertexBuffer.position(mNormalOffset);
			GLES20.glVertexAttribPointer(mNormalHandle, normalDataSize,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLES20.glEnableVertexAttribArray(mNormalHandle);

			vertexBuffer.position(mTextureOffset);
			GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
					textureDataSize, GLES20.GL_FLOAT, false, vertexStride,
					vertexBuffer);
			GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

			GLES20.glUniform3f(mLightPosHandle, lightPos[0], lightPos[1],
					lightPos[2]);
			GLES20.glUniform1f(mLightStrengthHandle, lightStrength);
			Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
			GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
					GLES20.GL_UNSIGNED_SHORT, indicesBuffer);

			GLES20.glDisableVertexAttribArray(mPositionHandle);
			GLES20.glDisableVertexAttribArray(mNormalHandle);
			GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
		} else {
			mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram,
					"u_MVPMatrix");
			GLRenderer.checkGLError("PLYModel mMVPMatrixHandle");
			mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram,
					"u_MVMatrix");
			GLRenderer.checkGLError("PLYModel mMVMatrixHandle");
			mLightPosHandle = GLES20.glGetUniformLocation(mProgram,
					"u_LightPos");
			GLRenderer.checkGLError("PLYModel mLightStrengthHandle");
			mPositionHandle = GLES20
					.glGetAttribLocation(mProgram, "a_Position");
			GLRenderer.checkGLError("PLYModel mPositionHandle");
			mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
			GLRenderer.checkGLError("PLYModel mColorHandle");
			mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
			GLRenderer.checkGLError("PLYModel mNormalHandle");
			mLightStrengthHandle = GLES20.glGetUniformLocation(mProgram,
					"u_LightStrength");
			GLRenderer.checkGLError("PLYModel mLightStrengthHandle");

			vertexBuffer.position(0);
			GLES20.glVertexAttribPointer(mPositionHandle, positionDataSize,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLES20.glEnableVertexAttribArray(mPositionHandle);

			vertexBuffer.position(mNormalOffset);
			GLES20.glVertexAttribPointer(mNormalHandle, normalDataSize,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLES20.glEnableVertexAttribArray(mNormalHandle);

			vertexBuffer.position(mColorOffset);
			GLES20.glVertexAttribPointer(mColorHandle, colorDataSize,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLES20.glEnableVertexAttribArray(mColorHandle);

			GLES20.glUniform3f(mLightPosHandle, lightPos[0], lightPos[1],
					lightPos[2]);
			GLES20.glUniform1f(mLightStrengthHandle, lightStrength);
			Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
			GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
			GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
					GLES20.GL_UNSIGNED_SHORT, indicesBuffer);

			GLES20.glDisableVertexAttribArray(mPositionHandle);
			GLES20.glDisableVertexAttribArray(mNormalHandle);
			GLES20.glDisableVertexAttribArray(mColorHandle);
		}

		GLRenderer.checkGLError("Draw PLYModel");
	}

	private boolean loadModel(String filename) {
		InputStream stream = null;
		BufferedReader reader = null;
		int vCount = 0;
		int fCount = 0;
		int i, j = 0;
		boolean isOk = false;
		ArrayList<String> header = new ArrayList<String>();

		try {
			stream = context.getAssets().open(filename);
			reader = new BufferedReader(new InputStreamReader(stream));

			String line = reader.readLine();

			while (line != null && !line.contains("end_header")) {
				header.add(line);
				line = reader.readLine();
			}

			for (i = 0; i < header.size(); i++) {
				line = header.get(i);
				if (line.contains("element vertex")) {
					int p = line.lastIndexOf(" ") + 1;
					try {
						vCount = Integer.parseInt(line.substring(p));
						Log.d("PLYModel", "vertex count from file: " + vCount);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}

				if (line.contains("element face")) {
					int p = line.lastIndexOf(" ") + 1;
					try {
						fCount = Integer.parseInt(line.substring(p));
						Log.d("PLYModel", "face count from file: " + fCount);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}

			if (hasTexture) {
				vertexStride = 8 * 4;
				vertices = new float[vCount * 8];
				for (i = 0; i < vCount; i++) {
					line = reader.readLine();
					if (line != null) {
						String[] split = line.split(" ");
						for (j = 0; j < split.length; j++) {
							try {
								vertices[i * 8 + j] = Float
										.parseFloat(split[j]);
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} else {
				vertexStride = 9 * 4;
				vertices = new float[vCount * 9];
				for (i = 0; i < vCount; i++) {
					line = reader.readLine();
					if (line != null) {
						String[] split = line.split(" ");
						for (j = 0; j < split.length; j++) {
							try {
								if (j >= 6) {
									vertices[i * 9 + j] = Float
											.parseFloat(split[j]) / 255f;
								} else {
									vertices[i * 9 + j] = Float
											.parseFloat(split[j]);
								}
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			indices = new short[fCount * 3];

			for (i = 0; i < fCount; i++) {
				line = reader.readLine();
				if (line != null) {
					String[] split = line.split(" ");
					for (j = 1; j < split.length; j++) {
						try {
							indices[i * 3 + (j - 1)] = Short
									.parseShort(split[j]);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}

			if (stream != null) {
				stream.close();
			}

			isOk = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return isOk;
	}

	public float getWidth() {
		if (loaded)
			return maxVals[0] - minVals[0];
		else
			return 0;
	}

	public float getHeight() {
		if (loaded)
			return maxVals[1] - minVals[1];
		else
			return 0;
	}

	public float getDepth() {
		if (loaded)
			return maxVals[2] - minVals[2];
		else
			return 0;
	}

	public float getLargestDimen() {
		if (getWidth() > getHeight()) {
			if (getWidth() > getDepth())
				return getWidth();
			else
				return getDepth();
		} else {
			if (getHeight() > getDepth())
				return getHeight();
			else
				return getDepth();
		}
	}

	public void setLightStrength(float strength) {
		lightStrength = strength;
	}

	public void setIdentity() {
		currentTrans.set(0f, 0f, 0f);
		Matrix.setIdentityM(modelMatrix, 0);
	}

	public void translate(float x, float y, float z) {
		currentTrans.add(x, y, z);
		Matrix.translateM(modelMatrix, 0, x, y, z);
	}

	public void translate(Vector3 v) {
		currentTrans.add(v);
		Matrix.translateM(modelMatrix, 0, v.x, v.y, v.z);
	}

	public void rotateEuler(float z, float x, float y) {
		Matrix.rotateM(modelMatrix, 0, z, 0f, 0f, 1f);
		Matrix.rotateM(modelMatrix, 0, x, 1f, 0f, 0f);
		Matrix.rotateM(modelMatrix, 0, y, 0f, 1f, 0f);
	}

	public void rotateAxis(float a, float x, float y, float z) {
		Matrix.rotateM(modelMatrix, 0, a, x, y, z);
	}

	public void scale(float s) {
		Matrix.scaleM(modelMatrix, 0, s, s, s);
	}

	public void scale(float sx, float sy, float sz) {
		Matrix.scaleM(modelMatrix, 0, sx, sy, sz);
	}

	public void getCenter(float[] vec4) {
		if (vec4.length != 4)
			throw new IllegalArgumentException("array must have lenght of 3");
		if (loaded) {
			vec4[0] = (maxVals[0] + minVals[0]) / 2f;
			vec4[1] = (maxVals[1] + minVals[1]) / 2f;
			vec4[2] = (maxVals[2] + minVals[2]) / 2f;
			vec4[3] = 1f;
		} else {
			vec4[0] = 0f;
			vec4[1] = 0f;
			vec4[2] = 0f;
			vec4[3] = 1f;
		}
	}

	public Vector3 getCenterVec() {
		if (loaded) {
			Vector3 v = new Vector3((maxVals[0] + minVals[0]) / 2f,
					(maxVals[1] + minVals[1]) / 2f,
					(maxVals[2] + minVals[2]) / 2f);
			v.add(currentTrans);
			return v;
		} else
			return new Vector3();
	}

	private class LoadModelTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			if (mLoadStatusListener != null)
				mLoadStatusListener.started();
		}

		@Override
		protected Boolean doInBackground(String... args) {
			boolean b = loadModel(args[0]);
			return b;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!result)
				throw new RuntimeException("ply model failed to load");

			ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
			bb.order(ByteOrder.nativeOrder());

			vertexBuffer = bb.asFloatBuffer();
			vertexBuffer.put(vertices);
			vertexBuffer.position(0);

			ByteBuffer ib = ByteBuffer.allocateDirect(indices.length * 2);
			ib.order(ByteOrder.nativeOrder());

			indicesBuffer = ib.asShortBuffer();
			indicesBuffer.put(indices);
			indicesBuffer.position(0);

			loaded = result;
			Log.d("PLYModel", "loaded = " + String.valueOf(loaded) + " "
					+ filename);

			if (mLoadStatusListener != null)
				mLoadStatusListener.completed();
			System.gc();
		}
	}

	public void setLoadStatusListener(LoadStatusListener listener) {
		mLoadStatusListener = listener;
	}

	public static abstract class LoadStatusListener {
		public abstract void started();

		public abstract void completed();
	}
}
