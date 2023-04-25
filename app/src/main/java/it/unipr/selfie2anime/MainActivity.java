// FABIOLA PASINI   313585  fabiola.pasini@studenti.unipr.it    PROGETTO CORS0 AMPS 2020/2021

package it.unipr.selfie2anime;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.pytorch.MemoryFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static it.unipr.selfie2anime.Utils.assetFilePath;
import static it.unipr.selfie2anime.Utils.calculateSubSampleSize;
import static it.unipr.selfie2anime.Utils.arrayFlotToBitmap;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnLongClickListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private static final String TAG = "project_app_TAG";
    private boolean isTouch = false;
    private boolean isLongTouched = false;
    private boolean finished = false;
    private Module module = null;
    private static final int SELECT_PICTURE = 1;
    private Bitmap bitmap = null;
    private Bitmap modeledBitmap = null;
    private Object lock = new Object();
    Mat modeledImage = null;
    Mat sampledImage=null;
    Mat originalImage=null;
    Mat mColor;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = findViewById(R.id.HelloVisionView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnLongClickListener((View.OnLongClickListener) this);

        String str3 = "<font face=\"Calibri\" color=\"#FF7A59\"> <big> Switch to convert in anime </big> </ font>";
        final TextView tv = findViewById(R.id.textview);
        tv.setText(Html.fromHtml(str3, 0));

        try {
            // loading image to be animezed
            this.bitmap = BitmapFactory.decodeStream(getAssets().open("meg.jpeg"));
            // resize
            this.bitmap = Bitmap.createScaledBitmap(this.bitmap, 256, 256, false);

            // loading torch module for mobile
            module = LiteModuleLoader.load(assetFilePath(this, "phone_net_G_A.ptl"));
        }
        catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            finish();
        }

        final ImageView iv = findViewById(R.id.image);
        iv.setImageBitmap(this.bitmap);
        modeledBitmap = bitmapToModeledBitmap(bitmap);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(isTouch){
                    isTouch = false;
                    iv.setImageBitmap(modeledBitmap);
                }
                else {
                    isTouch = true;
                    iv.setImageBitmap(bitmap);
                }
            }
        });
    }


    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this, mLoaderCallback);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mColor = new Mat(height, width, CvType.CV_8UC4);
    }


    @Override
    public void onCameraViewStopped() {
        mColor.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mColor = inputFrame.rgba();

        // raddrizzo l'inquadratura della camera
        Mat mColorT = mColor.t();
        Core.flip(mColor.t(), mColorT, -1);
        Imgproc.resize(mColorT, mColorT, mColor.size());
        mColorT.copyTo(mColor);

        if(isLongTouched){
            /*
             * funizona ma non in tempo reale: acquisisce una immagine come bitmat, la mostra e poi e possibile
             * switcharla in anime premendo il pulsante di switch sottostante.
             * */

            /* INIZIO VARIANTE 1
            this.runOnUiThread(new Runnable() {
                public void run() {
                    Log.i(TAG, "mColor: (" + mColor.width() +"," + mColor.height()+")");
                    Mat image = new Mat (mColor.width(),mColor.height(), CvType.CV_8UC4);
                    mColor.copyTo(image);
                    mOpenCvCameraView.disableView();
                    mColor.release();
                    mOpenCvCameraView.setVisibility(SurfaceView.GONE);
                    displayImage(image);
                }
            });
            return mColor;
            /* FINE VARIANTE 1*/


            /*
             * funziona in tempo reale: acquisisce una immagine, se si preme sulla vista della camera a lungo si acquisce l'imamgine,
             * la si elebora e si mostra in output l'anime fino non si preme di nuovo.
             * */

            /* INIZIO VARIANTE 2 */
            this.runOnUiThread( new Runnable() {
                public void run() {
                    bitmap = Bitmap.createBitmap(mColor.cols(), mColor.rows(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(mColor, bitmap);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, false);
                    modeledBitmap = bitmapToModeledBitmap(bitmap);

                    finished = true;
                    synchronized(lock) {
                        lock.notify();
                    }
                }
            });
            
            synchronized(lock) {
                while(!finished){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            finished = false;

            modeledImage = new Mat(mColor.width(), mColor.height(), CvType.CV_8UC4, new Scalar(255, 255, 255, 0));
            Utils.bitmapToMat(this.modeledBitmap, modeledImage);
            Imgproc.resize(modeledImage, modeledImage, new Size(mColor.width(), mColor.height()));

            return modeledImage;
            /* FINE VARIANTE 2 */
        }
        else {
            return mColor;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.decision_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.galleria) {
            findViewById(R.id.image).setVisibility(View.VISIBLE);
            findViewById(R.id.button).setVisibility(View.VISIBLE);
            findViewById(R.id.textview).setVisibility(View.GONE);
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, "Seleziona immagine"), SELECT_PICTURE);
            return true;
        }
        else if (id == R.id.camera) {
            Log.i(TAG, "Using camera!");
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.enableView();
            findViewById(R.id.image).setVisibility(View.GONE);
            findViewById(R.id.textview).setVisibility(View.GONE);

            Toast.makeText(this, "Long click to switch", Toast.LENGTH_SHORT).show();

            /* INIZIO VARIANTE 1*/
            findViewById(R.id.button).setVisibility(View.GONE);     // il bottone per switchare non serve nella variante 2
            /* FINE VARIANTE 1 */
        }
        else if (id == R.id.salvare) {
            saveToGallery(this.modeledBitmap);
            return true;
        }
        return true;
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            String selectedImagePath = getPath(selectedImageUri);
            Log.i(TAG, "selectedImagePath: " + selectedImagePath);
            loadImage(selectedImagePath);
            displayImage(sampledImage);
        }
    }


    public void saveToGallery(Bitmap toBeSaved){
        FileOutputStream outputStream = null;
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/APMS_Pasini");
        if(!dir.exists()){
            Log.i(TAG, dir + " does not exist yet");
            dir.mkdirs();
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String millis = String.valueOf(System.currentTimeMillis());
        String fileName = "modeledImage_" + currentDateandTime+"_"+millis + ".jpeg";

        Log.i(TAG, "Saving image: " + fileName + " in: " + dir);
        Toast.makeText(this, "Saving image: " + fileName + " in: " + dir, Toast.LENGTH_SHORT).show();

        File outFile = new File(dir,fileName);
        try{
            outputStream = new FileOutputStream(outFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        toBeSaved.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        try{
            outputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            outputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    private String getPath(Uri uri) {
        if(uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if(cursor != null ){
            int column_index = cursor.getColumnIndexOrThrow( MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }


    private void loadImage(String path){
        originalImage = Imgcodecs.imread(path);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        sampledImage=new Mat();
        double downSampleRatio= calculateSubSampleSize(rgbImage,width,height);
        Imgproc.resize(rgbImage, sampledImage, new Size(),downSampleRatio,downSampleRatio,Imgproc.INTER_AREA);
    }


    // from image to bitmap and modelBitmap
    private void displayImage(Mat image) {
        //findViewById(R.id.image).setVisibility(View.VISIBLE);
    // Creiamo una Bitmap
        this.bitmap = Bitmap.createBitmap(image.cols(), image.rows(),Bitmap.Config.RGB_565);
    // Convertiamo l'immagine di tipo Mat in una Bitmap
        Utils.matToBitmap(image, this.bitmap);
        this.bitmap = Bitmap.createScaledBitmap(this.bitmap, 256, 256, false);
    // Collego la ImageView e gli assegno la BitMap
        ImageView iv = findViewById(R.id.image);
        iv.setVisibility(View.VISIBLE);
        iv.setImageBitmap(bitmap);

        modeledBitmap = bitmapToModeledBitmap(this.bitmap);
    }


    // make the inference
    public Bitmap bitmapToModeledBitmap(Bitmap in){
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(in, new float[]{0.5f, 0.5f, 0.5f}, new float[]{0.5f, 0.5f, 0.5f}, MemoryFormat.CHANNELS_LAST);
        // running the model
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] array1 = outputTensor.getDataAsFloatArray();
        List<Float> arralist = new ArrayList<Float>();
        for (int i = 0; i < array1.length; i++) {
            arralist.add(array1[i]);
        }
        // get the bitmap from the arraylist
        return arrayFlotToBitmap(arralist, 256, 256);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int X = (int) event.getX();
        int Y = (int) event.getY();
        int eventaction = event.getAction();

        if (eventaction == MotionEvent.ACTION_MOVE) {
            Toast.makeText(this, "TOUCH " + "X: " + X + " Y: " + Y, Toast.LENGTH_SHORT).show();
        }
        return true;
    }


    // se clicco per qualche secondo
    @Override
    public boolean onLongClick(View view) {
        isLongTouched = !isLongTouched;
        return false;
    }

}

