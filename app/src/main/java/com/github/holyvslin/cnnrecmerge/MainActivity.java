package com.github.holyvslin.cnnrecmerge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.Color;
import android.net.Uri;
//import android.os.Build;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.AppCompatEditText;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.ActivityCompat;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

//import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.ByteBuffer;

/**
 * Created by holyvslin on 2020/01/01.
 */

public class MainActivity  extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";

    private Button imgChooseButton, imgProcessButton, startRecognitionButton;
    private ImageView originalImageView, midPreProcessImageView, preProcessImageView;

    private ArrayList<Integer> validSubBitmapArrayListIndex;
    private ArrayList<Bitmap> subBitmapArrayList,validSubBitmapArrayList, validSubBitmapArrayListToModel;

    //private ArrayList<Bitmap> subBitmapArray, subBitmapArrayToModel;
    //private ArrayList<Integer> BitmapIndexList;
    private TextView cnnrecTextView, imgprocTextView, originalTextView, preProcessTextView, midPreProcessTextView;
    private boolean pruningSetting = false;
    private int threshold;
    private String model_file;
    private Bitmap originalBitmap, rectBoundingBitmap, scaledBitmap, grayBitmap, gaussFilterBitmap, binariBitmap, expendBitmap, corrodeBitmap, subBitmapToModel;
    private Switch modelPruningSwitch;
    AppCompatEditText etScaleRate, etPaddingRate;
    private int scaleRate;
    private int paddingRate;
    private Thread thread;
    String realPath ;
    Context context = this;

    //private long preTime, postTime, costTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate START");

        super.onCreate(savedInstanceState);
        Log.i(TAG,"before permission:");
        requestReadExternalPermission();
        Log.i(TAG,"after permission:");

        setContentView(R.layout.activity_main);
        //verifyStoragePermissions(this);

        originalImageView = (ImageView) findViewById(R.id.originalImgView);
        preProcessImageView = (ImageView) findViewById(R.id.preProcessImgView);
        midPreProcessImageView = (ImageView) findViewById(R.id.midPreProcessImgView);
        imgChooseButton = (Button) findViewById(R.id.imgChooseButton);
        imgProcessButton = (Button) findViewById(R.id.imgProcessButton);
        startRecognitionButton = (Button) findViewById(R.id.startRecognitionButton);
        //startRecognitionButton.setVisibility(View.GONE);
        cnnrecTextView = (TextView) findViewById(R.id.cnnrecTextView);
        imgprocTextView = (TextView) findViewById(R.id.imgprocTextView);
        originalTextView = (TextView) findViewById(R.id.originalTextView);
        preProcessTextView  = (TextView) findViewById(R.id.preProcessTextView);
        midPreProcessTextView  = (TextView) findViewById(R.id.midPreProcessTextView);
        imgChooseButton.setOnClickListener(this);
        imgProcessButton.setOnClickListener(this);
        startRecognitionButton.setOnClickListener(this);

        etScaleRate = (AppCompatEditText) findViewById(R.id.etScaleRate);
        etPaddingRate = (AppCompatEditText) findViewById(R.id.etPaddingRate);

        modelPruningSwitch = (Switch) findViewById(R.id.modelPruningSwitch);
        modelPruningSwitch.setSwitchTextAppearance(MainActivity.this,R.style.s_false);
        modelPruningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplication(), "打开轻量化模型", Toast.LENGTH_SHORT).show();
                    modelPruningSwitch.setSwitchTextAppearance(MainActivity.this,R.style.s_true);
                    pruningSetting = true;
                    Log.i(TAG,"打开轻量化模型");
                }else{
                    Toast.makeText(getApplication(), "关闭轻量化模型", Toast.LENGTH_SHORT).show();
                    modelPruningSwitch.setSwitchTextAppearance(MainActivity.this,R.style.s_false);
                    pruningSetting = false;
                    Log.i(TAG,"关闭轻量化模型");
                }
            }
        });

        Log.d(TAG, "onCreate END");
    }

    /**
     * onCLick()
     * 点击按钮，选择图片按钮和开始识别按钮
     * @param v
     * @return
     */
    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick START");

        scaleRate = Integer.valueOf(TextUtils.isEmpty(etScaleRate.getText().toString()) ? "10" : etScaleRate.getText().toString());
        Log.i(TAG,"图像压缩比例："+scaleRate+"%");

        paddingRate = Integer.valueOf(TextUtils.isEmpty(etPaddingRate.getText().toString()) ? "20" : etPaddingRate.getText().toString());
        Log.i(TAG,"子图填充比例："+paddingRate+"%");

        if(v == imgChooseButton) {
            // 重新选择图像时，清空所有显示
            originalTextView.setVisibility(View.INVISIBLE);
            originalImageView.setVisibility(View.INVISIBLE);

            midPreProcessTextView.setVisibility(View.INVISIBLE);
            midPreProcessImageView.setVisibility(View.INVISIBLE);

            preProcessTextView.setVisibility(View.INVISIBLE);
            preProcessImageView.setVisibility(View.INVISIBLE);

            cnnrecTextView.setText("");
            imgprocTextView.setText("");
            cnnrecTextView.setVisibility(View.INVISIBLE);
            imgprocTextView.setVisibility(View.INVISIBLE);

            chooseFile();

        } else if(v == imgProcessButton){

            // 重新选择图像时，清空所有显示

            midPreProcessTextView.setVisibility(View.INVISIBLE);
            midPreProcessImageView.setVisibility(View.INVISIBLE);

            preProcessTextView.setVisibility(View.INVISIBLE);
            preProcessImageView.setVisibility(View.INVISIBLE);

            cnnrecTextView.setText("");
            imgprocTextView.setText("");
            cnnrecTextView.setVisibility(View.INVISIBLE);
            imgprocTextView.setVisibility(View.INVISIBLE);

            imageProcess(originalBitmap);

        }else if(v == startRecognitionButton){

            cnnRecognition();
        }
        Log.d(TAG, "onClick END");
    }

    /**
     * chooseFile()
     * 选择图片按钮
     * @param
     * @return
     */
    private void chooseFile() {
        Log.d(TAG, "chooseFile START");

        // 选择文件前，先释放掉Bitmap空间
        Log.d(TAG, "选择文件前，先释放掉Bitmap空间");
        if( subBitmapArrayList != null ) {
            subBitmapArrayList.clear();
            subBitmapArrayList = null;
        }
        if( validSubBitmapArrayList != null ) {
            validSubBitmapArrayList.clear();
            validSubBitmapArrayList = null;
        }
        if( validSubBitmapArrayListToModel != null ) {
            validSubBitmapArrayListToModel.clear();
            validSubBitmapArrayListToModel = null;
        }

        if( originalBitmap != null) {
            originalBitmap.recycle();
            originalBitmap = null;
        }

        if( rectBoundingBitmap != null) {
            rectBoundingBitmap.recycle();
            rectBoundingBitmap = null;
        }

        if( scaledBitmap != null) {
            scaledBitmap.recycle();
            scaledBitmap = null;
        }

        if( grayBitmap != null) {
            grayBitmap.recycle();
            grayBitmap = null;
        }

        if( gaussFilterBitmap != null) {
            gaussFilterBitmap.recycle();
            gaussFilterBitmap = null;
        }

        if( binariBitmap != null) {
            binariBitmap.recycle();
            binariBitmap = null;
        }

        if( expendBitmap != null) {
            expendBitmap.recycle();
            expendBitmap = null;
        }

        if( corrodeBitmap != null) {
            corrodeBitmap.recycle();
            corrodeBitmap = null;
        }

        if( subBitmapToModel != null) {
            subBitmapToModel.recycle();
            subBitmapToModel = null;
        }
        Log.d(TAG, "选择文件前，已经释放掉Bitmap空间");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult( Intent.createChooser(intent, "选择文件"), 1);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请先安装一个文件管理器.", Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "chooseFile END");
    }

    /**
     * onActivity()
     * 获取图片路径及原始图片，对原始图片进行预处理，并显示原始图片和处理后的图片
     * @param
     * @return
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult START");
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                Log.i(TAG,"uri:"+uri);

                try {
                    //String realPath = FileUtils.getPath(this,uri);
                    realPath = FileUtils.getPath(this,uri);
                    Log.i(TAG,"realPath:"+realPath);
                    imgprocTextView.setVisibility(View.VISIBLE);

                    if(realPath == null) {
                        imgprocTextView.setText("文件不存在，请重新选择！");
                        imgChooseButton.setText("重新选择文件");
                    }else if(!(realPath.endsWith(".jpg") || realPath.endsWith(".png") || realPath.endsWith(".bmp"))) {
                        imgprocTextView.setText("文件必须是jpg/png/bmp格式的，请重新选择！");
                        imgChooseButton.setText("重新选择文件");
                    }else{

                        originalBitmap = decodeUri(uri);
                        Log.i(TAG,"new Config:"+originalBitmap.getConfig()+" Width:"+originalBitmap.getWidth()+" Heigth:"+originalBitmap.getHeight());
                        // process image
                        //bitmap = imageProcess(originalBitmap);

                        originalImageView.setVisibility(View.VISIBLE);
                        originalImageView.setImageBitmap(originalBitmap);
                        originalTextView.setVisibility(View.VISIBLE);
                        originalTextView.setText("原图像");
                        //preProcessImageView.setVisibility(View.VISIBLE);
                        //preProcessImageView.setImageBitmap(bitmap);
                        //newimageView.setImageBitmap(cutBitmap);

                        String temp[] = realPath.split("\\/");
                        String fileName = temp[temp.length-1];
                        //imgprocTextView.setVisibility(View.VISIBLE);
                        imgprocTextView.setText("当前选择图像: "+fileName+" "+originalBitmap.getWidth()+"x"+originalBitmap.getHeight());
                        //imgprocTextView.setText("当前选择文件:"+realPath);

                    }

                }catch (java.lang.SecurityException e){
                    imgprocTextView.setVisibility(View.VISIBLE);
                    imgprocTextView.setText("应用需要读取SD卡，请先授权读取SD卡！");
                }catch (Exception e){
                    Log.w(TAG,"onActivityResult:"+e);
                    imgprocTextView.setVisibility(View.VISIBLE);
                    imgprocTextView.setText("选择文件出错:"+e.getMessage());
                }
            }
        }
        Log.d(TAG, "onActivityResult END");
    }

    /**
     * decodeUrl（）
     * 输入原始图片路径，返回该图片Bitmap。
     * @param selectedImageUrl
     * @return　该图片属于各个类别的概率。
     */
    private Bitmap decodeUri(Uri selectedImageUrl) throws FileNotFoundException {
        Log.d(TAG, "decodeUri START");

        Log.i(TAG,"decodeUri/selectedImageUrl:"+selectedImageUrl);
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        //o.inJustDecodeBounds = true;
        o.inSampleSize = 1;

        Log.d(TAG, "decodeUri END");
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImageUrl), null, o);
    }

    /**
     * imageProcess（）
     * 输入一张原始图片,进行预处理,裁剪,压缩,灰度化，高斯滤波，二值化，膨胀，腐蚀。
     * @param originalBitmap
     * @return　处理后的图片。
     */
    private void imageProcess(Bitmap originalBitmap) {
        Log.d(TAG, "imageProcess START");
        try {

            if(originalBitmap == null) {
                cnnrecTextView.setVisibility(View.VISIBLE);
                cnnrecTextView.setText("请选择图片！");
            }
            else {

                ImageProcessService imageProcessService = new ImageProcessService(MainActivity.this);
                // 裁剪 Resize cut picture
                /*
                int cut_x = 50, cut_y = 50;
                Bitmap cutBitmap = Bitmap.createBitmap(rgbaBitmap, cut_x, cut_y, rgbaBitmap.getWidth()-2*cut_x, rgbaBitmap.getHeight()-2*cut_y);
                Log.i(TAG,"cutBitmap Config:"+cutBitmap.getConfig()+" Width:"+cutBitmap.getWidth()+" Heigth:"+cutBitmap.getHeight());
                */

                long preTimeimageProcess, postTimeimageProcess, costTimeimageProcess;
                preTimeimageProcess = System.currentTimeMillis();

                // 压缩
                double scale = 1;
                scale = (double) scaleRate/100;
                int originalWidth = originalBitmap.getWidth();
                int originalHeigth = originalBitmap.getHeight();
                Log.d(TAG, "原图大小 W x H：" + originalWidth + "x" + originalHeigth);

                int scaledWidth = (int) (originalWidth*scale);
                int scaledHeigth = (int) (originalHeigth*scale);
                Log.d(TAG, "压缩比例:" + scale + " 压缩后图片大小 W x H：" + scaledWidth + "x" + scaledHeigth);

                // rescale
                scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeigth, true);

                //保存所有处理过程图片
                boolean isSave = false;
                String temp[] = realPath.split("\\.");
                String timeStamp = Long.toString(System.currentTimeMillis());

                if (isSave == true) {

                    String scaledBitmapName = temp[0] + "_" + timeStamp + "_scaledBitmap." + temp[temp.length-1];
                    if(saveImg(scaledBitmap, scaledBitmapName, context)) {
                        Log.d(TAG, scaledBitmapName+"图片保存成功！");
                    }
                }

                // 灰度
                //long preTimeGray, postTimeGray, costTimeGray;
                //preTimeGray = System.currentTimeMillis();
                //Log.d(TAG, "Before 灰度, 时间点：" + preTimeGray);
                grayBitmap = imageProcessService.toGrayscale(scaledBitmap, 4);
                //Bitmap grayBitmap = imageProcessService.toGrayscale(originalBitmap,4);
                //postTimeGray = System.currentTimeMillis();
                //Log.d(TAG, "After 灰度， 时间点：" + postTimeGray);
                //costTimeGray = postTimeGray - preTimeGray;
                //Log.d(TAG, "计算灰度耗时：" + costTimeGray + "ms");

                if (isSave == true) {
                    String grayBitmapName = temp[0] + "_" + timeStamp + "_grayBitmap." + temp[temp.length-1];
                    if(saveImg(grayBitmap, grayBitmapName, context)) {
                        Log.d(TAG, grayBitmapName+"图片保存成功！");
                    }
                }

                // 直方图均衡化
                //Bitmap equalizedHistBitmap = imageProcessService.toEqualizedHist(grayBitmap);

                // 高斯滤波 - 消除高频噪声
                //long preTimegauss, postTimegauss, costTimegauss;
                //preTimegauss = System.currentTimeMillis();
                //Log.d(TAG, "Before 高斯滤波, 时间点：" + preTimegauss);
                gaussFilterBitmap = imageProcessService.gaussFilter(grayBitmap);
                //postTimegauss = System.currentTimeMillis();
                //Log.d(TAG, "After 高斯滤波， 时间点：" + postTimeGray);
                //costTimegauss = postTimegauss - preTimegauss;
                //Log.d(TAG, "计算高斯滤波耗时：" + costTimeGray + "ms");

                if (isSave == true) {
                    String gaussFilterBitmapName = temp[0] + "_" + timeStamp + "_gaussFilterBitmap." + temp[temp.length-1];
                    if(saveImg(gaussFilterBitmap, gaussFilterBitmapName, context)) {
                        Log.d(TAG, gaussFilterBitmapName+"图片保存成功！");
                    }
                }

                // 二值化，直接判断是否需要反色
                //long preTimeBinari, postTimeBinari,costTimeBinari;
                //preTimeBinari = System.currentTimeMillis();
                //Log.d(TAG, "Before 二值化, 时间点：" + preTimeBinari);
                binariBitmap = imageProcessService.toBinarization(gaussFilterBitmap);
                //postTimeBinari = System.currentTimeMillis();
                //Log.d(TAG, "After 二值化， 时间点：" + postTimeBinari);
                //costTimeBinari = postTimeBinari - preTimeBinari;
                threshold = imageProcessService.bestThreshold;
                //Log.d(TAG, "计算二值化耗时：" + costTimeBinari + "ms" + "阈值" + threshold);
                Log.d(TAG, "白点总数：" + imageProcessService.finalWhiteCount+"黑点总数：" + imageProcessService.finalBlackCount);

                if (isSave == true) {
                    String binariBitmapName = temp[0] + "_" + timeStamp + "_binariBitmap." + temp[temp.length-1];
                    if(saveImg(binariBitmap, binariBitmapName, context)) {
                        Log.d(TAG, binariBitmapName+"图片保存成功！");
                    }
                }

                // 膨胀
                //long preTimeExpend, postTimeExpend, costTimeExpend;
                //preTimeExpend = System.currentTimeMillis();
                //Log.d(TAG, "Before 膨胀, 时间点：" + preTimeExpend);
                expendBitmap = imageProcessService.toExpend(binariBitmap);
                //Bitmap toRemoveNoiseBitmap = imageProcessService.toRemoveNoise(expendBitmap);
                //postTimeExpend = System.currentTimeMillis();
                //Log.d(TAG, "After 膨胀， 时间点：" + postTimeExpend);
                //costTimeExpend = postTimeExpend - preTimeExpend;
                //Log.d(TAG, "计算膨胀耗时：" + costTimeExpend + "ms");

                if (isSave == true) {
                    String expendBitmapName = temp[0] + "_" + timeStamp + "_expendBitmap." + temp[temp.length-1];
                    if(saveImg(expendBitmap, expendBitmapName, context)) {
                        Log.d(TAG, expendBitmapName+"图片保存成功！");
                    }
                }

                // 部分腐蚀
                //long preTimeCorrode, postTimeCorrode, costTimeCorrode;
                //preTimeCorrode = System.currentTimeMillis();
                //Log.d(TAG, "Before 腐蚀, 时间点：" + preTimeCorrode);
                //全腐蚀
                //corrodeBitmap = imageProcessService.toCorrode(expendBitmap);
                Bitmap removeNoiseBitmap = imageProcessService.toRemoveNoise(expendBitmap);
                //postTimeCorrode = System.currentTimeMillis();
                //Log.d(TAG, "After 腐蚀， 时间点：" + postTimeCorrode);
                //costTimeCorrode = postTimeCorrode - preTimeCorrode;
                //Log.d(TAG, "计算腐蚀耗时：" + costTimeCorrode + "ms");

                if (isSave == true) {
                    String removeNoiseBitmapName = temp[0] + "_" + timeStamp + "_removeNoiseBitmap." + temp[temp.length-1];
                    if(saveImg(removeNoiseBitmap, removeNoiseBitmapName, context)) {
                        Log.d(TAG, removeNoiseBitmapName+"图片保存成功！");
                    }
                }

                // 子图计算分割
                //Log.d(TAG, "子图分割处理开始!");
                double padding = 0.2;
                padding = (double) paddingRate/100;
                //long preTimeDivide, postTimeDivide, costTimeDivide;
                //preTimeDivide = System.currentTimeMillis();
                //Log.d(TAG, "Before 子图, 时间点：" + preTimeDivide);
                // 所有分割子图列表
                subBitmapArrayList = imageProcessService.getSubBitmapArrayList(removeNoiseBitmap);
                // 所有有效子图并填充后列表
                validSubBitmapArrayList = imageProcessService.getValidsubBitmapArrayList(subBitmapArrayList, padding);

                // 所有子图序号
                validSubBitmapArrayListIndex = imageProcessService.subBitmapArrayListIndex;
                //postTimeDivide = System.currentTimeMillis();
                //Log.d(TAG, "After 子图， 时间点：" + postTimeDivide);
                //costTimeDivide = postTimeDivide - preTimeDivide;
                //Log.d(TAG, "计算子图分割耗时：" + costTimeDivide + "ms");
                //Log.d(TAG, "子图分割处理结束!");

                Log.d(TAG, "subBitmapArrayList.size(): "+validSubBitmapArrayList.size());

                if(validSubBitmapArrayList.size() == 0) {
                    imgprocTextView.setVisibility(View.VISIBLE);
                    imgprocTextView.setText("空白图片，请输入手写数字图片！");
                    Log.d(TAG, "imgproc 空白图片，请输入手写数字图片！");
                }
                else{

                    // 获取所有子图的最小外接矩形列表
                    ArrayList<ArrayList<Integer>> allMinRectBoundingArrayList = imageProcessService.allMinRectBoundingPositionArrayList;
                    // 每个子图包含最小外接矩形
                    rectBoundingBitmap = imageProcessService.drawMinRectBoundingBitmap(scaledBitmap,allMinRectBoundingArrayList);

                    if (isSave == true) {
                        String rectBoundingBitmapName = temp[0] + "_" + timeStamp + "_rectBoundingBitmap."+ temp[temp.length-1];
                        if(saveImg(rectBoundingBitmap, rectBoundingBitmapName, context)) {
                            Log.d(TAG, rectBoundingBitmapName+"图片保存成功！");
                        }
                    }

                    // 输入模型子图列表
                    validSubBitmapArrayListToModel = new ArrayList<Bitmap>();

                    // 分割后子图依次处理，满足模型输入要求：(1)28x28 (2)黑底白字
                    int modelWidth=28;
                    int modelHeigth=28;
                    //String subPictureName;
                    //String temp[] = realPath.split("\\.");
                    //String timeStamp = Long.toString(System.currentTimeMillis());
                   
                    for(int subIndex=0; subIndex < validSubBitmapArrayList.size(); subIndex++) {

                        Bitmap subBitmap = validSubBitmapArrayList.get(subIndex);

                        if (isSave == true) {
                            String subPictureName = temp[0] + "_" + timeStamp + "_subBitmap_" + subIndex + "." + temp[temp.length-1];
                            if(saveImg(subBitmap, subPictureName, context)) {
                                Log.d(TAG, subPictureName+"图片保存成功！");
                            }
                        }

                        int rowIndex = validSubBitmapArrayListIndex.get(subIndex);

                        Log.d(TAG, "subIndex:" + subIndex + " rowIndex:" + rowIndex);

                        // resize to modelWidth x modelHeigth 28x28
                        //long preTimeResize, postTimeResize, costTimeResize;
                        //preTimeResize = System.currentTimeMillis();
                        //Log.d(TAG, "Before "+subBitmap.getWidth()+"x"+subBitmap.getHeight()+ "修改为28x28, 时间点：" + preTimeResize);

                        Bitmap resizeBitmapTemp = Bitmap.createScaledBitmap(subBitmap, modelWidth, modelHeigth, true);
                        //压缩后4邻域膨胀
                        //Bitmap expendFourBitmap = imageProcessService.toExpendFour(resizeBitmapTemp);

                        //postTimeResize = System.currentTimeMillis();
                        //Log.d(TAG, "After 修改为28x28， 时间点：" + postTimeResize);
                        //costTimeResize = postTimeResize - preTimeResize;
                        //Log.d(TAG, "修改为28x28耗时：" + costTimeResize + "ms");

                        if (isSave == true) {
                            String resizeBitmapTempName = temp[0] + "_" + timeStamp + "_resizeBitmapTemp_" + subIndex + "." + temp[temp.length-1];
                            if(saveImg(resizeBitmapTemp, resizeBitmapTempName, context)) {
                                Log.d(TAG, resizeBitmapTempName+"图片保存成功！");
                            }
                        }

                        // 预处理的图是白底黑字，需要反色
                        //Log.i(TAG,"原图是白底黑字，需要反色 toInverse");
                        //long preTimeInverse, postTimeInverse, costTimeInverse;
                        //preTimeInverse = System.currentTimeMillis();
                        //Log.d(TAG, "Before 反色, 时间点：" + preTimeInverse);
                        // 反色并存储
                        Bitmap modelInverseBitmap = imageProcessService.toInverse(resizeBitmapTemp);
                        //postTimeInverse = System.currentTimeMillis();
                        //Log.d(TAG, "After 反色， 时间点：" + postTimeInverse);
                        //costTimeInverse = postTimeInverse - preTimeInverse;
                        //Log.d(TAG, "计算反色耗时：" + costTimeInverse + "ms");

                        validSubBitmapArrayListToModel.add(modelInverseBitmap);

                        if (isSave == true) {
                            String modelInverseBitmapName = temp[0] + "_" + timeStamp + "_modelInverseBitmap_" + subIndex + "." + temp[temp.length-1];
                            if(saveImg(modelInverseBitmap, modelInverseBitmapName, context)) {
                                Log.d(TAG, modelInverseBitmapName+"图片保存成功！");
                            }
                        }

                    }

                    midPreProcessImageView.setVisibility(View.VISIBLE);
                    midPreProcessImageView.setImageBitmap(removeNoiseBitmap);
                    //midPreProcessImageView.setImageBitmap(subBitmapArrayList.get(0));
                    midPreProcessTextView.setVisibility(View.VISIBLE);
                    midPreProcessTextView.setText("预处理图像");

                    //debug
                    preProcessImageView.setVisibility(View.VISIBLE);
                    preProcessImageView.setImageBitmap(rectBoundingBitmap);
                    //preProcessImageView.setImageBitmap(subBitmapArrayListToModel.get(0));
                    preProcessTextView.setVisibility(View.VISIBLE);
                    //preProcessTextView.setText("最小外接矩形");
                    preProcessTextView.setText("分割提取有效子图");

                    // ms转换成s
                    //long totalTimeLong = costTimeGray + costTimegauss + costTimeBinari + costTimeExpend + costTimeCorrode;
                    //long totalTimeLong = costTimeGray + costTimegauss + costTimeBinari + costTimeExpend + costTimeCorrode + costTimeInverse + costTimeResize;
                    //double totalProcessTime = ((double) totalTimeLong)/1000;
                    //String totalTime = String.format("%.1f",totalProcessTime);
                    //String totalTime = String.valueOf(totalTimeLong);

                    postTimeimageProcess = System.currentTimeMillis();
                    costTimeimageProcess = postTimeimageProcess - preTimeimageProcess;
                    imgprocTextView.setVisibility(View.VISIBLE);
                    imgprocTextView.setText("原图像尺寸: "+originalWidth+"x"+originalHeigth+", 图像压缩后: "+scaledWidth+"x"+scaledHeigth + "\n二值化阈值: "+threshold+", 图像处理总耗时: " + costTimeimageProcess + "ms" );
                    Log.d(TAG, "图像处理总耗时:" + costTimeimageProcess + "ms");
                }

            }

        } catch (Exception e)  {
            Log.e(TAG, "图像处理发生异常:" + e);
            imgprocTextView.setVisibility(View.VISIBLE);
            imgprocTextView.setText("图像处理发生异常！" + e.getMessage());
        }
        Log.d(TAG, "imageProcess END");
    }

    /**
     * cnnRecognition（）
     * 输入一张28x28黑底白字图片，返回该图片属于各个类别的概率值（识别精确度），以及最终的识别结果。
     * @param
     * @return　该图片属于各个类别的概率。
     */
    private void cnnRecognition() {
        Log.d(TAG, "cnnRecognition START");

        try {

            //分割后子图依次识别处理

            Log.d(TAG, "validSubBitmapArrayListToModel.size():" + validSubBitmapArrayListToModel.size());
            if(validSubBitmapArrayListToModel.size() == 0) {
                cnnrecTextView.setVisibility(View.VISIBLE);
                cnnrecTextView.setText("空白图片，请输入手写数字图片！");
                Log.d(TAG, "cnnrec 空白图片，请输入手写数字图片！");
            }
            else {

                ArrayList<String> DigitLetterMapping47 = new ArrayList<String>();
                DigitLetterMapping47.add("0");
                DigitLetterMapping47.add("1");
                DigitLetterMapping47.add("2");
                DigitLetterMapping47.add("3");
                DigitLetterMapping47.add("4");
                DigitLetterMapping47.add("5");
                DigitLetterMapping47.add("6");
                DigitLetterMapping47.add("7");
                DigitLetterMapping47.add("8");
                DigitLetterMapping47.add("9");
                DigitLetterMapping47.add("A");
                DigitLetterMapping47.add("B");
                DigitLetterMapping47.add("C");
                DigitLetterMapping47.add("D");
                DigitLetterMapping47.add("E");
                DigitLetterMapping47.add("F");
                DigitLetterMapping47.add("G");
                DigitLetterMapping47.add("H");
                DigitLetterMapping47.add("I");
                DigitLetterMapping47.add("J");
                DigitLetterMapping47.add("K");
                DigitLetterMapping47.add("L");
                DigitLetterMapping47.add("M");
                DigitLetterMapping47.add("N");
                DigitLetterMapping47.add("O");
                DigitLetterMapping47.add("P");
                DigitLetterMapping47.add("Q");
                DigitLetterMapping47.add("R");
                DigitLetterMapping47.add("S");
                DigitLetterMapping47.add("T");
                DigitLetterMapping47.add("U");
                DigitLetterMapping47.add("V");
                DigitLetterMapping47.add("W");
                DigitLetterMapping47.add("X");
                DigitLetterMapping47.add("Y");
                DigitLetterMapping47.add("Z");
                DigitLetterMapping47.add("a");
                DigitLetterMapping47.add("b");
                DigitLetterMapping47.add("d");
                DigitLetterMapping47.add("e");
                DigitLetterMapping47.add("f");
                DigitLetterMapping47.add("g");
                DigitLetterMapping47.add("h");
                DigitLetterMapping47.add("n");
                DigitLetterMapping47.add("q");
                DigitLetterMapping47.add("r");
                DigitLetterMapping47.add("t");

                System.out.println(System.getProperty("java.library.path"));
                System.loadLibrary("tensorflow_inference");
                //ReLinker.loadLibrary("tensorflow_inference");

                Log.d(TAG, "Before RecognitionService, pruningSetting:"+pruningSetting);
                RecognitionService recognitionService = new RecognitionService(MainActivity.this, pruningSetting);
                //new RecognitionService(MainActivity.this, pruningSetting);
                model_file = RecognitionService.MODEL_FILE;

                //Log.d(TAG, "After RecognitionService");


                // String timeStamp = Long.toString(System.currentTimeMillis());

                StringBuffer recognitionResult = new StringBuffer();
                recognitionResult.append("识别结果:\n");
                long totalCostTime = 0;
                for (int index = 0; index < validSubBitmapArrayListToModel.size(); index++) {

                    long preTime, postTime, costTime;
                    //imgprocTextView.setText("识别结果："+ bitmap);
                    Log.d(TAG, "Before recognize");
                    preTime = System.currentTimeMillis();

                    subBitmapToModel = validSubBitmapArrayListToModel.get(index);
                    int rowIndex = validSubBitmapArrayListIndex.get(index);
                    Log.d(TAG, "index:" + index + " rowIndex:" + rowIndex);

                    if (index > 0 && rowIndex != validSubBitmapArrayListIndex.get(index - 1)) {
                        recognitionResult.append("\n");
                    }

                    //依次识别
                    //float[] outSoftmax = recognitionService.recognize(preProcessBitmap);
                    //float[] outSoftmax = recognitionService.recognize(originalBitmap);
                    float[] outSoftmax = recognitionService.recognize(subBitmapToModel);
                    int result_index = recognitionService.argmax(outSoftmax);
                    String result = DigitLetterMapping47.get(result_index);
                    double maxPercentage = recognitionService.max(outSoftmax);

                    postTime = System.currentTimeMillis();
                    costTime = postTime - preTime;
                    totalCostTime += costTime;
                    Log.d(TAG, "识别耗时：" + costTime + "ms");
                    Log.d(TAG, "After recognize");
                    //Log.d(TAG, "outSoftmax: " + Arrays.toString(outSoftmax));
                    Log.d(TAG, "Recognize Result: " + result + " maxPercentage:" + maxPercentage);

                    recognitionResult.append(result).append(", ");
                }

                recognitionResult.append("\n识别总耗时: ").append(totalCostTime).append("ms\n模型: ").append(model_file);
                cnnrecTextView.setVisibility(View.VISIBLE);
                cnnrecTextView.setText(recognitionResult);
                Log.d(TAG, String.valueOf(recognitionResult));
            }
            //cnnrecTextView.setText(String.format("识别结果：%d\n精确度:%s\n识别耗时:%sms\n模型:%s", recognitionService.argmax(outSoftmax), recognitionService.max(outSoftmax), costTime, model_file));

            //Toast.makeText(this,"outSoftmax:"+ Arrays.toString(outSoftmax),Toast.LENGTH_LONG).show();



            //startRecognitionButton.setVisibility(View.GONE);
            //imgChooseButton.setText("继续选择文件");
            //imgChooseButton.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "识别中发生异常:" + e);
            cnnrecTextView.setVisibility(View.VISIBLE);
            cnnrecTextView.setText("识别中发生异常！"+e.getMessage()+"\n先处理图像。");

            //startRecognitionButton.setVisibility(View.GONE);
            //imgChooseButton.setText("重新选择文件");
            //imgChooseButton.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "cnnRecognition END");
    }

    /**
     * requestReadExternalPermission()
     * 读取SD卡是否需要用户授权输入一张图片。
     * @param
     * @return
     */
    @SuppressLint("NewApi")
    private void requestReadExternalPermission() {
        Log.d(TAG, "requestReadExternalPermission START");
        Log.d(TAG, "check..."+checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
        Log.d(TAG, "Pack..."+PackageManager.PERMISSION_GRANTED);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ permission IS NOT granted...");

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            //imgprocTextView.setText("READ permission IS NOT granted...");

        } else {
            Log.d(TAG, "READ permission is granted...");
            //imgprocTextView.setText("READ permission is granted...");
        }
        Log.d(TAG, "requestReadExternalPermission END");

        Log.d(TAG, "requestWriteExternalPermission START");
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "WRITE permission IS NOT granted...");

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            //debugTextView.setText("READ permission IS NOT granted...");

        } else {
            Log.d(TAG, "WRITE permission is granted...");
            //debugTextView.setText("READ permission is granted...");
        }

        Log.d(TAG, "requestWriteExternalPermission END");

    }

    /**
     * saveImg()
     * 读取SD卡是否需要用户授权输入一张图片。
     * @param bitmap, name, context
     * @return
     */
    private static boolean saveImg(Bitmap bitmap, String name, Context context) {
        try {
            /*
            String sdcardPath = System.getenv("EXTERNAL_STORAGE");      //获得sd卡路径
            String dir = sdcardPath + "/DCIM/";                    //图片保存的文件夹名
            File file = new File(dir);                                 //已File来构建
            if (!file.exists()) {                                     //如果不存在  就mkdirs()创建此文件夹
                file.mkdirs();
            }
            Log.i("SaveImg", "file uri==>" + dir);
            File mFile = new File(dir + name);                        //将要保存的图片文件
             */
            // 直接传入完整路径和文件名
            File mFile = new File(name);
            if (mFile.exists()) {

                Toast.makeText(context, "该图片已存在!", Toast.LENGTH_SHORT).show();
                return false;
            }

            FileOutputStream outputStream = new FileOutputStream(mFile);     //构建输出流
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);  //compress到输出outputStream
            Uri uri = Uri.fromFile(mFile);                                  //获得图片的uri
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)); //发送广播通知更新图库，这样系统图库可以找到这张图片
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


}
