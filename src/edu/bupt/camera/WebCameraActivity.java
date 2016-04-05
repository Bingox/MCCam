package edu.bupt.camera;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import edu.bupt.mccam.R;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

public class WebCameraActivity extends Activity{
		SurfaceView sView;
		SurfaceHolder surfaceHolder;
		Camera camera; // ����ϵͳ���õ������
		boolean isPreview = false; // �Ƿ��������
		private String ipname;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// ����ȫ��
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.webcamera);

			// ��ȡIP��ַ
			ipname = "10.105.39.42";
			sView = (SurfaceView) findViewById(R.id.sView); // ��ȡ������SurfaceView���
			surfaceHolder = sView.getHolder(); // ���SurfaceView��SurfaceHolder

			// ΪsurfaceHolder���һ���ص�������
			surfaceHolder.addCallback(new Callback() {
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				}

				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					initCamera(); // ������ͷ
				}

				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					// ���camera��Ϊnull ,�ͷ�����ͷ
					if (camera != null) {
						if (isPreview)
							camera.stopPreview();
					}
				}
			});
			// ���ø�SurfaceView�Լ���ά������
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		}

		private void initCamera() {
			if (!isPreview) {
				camera = Camera.open();
			}
			if (camera != null && !isPreview) {
				try {
					camera.setPreviewDisplay(surfaceHolder); // ͨ��SurfaceView��ʾȡ������
					setCameraDisplayOrientation(WebCameraActivity.this, 0, camera);
					camera.setPreviewCallback(new StreamIt(ipname)); // ���ûص�����
					camera.startPreview(); // ��ʼԤ��
					camera.autoFocus(null); // �Զ��Խ�
				} catch (Exception e) {
					e.printStackTrace();
				}
				isPreview = true;
			}
	}

		public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			int degrees = 0;
			switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
			}
			int rotationDegrees;
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				rotationDegrees = (info.orientation + degrees) % 360;
				rotationDegrees = (360 - rotationDegrees) % 360; // compensate the mirror
			} else {
				rotationDegrees = (info.orientation - degrees + 360) % 360;
			}
			camera.setDisplayOrientation(rotationDegrees);
		}

	class StreamIt implements Camera.PreviewCallback {
		private String ipname;

		public StreamIt(String ipname) {
			this.ipname = ipname;
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			Size size = camera.getParameters().getPreviewSize();
			try {
				// ����image.compressToJpeg������YUV��ʽͼ������dataתΪjpg��ʽ
				YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
				Log.d("size", "width: " + size.width + " height: " + size.height);
				if (image != null) {
					ByteArrayOutputStream outstream = new ByteArrayOutputStream();
//					image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, outstream);
					image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, outstream);
					outstream.flush();
					// �����߳̽�ͼ�����ݷ��ͳ�ȥ
					Thread th = new MyThread(outstream, ipname);
					th.start();
				}
			} catch (Exception ex) {
				Log.e("Sys", "Error:" + ex.getMessage());
			}
		}
	}

	class MyThread extends Thread {
		private byte byteBuffer[] = new byte[1024];
		private OutputStream outsocket;
		private ByteArrayOutputStream myoutputstream;
		private String ipname;

		public MyThread(ByteArrayOutputStream myoutputstream, String ipname) {
			this.myoutputstream = myoutputstream;
			this.ipname = ipname;
			try {
				myoutputstream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				// ��ͼ������ͨ��Socket���ͳ�ȥ
				Socket tempSocket = new Socket(ipname, 6000);
				outsocket = tempSocket.getOutputStream();
				ByteArrayInputStream inputstream = new ByteArrayInputStream(myoutputstream.toByteArray());
				int amount;
				while ((amount = inputstream.read(byteBuffer)) != -1) {
					outsocket.write(byteBuffer, 0, amount);
				}
				myoutputstream.flush();
				myoutputstream.close();
				tempSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	  }
	
		@Override
		protected void onDestroy() {
			super.onDestroy();
			camera.release();
			camera = null;
		}
}
