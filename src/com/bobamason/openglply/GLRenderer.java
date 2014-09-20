package com.bobamason.openglply;
import android.content.*;
import android.graphics.*;
import android.opengl.*;
import android.util.*;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.*;

import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;

public class GLRenderer implements GLSurfaceView.Renderer
{

	private final String vertexShaderCode = 
	"uniform mat4 u_MVPMatrix;      \n"		
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


	private final String fragmentShaderCode = 
	"precision mediump float;       \n"		

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

	private float[] mProjectionMatrix = new float[16];

	private float[] mViewMatrix = new float[16];

	private float[] mvMatrix = new float[16];

	private Context context;

	private float[] mRotationMatrix = new float[16];

	private float[] mLightModelMatrix = new float[16];

	private float[] mLightPos = {1f, 0f, -1f, 1f};

	private float[] mLightPosInModelSpace = new float[4];

	private float[] mLightPosInEyeSpace = new float[4];

	private int mProgram;

	private int startedCount = 0;

	private int completedCount = 0;

	private PLYModel cube;

	private LoadingAnimation anim;

	private boolean allLoaded = false;

	private float angle = 0;

	private PLYModel planet;

	public void setContext(Context context)
	{
		this.context = context;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config)
	{
		GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1f);

		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		if (vertexShader == 0)
			throw new RuntimeException("error creating vertex shader");
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		if (fragmentShader == 0)
			throw new RuntimeException("error creating fragment shader");


		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragmentShader);

		GLES20.glLinkProgram(mProgram);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		//contruct model StlModel(GLRender, Context ctx, String filename, int program)
		//and set lightStrength
		anim = new LoadingAnimation();

		cube = new PLYModel(context, "cube.ply", mProgram, mLoadStatusListener);
		planet = new PLYModel(context, "planet1.ply", mProgram, mLoadStatusListener);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		float ratio = (float) width / height;
		GLES20.glViewport(0, 0, width, height);
		float zoom = 2.0f;
		Matrix.frustumM(mProjectionMatrix, 0, -ratio / zoom, ratio / zoom, -1 / zoom, 1 / zoom, 1 , 100);
		//set STLModel projectionMatrix
		anim.setProjection(mProjectionMatrix);
		cube.setProjectionMatrix(mProjectionMatrix);
		cube.setLightStrength(4);
		planet.setProjectionMatrix(mProjectionMatrix);
		planet.setLightStrength(4);
	}

	@Override
	public void onDrawFrame(GL10 unused)
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (allLoaded)
		{
			Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -1.0f, 0f, 0f, 1f, 0f, 1f, 0f);
			
			Matrix.setIdentityM(mLightModelMatrix, 0);
			Matrix.multiplyMV(mLightPosInModelSpace, 0, mLightModelMatrix, 0, mLightPos, 0);
			Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInModelSpace, 0);

			//model transformations
			//STLModel.draw(float[] viewMatrix, float[] lightPos, float[] color)
			cube.setIdentity();
			cube.translate(0f,0.4f,1.7f);
			cube.rotateEuler(0f, angle * 0.4f, angle);
			cube.scale(0.4f);
			cube.draw(mViewMatrix, mLightPosInEyeSpace);
			
			planet.setIdentity();
			planet.translate(0f,-0.5f,1.2f);
			planet.rotateEuler(0f, 0f, angle);
			planet.scale(0.4f);
			planet.draw(mViewMatrix, mLightPosInEyeSpace);
			
			angle += 0.8f;
		}
		else
		{
			anim.draw();
		}
	}

	public static int loadShader(int type, String shaderCode)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);
		return shader;
	}
	
	public static int loadTexture(final Context context, final int resourceId)
	{
		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);

		if (textureHandle[0] != 0)
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;   // No pre-scaling

			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}

		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error loading texture.");
		}

		return textureHandle[0];
	}

	private PLYModel.LoadStatusListener mLoadStatusListener = new PLYModel.LoadStatusListener(){
		@Override
		public void started()
		{
			if (allLoaded)allLoaded = false;
			startedCount++;
			Log.d("started ", startedCount + " started");
		}

		@Override
		public void completed()
		{
			completedCount++;
			if (completedCount == startedCount)
			{
				allLoaded = true;
			}
			Log.d("completed ", completedCount + " completed");
		}
	};
}
