package com.edotassi.barcode;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

public class MainActivity extends Activity {

	private Camera mCamera;
	private CameraPreview mPreview;
	private Handler autoFocusHandler;

	private ArrayList<String> eans = new ArrayList<String>();
	private ArrayAdapter<String> adapter;

	private ToneGenerator tg;
	
	TextView scanText;
	Button scanButton;

	ImageScanner scanner;

	private boolean barcodeScanned = false;
	private boolean previewing = true;

	static {
		System.loadLibrary("iconv");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.i("com.edotassi.barcode", "activity created");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		autoFocusHandler = new Handler();
		mCamera = getCameraInstance();

		/* Instance barcode scanner */
		scanner = new ImageScanner();
		scanner.setConfig(0, Config.X_DENSITY, 3);
		scanner.setConfig(0, Config.Y_DENSITY, 3);

		mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);

		scanText = (TextView) findViewById(R.id.scanText);

		scanButton = (Button) findViewById(R.id.ScanButton);

		scanButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (barcodeScanned) {
					barcodeScanned = false;
					scanText.setText("Scanning...");
					mCamera.setPreviewCallback(previewCb);
					mCamera.startPreview();
					previewing = true;
					mCamera.autoFocus(autoFocusCB);
				}
			}
		});

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, eans);
		ListView lv = (ListView) findViewById(R.id.listView1);
		lv.setAdapter(adapter);
		
		tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
		
	}

	@Override
	protected void onPause() {
		Log.i("com.edotassi.barcode", "activity paused");
		super.onPause();
		releaseCamera();
	}

	@Override
	protected void onDestroy() {
		Log.i("com.edotassi.barcode", "activity destoied");
		super.onDestroy();
		releaseCamera();
	}

	@Override
	protected void onResume() {
		Log.i("com.edotassi.barcode", "activity resumed");
		super.onResume();
		if (mCamera == null) {
			mCamera = getCameraInstance();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
			Camera.Parameters p = c.getParameters();
			List<Size> sizes = p.getSupportedPictureSizes();
			int w = 0, h = 0;
			for(Size sz : sizes) {
				if (sz.width > w) w = sz.width;
				if (sz.height > h) h = sz.height;
			}
			p.setPictureSize(w, h);
			c.setParameters(p);
		} catch (Exception e) {
			Log.e("com.edotassi.barcode",
					"eccezione camera.open() => " + e.getMessage());
		}
		return c;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			previewing = false;
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	private Runnable doAutoFocus = new Runnable() {
		public void run() {
			if (previewing)
				mCamera.autoFocus(autoFocusCB);
		}
	};

	PreviewCallback previewCb = new PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
			Camera.Parameters parameters = camera.getParameters();
			Size size = parameters.getPreviewSize();

			Image barcode = new Image(size.width, size.height, "Y800");
			barcode.setData(data);

			int result = scanner.scanImage(barcode);

			if (result != 0) {
				// previewing = false;
				// mCamera.setPreviewCallback(null);
				// mCamera.stopPreview();

				SymbolSet syms = scanner.getResults();
				for (Symbol sym : syms) {
					// scanText.setText("barcode result " + sym.getData());
					String ean = sym.getData();
					if (!(eans.contains(ean))) {
						eans.add(sym.getData());
						tg.startTone(ToneGenerator.TONE_PROP_BEEP);
					}
					// barcodeScanned = true;
				}
				adapter.notifyDataSetChanged();
			}
		}
	};

	// Mimic continuous auto-focusing
	AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			autoFocusHandler.postDelayed(doAutoFocus, 1000);
		}
	};
}
