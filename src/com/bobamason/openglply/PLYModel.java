package com.bobamason.openglply;

import android.opengl.*;
import java.nio.*;
import java.util.*;
import java.io.*;
import android.content.*;
import android.os.*;
import android.util.*;

public class PLYModel {
	private FloatBuffer vertexBuffer;
	
	private ShortBuffer indicesBuffer;

	static final int positionDataSize = 3;

	static final int normalDataSize = 3;
	
	static final int colorDataSize = 3;

	int vertexStride = 9 * 4;

	private int mProgram;

	private int mPositionHandle;

	private int mColorHandle;

	private final int mNormalOffset = 3;
	
	private final int mColorOffset = 6;

	private int mMVPMatrixHandle;

	private boolean loaded = false;

	private float[] vertices;

	private Context context;

	private float[] minVals = {0f, 0f, 0f};

	private float[] maxVals = {0f, 0f, 0f};

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

	public PLYModel(Context ctx, String filename, int program, LoadStatusListener listener) {

		context = ctx;
		this.filename = filename;
		mProgram = program;
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

		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix"); 
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal"); 
        mLightStrengthHandle = GLES20.glGetUniformLocation(mProgram, "u_LightStrength");


		vertexBuffer.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, positionDataSize, 
									 GLES20.GL_FLOAT, false, 
									 vertexStride, vertexBuffer);
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		vertexBuffer.position(mNormalOffset);
		GLES20.glVertexAttribPointer(mNormalHandle, normalDataSize, 
									 GLES20.GL_FLOAT, false, 
									 vertexStride, vertexBuffer);
		GLES20.glEnableVertexAttribArray(mNormalHandle);

		vertexBuffer.position(mColorOffset);
		GLES20.glVertexAttribPointer(mColorHandle, colorDataSize, 
									 GLES20.GL_FLOAT, false, 
									 vertexStride, vertexBuffer);
		GLES20.glEnableVertexAttribArray(mColorHandle);

		GLES20.glUniform3f(mLightPosHandle, lightPos[0], lightPos[1], lightPos[2]);
		GLES20.glUniform1f(mLightStrengthHandle, lightStrength);

		Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);

		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);

		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
		
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);
		
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mNormalHandle);
		GLES20.glDisableVertexAttribArray(mColorHandle);
	}

	private boolean loadModel(String filename) {
		InputStream stream = null;
		BufferedReader reader = null;
		int vCount = 0;
		int fCount = 0;
		int i, j = 0;
		boolean isOk = false;
		ArrayList<String> header = new ArrayList<String>();

		try
		{
			stream = context.getAssets().open(filename);
			reader = new BufferedReader(new InputStreamReader(stream));

			String line = reader.readLine();

			while (line != null && !line.contains("end_header"))
			{
				header.add(line);
				line = reader.readLine();
			}

			for (i = 0; i < header.size(); i++)
			{
				line = header.get(i);
				if (line.contains("element vertex"))
				{
					int p = line.lastIndexOf(" ") + 1;
					try
					{
						vCount = Integer.parseInt(line.substring(p));
						vertices = new float[vCount * 9];
						Log.d("PLYModel" , "vertex count from file: " + vCount);
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}

				if (line.contains("element face"))
				{
					int p = line.lastIndexOf(" ") + 1;
					try
					{
						fCount = Integer.parseInt(line.substring(p));
						indices = new short[fCount * 3];
						Log.d("PLYModel" , "face count from file: " + fCount);
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}

			for(i = 0; i < vCount; i++){
				line = reader.readLine();
				if(line != null){
					String[] split = line.split(" ");
					for(j = 0; j < split.length; j ++){
						try{
							if(j >= 6){
								vertices[i * 9 + j] = Float.parseFloat(split[j])/255f;
							}
							else{
								vertices[i * 9 + j] = Float.parseFloat(split[j]);
							}
						}catch(NumberFormatException e){
							e.printStackTrace();
						}
					}
				}
			}

			for(i = 0; i < fCount; i++){
				line = reader.readLine();
				if(line != null){
					String[] split = line.split(" ");
					for(j = 1; j < split.length; j ++){
						try{
							indices[i * 3 + (j - 1)] = Short.parseShort(split[j]);
						}catch(NumberFormatException e){
							e.printStackTrace();
						}
					}
				}
			}

			if (stream != null)
			{
				stream.close();
			}

			isOk = true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return isOk;
	}

	public float getWidth() {
		if (loaded)
			return maxVals[0] - minVals[0];
		else return 0;
	}

	public float getHeight() {
		if (loaded)
			return maxVals[1] - minVals[1];
		else return 0;
	}

	public float getDepth() {
		if (loaded)
			return maxVals[2] - minVals[2];
		else return 0;
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
		currentTrans.set(0f,0f,0f);
		Matrix.setIdentityM(modelMatrix, 0);
	}

	public void translate(float x, float y, float z) {
		currentTrans.add(x,y,z);
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
		if (vec4.length != 4) throw new IllegalArgumentException("array must have lenght of 3");
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
		if (loaded){
			Vector3 v = new Vector3((maxVals[0] + minVals[0]) / 2f,
								(maxVals[1] + minVals[1]) / 2f,
								(maxVals[2] + minVals[2]) / 2f);
			v.add(currentTrans);
			return v;
		}else
			return new Vector3();
	}

	private class LoadModelTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			if (mLoadStatusListener != null) mLoadStatusListener.started();
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
			Log.d("PLYModel", "loaded = " + String.valueOf(loaded) + " " + filename);

			if (mLoadStatusListener != null) mLoadStatusListener.completed();
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
