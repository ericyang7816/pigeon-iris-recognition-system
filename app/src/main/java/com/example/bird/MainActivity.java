package com.example.bird;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import Jama.*;

class Bird implements Comparable<Bird>{
    int id;
    String category;
    Float[] feature;
    float similarity = 0;

    public Bird(int id, String category, Float[] feature) {
        this.id = id;
        this.category = category;
        this.feature = feature;
    }

    public Bird(int id, String category, Float[] feature, float sim) {
        this.id = id;
        this.category = category;
        this.feature = feature;
        this.similarity = sim;
    }

    public Float[] getFeature(){
        return this.feature;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    @Override
    public int compareTo(Bird bird) {
        if(this.similarity> bird.similarity){
            return -1;
        }else{
            return 1;
        }
    }

};



public class MainActivity extends AppCompatActivity implements Runnable {
    private int mImageIndex = 0;
    private String[] mTestImages = {"test1.png", "2473.jpg", "test3.png"};

    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    ArrayList<Bird> birds = new ArrayList<Bird>();
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private Module mModule2 = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

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

    //读取已有的向量数据
    //outputs2,ArrayList<Bird> birds
//        int id;
//        String category;
//        ArrayList<Float> feature;
    public static float cosineSimilarity(Float[] array1,float[] array2){
        float xx = 0;
        float yy = 0;
        float xy = 0;
        for (int i=0;i<array1.length;i++){
            xx+= array1[i]*array1[i];
            yy+= array2[i]*array2[i];
            xy+= array1[i]*array2[i];
        }
        return (float) (xy/(Math.sqrt(xx)*Math.sqrt(yy)));
    }

    @Override
    protected void  onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);

        //在本Activity中添加和确认访问权限，如需要
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        //加载布局
        setContentView(R.layout.activity_main);
        //获取assets中的图片
        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex+1]));
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);

        //按钮：孪生比较分析
        mButtonDetect = findViewById(R.id.detectButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButtonDetect.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonDetect.setText(getString(R.string.run_model));
                //为了应用模型，图片需要缩放的比例
                mImgScaleX = (float)mBitmap.getWidth() / PrePostProcessor.mInputWidth;
                mImgScaleY = (float)mBitmap.getHeight() / PrePostProcessor.mInputHeight;
                //图片在ImageView上缩放的比例
                mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageView.getWidth() / mBitmap.getWidth() : (float)mImageView.getHeight() / mBitmap.getHeight());
                mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageView.getHeight() / mBitmap.getHeight() : (float)mImageView.getWidth() / mBitmap.getWidth());
                //图片在ImageView上左上角的坐标
                mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
                mStartY = (mImageView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;

                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });

        try {
            //加载模型
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "best.torchscript.pt"));
            mModule2 = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "model_80_6000_feat10.ptl"));
            //加载标签
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes2.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
            //加载图片样本数据,结果为ArrayList<Bird> birds
            BufferedReader br2 = new BufferedReader(new InputStreamReader(getAssets().open("classes2.txt")));
            String line2;
            String readJson = "";
            while ((line2 = br2.readLine()) != null) {
                readJson += line2;
            }
            JSONObject jsonObject = JSON.parseObject(readJson);
            JSONArray birdsJson =  jsonObject.getJSONArray("birds");
            for(int i =0;i<birdsJson.size();i++){
                JSONObject key = (JSONObject)birdsJson.get(i);
                int key_id = key.getIntValue("id");
                String key_category = key.getString("category");
                JSONArray key_array = key.getJSONArray("feature");
                Float[] key_feature = key_array.toArray(new Float[key_array.size()]);
                Bird tempBird = new Bird(key_id ,key_category, key_feature);
                birds.add(tempBird);
            }

        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
    }



    @Override
    public void run() {
        //
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);
        //提取图片（一张为例）
        Result oneResult = results.get(0);
        int left1 = oneResult.rect.left;
        int top1 = oneResult.rect.top;
        int right1 = oneResult.rect.right;
        int bottom1 = oneResult.rect.bottom;
        float left2 = (left1-mStartX)/mIvScaleX;
        float top2 = (top1-mStartY)/mIvScaleY;
        float right2 = (right1-mStartX)/mIvScaleX;
        float bottom2 = (bottom1-mStartY)/mIvScaleY;
        float x = left2;
        float y = top2;
        float w = right2-left2;
        float h = bottom2-top2;
        int x_int = (int)x;
        //获得眼睛图片newBitmap,并进行缩放成要求比例
        Bitmap newBitmap = Bitmap.createBitmap(mBitmap,(int)x,(int)y,(int)w,(int)h);
        Bitmap resizedBitmap2 = Bitmap.createScaledBitmap(newBitmap, 100, 100, true);
        final Tensor inputTensor2 = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap2, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple2 = mModule.forward(IValue.from(inputTensor2)).toTuple();
        final Tensor outputTensor2 = outputTuple2[0].toTensor();
        final float[] outputs2 = outputTensor2.getDataAsFloatArray();

        //读取已有的向量数据
        //outputs2,ArrayList<Bird> birds
//        int id;
//        String category;
//        ArrayList<Float> feature;
        //计算余弦并排序
        ArrayList<Bird> used_birds = birds;
        for(Bird abird : used_birds){
            abird.getFeature();
            float tempCos = cosineSimilarity(abird.getFeature(), outputs2);
            abird.setSimilarity(tempCos);
        }
        Collections.sort(used_birds);
        //选择前几的图片和相似度

        //
        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mImageView.setImageBitmap(newBitmap);
//            mButtonDetect.setEnabled(true);
//            mButtonDetect.setText(getString(R.string.detect));
//            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
//            mResultView.setResults(results);
//            mResultView.invalidate();
//            mResultView.setVisibility(View.VISIBLE);
        });
    }


}

//Bitmap.createBitmap方法中的五个参数意义分别为：需要切割的图片资源、切割起始点的X坐标、切割起始点的Y坐标、切割多宽、切割多高。