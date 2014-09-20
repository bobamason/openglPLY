package com.bobamason.openglply;
import android.opengl.*;
import android.util.*;

public class Vector3 {
	public float x, y, z;

	private float[] vIn = new float[4];

	private float[] vOut = new float[4];

	public Vector3() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3(Vector3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public Vector3(float[] v) {
		if (v.length >= 3) {
			this.x = v[0];
			this.y = v[1];
			this.z = v[2];
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 0;
		}
	}

	public Vector3 add(Vector3 v) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;
		return this;
	}
	
	public Vector3 add(float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public Vector3 subtract(Vector3 v) {
		this.x -= v.x;
		this.y -= v.y;
		this.z -= v.z;
		return this;
	}

	public static Vector3 subtract(Vector3 v1, Vector3 v2) {
		return new Vector3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
	}

	public Vector3 multMat4(float[] mat) {
		if (mat.length != 16)throw new IllegalArgumentException("must be 4x4 matrix");
		prepareArray();
		Matrix.multiplyMV(vOut, 0, mat, 0, vIn, 0);
		setValues();
		return this;
	}

	public Vector3 multC(float c) {
		this.x *= c;
		this.y *= c;
		this.z *= c;
		return this;
	}

	public Vector3 div(float c) {
		if (c != 0) {
			this.x /= c;
			this.y /= c;
			this.z /= c;
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 0;
		}
		return this;
	}
	
	public Vector3 normalize(){
		this.div(this.mag());
		return this;
	}
	
	public float mag(){
		return FloatMath.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
	}
	
	public float dot(Vector3 v){
		return this.x * v.x + this.y * v.y + this.z * v.z;
	}

	public static float dot(Vector3 v1, Vector3 v2){
		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}
	
	public static float angle(Vector3 v1, Vector3 v2){
		return (float)Math.acos(Vector3.dot(v1, v2) / v1.mag() * v2.mag());
	}
	
	public static Vector3 cross(Vector3 v1, Vector3 v2){
		return new Vector3(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x);
	}
	
	private void toArray(float[] a){
		if(a.length == 3){
			a[0] = this.x;
			a[1] = this.y;
			a[2] = this.z;
		}else if(a.length == 4){
			a[0] = this.x;
			a[1] = this.y;
			a[2] = this.z;
			a[3] = 1f;
		}else{
			throw new IllegalArgumentException();
		}
	}
	
	public Vector3 set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public Vector3 set(Vector3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
		return this;
	}

	private void prepareArray() {
		vIn[0] = this.x;
		vIn[1] = this.y;
		vIn[2] = this.z;
		vIn[3] = 1f;
	}

	private void setValues() {
		this.x = vOut[0];
		this.y = vOut[1];
		this.z = vOut[2];
	}
}
