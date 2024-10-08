package com.example.intentdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class IntentActivity extends Activity {
	private static final String TAG = "CameraActivity";
	private static final int TAKE_PICTURE = 1;
	private static final int ID_DO_EXPLICIT_BARCODE_ZXING = 2;
	private static final int ID_DO_IMPLICIT_BARCODE = 3;
	private Uri outputFileUri;
	private ImageView image;
	private String pathAndFile;

	private Integer myInteger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		image = (ImageView) findViewById(R.id.imageView1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_camera, menu);
		return true;
	}
	/////////////////////////////////////////////////////////////
	//all the button handlers
	public void doExplicitLaunch(View v) {
		Intent myIntent = new Intent(this,SumActivity.class);
		startActivity(myIntent);
	}

	public void doExplicitLaunchWithData(View v) {
		Bundle sumInputs = new Bundle();
		sumInputs.putDouble("input1", 3);
		sumInputs.putDouble("input2", 4);
		Intent activityIntent =
				new Intent(this, SumActivity.class);
		activityIntent.putExtras(sumInputs);
		startActivity(activityIntent);
	}

	public void doExplicitBarcode(View view) {
		//note you have to install the zxing barcode scanner on your device
		//to have the following not error out, or...

		Intent myIntent = new Intent("com.google.zxing.client.android.SCAN");
		myIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");

		//Check before you willy nilly launch something that may not be there
		if (isAvailable(this, myIntent))
			startActivityForResult(myIntent, ID_DO_EXPLICIT_BARCODE_ZXING);
		else {
			Log.d(TAG, "doExplicitBarcode: zxing aint there ");
			Toast.makeText(IntentActivity.this, "zxing aint there", Toast.LENGTH_SHORT).show();
		}
	}

	public void doLaunchCommsApp(View v) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_EMAIL, "kperkins@cnu.edu");
		intent.putExtra(Intent.EXTRA_SUBJECT, "My Subject");
		intent.putExtra(Intent.EXTRA_TEXT, "I am an email body.");
		startActivity(Intent.createChooser(intent, "Send Email"));
	}

	public void doLaunchCustomAction(View v) {

		Intent myIntent = new Intent("com.example.custom_intent.YOUR_ACTION");
		if (myIntent.resolveActivity(getPackageManager()) == null) {
			// Error occurred while creating the File
			Toast.makeText(this, "INSTALL 4_CustomIntent_and_BogusEmail_App FIRST", Toast.LENGTH_SHORT).show();
			return;
		}
		startActivity(myIntent);
	}

	//*****where photo is stored
	String mCurrentPhotoPath;
	private File createImageFile() {

		try {

			// get external directories that the media scanner scans
			File[] storageDir = getExternalMediaDirs();

			//create a file
			File imagefile = new File(storageDir[0], "img2.png");

			//make sure directory is there, (it should be)
			if (!storageDir[0].exists()) {
				if (!storageDir[0].mkdirs()) {
					Log.e(TAG, "Failed to create file in: " + storageDir[0]);
					return null;
				}
			}

			//make file where image will be stored
			//returns true if successfully created, false if already there
			imagefile.createNewFile();

			// Save a file: path for use with ACTION_VIEW intents
			mCurrentPhotoPath = imagefile.getAbsolutePath();
			return imagefile;

		} catch (IOException ex) {
			// Error occurred while creating the File
			Toast.makeText(this, "Horrible biz here, IOException occurred", Toast.LENGTH_SHORT).show();
			return null;
		}
	}
	////////////////////////////////////////////////////
	// note , camera image capture comes from the following
	// https://developer.android.com/training/camera/photobasics.html#TaskPath
	////////////////////////////////////////////////////
	public void dolaunchCameraApp(View v) {
		// create intent to take picture with camera and specify storage
		// location so we can easily get it
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		if (intent.resolveActivity(getPackageManager()) != null){
			// Create the File where the photo should go
			File photoFile = createImageFile();

			// Continue only if the File was successfully created
			//  see https://developer.android.com/reference/androidx/core/content/FileProvider
			if (photoFile != null) {
				Uri photoURI = FileProvider.getUriForFile(this,
						"com.example.intentdemo.fileprovider",
						photoFile);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
				startActivityForResult(intent, TAKE_PICTURE);
			}
		}
		else
			Toast.makeText(this,"UhOhhhh....No camera mate", Toast.LENGTH_SHORT).show();

	}
	/////////////////////////////////////////////////////////////
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (TAKE_PICTURE):
                takepicture(resultCode);

                //tell scanner to pic up this image
				scanSavedMediaFile(mCurrentPhotoPath);

                break;
            case(ID_DO_EXPLICIT_BARCODE_ZXING):
			case (ID_DO_IMPLICIT_BARCODE ):
				doBarcodeResults(resultCode,data);
                break;
        }
	}

	//see if app that intent is interested in is installed
	//see http://www.grokkingandroid.com/checking-intent-availability/
	public static boolean isAvailable(Context ctx, Intent intent) {
		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list =
				mgr.queryIntentActivities(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

    private void doBarcodeResults(int resultCode,Intent intent) {
        if (resultCode == RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
            Log.d(TAG, "In onActivityResult for ID_DO_EXPLICIT_BARCODE");
            Log.d(TAG, "contents="+ contents);
            Log.d(TAG, "format="+ format);

            //lets take a look at what it means
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(contents));
            startActivity(i);
        } else if (resultCode == RESULT_CANCELED) {
            // Handle problems
            Toast.makeText(this, "Problems mate, Result Canceled", Toast.LENGTH_SHORT).show();

        }
    }

	private void setPic() {
		// Get the dimensions of the View
		int targetW = image.getWidth();
		int targetH = image.getHeight();

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;

		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		image.setImageBitmap(bitmap);
	}
    private void takepicture(int resultCode) {
        if (resultCode == RESULT_OK) {
			setPic();
        }
    }

	/**
	 * Notifies the OS to index the new image, so it shows up in Gallery.
	 * see https://www.programcreek.com/java-api-examples/index.php?api=android.media.MediaScannerConnection
	 */
	public void scanSavedMediaFile( final String path) {
		// silly array hack so closure can reference scannerConnection[0] before it's created
		final MediaScannerConnection[] scannerConnection = new MediaScannerConnection[1];
		try {
			MediaScannerConnection.MediaScannerConnectionClient scannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
				public void onMediaScannerConnected() {
					scannerConnection[0].scanFile(path, null);
				}

				@Override
				public void onScanCompleted(String path, Uri uri) {

				}

			};
			scannerConnection[0] = new MediaScannerConnection(this, scannerClient);
			scannerConnection[0].connect();
		} catch (Exception ignored) {
		}
	}
}

