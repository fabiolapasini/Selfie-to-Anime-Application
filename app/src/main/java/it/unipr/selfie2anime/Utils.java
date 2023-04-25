package it.unipr.selfie2anime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class Utils extends AppCompatActivity {

    // get the assets path
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }


    public static double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
        int height = srcImage.height();
        int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            double heightRatio = (double) reqHeight / (double) height;
            double widthRatio = (double) reqWidth / (double) width;
            inSampleSize = heightRatio<widthRatio ? heightRatio :widthRatio;
        }
        return inSampleSize;
    }


    // get the bitmap from the arraylist
    public static Bitmap arrayFlotToBitmap(List<Float> floatArray, int width, int height){
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int pixels[] = new int[width * height * 4];

        float Maximum = Collections.max(floatArray);
        float minmum = Collections.min(floatArray);
        float delta = Maximum - minmum ;

        for (int i=0; i<width*height; i++){
            int r = ((int) ((((floatArray.get(i) - minmum) / delta) * 255)));
            int g = ((int) ((((floatArray.get(i + width*height) - minmum) / delta) * 255)));
            int b = ((int) ((((floatArray.get(i + 2 * width*height) - minmum) / delta) * 255)));
            pixels[i] = Color.argb(255, r, g, b);

        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return bmp ;
    }
}