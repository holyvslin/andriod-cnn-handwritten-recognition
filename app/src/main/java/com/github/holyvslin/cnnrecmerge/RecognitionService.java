package com.github.holyvslin.cnnrecmerge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class RecognitionService {

    public static final String TAG = "RecognitionService";
    public static String MODEL_FILE = "";
    //asserts目录下的pb文件名字
    public static final String MODEL_FILE_PRUNING_OFF = "file:///android_asset/Android-model.pb";
    //asserts目录下的pb文件名字
    public static final String MODEL_FILE_PRUNING_ON = "file:///android_asset/LightWeightAndroid-model.pb";
    //输入节点的名称
    public static final String INPUT_NODE = "input";
    //输出节点的名称
    public static final String OUTPUT_NODE = "out_softmax";
    // keep_prob节点的名称
    public static final String KEEP_PROB_NODE = "keep_prob_placeholder";

    //输出节点的个数，即总的类别数。
    //public static final int NUM_CLASSES = 10;
    public static final int NUM_CLASSES = 47;
    //输入图片的像素高
    public static final int HEIGHT = 28;
    //输入图片的像素宽
    public static final int WIDTH = 28;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;


    public RecognitionService(Context context, boolean isPruning){

        //初始化TensorFlowInferenceInterface对象。
        //根据指定的MODEL_FILE创建一个本地的TensorFlow session
        //初始化TensorFlowInferenceInterface对象
        Log.i(TAG,"isPruning:"+isPruning);
        if (isPruning == true) {

            String tempOn[] = MODEL_FILE_PRUNING_ON.split("\\/");
            MODEL_FILE = tempOn[tempOn.length-1];
        } else {
            String tempOff[] = MODEL_FILE_PRUNING_OFF.split("\\/");
            MODEL_FILE = tempOff[tempOff.length-1];
        }

        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);

    }

    /**
     * recognition()
     * 输入一张28x28黑底白字图片到训练好的模型中，返回该图片属于各个类别的概率值（识别精确度），以及最终的识别结果
     * @param bitmap
     * @return　该图片属于各个类别的概率。
     */
    public float[] recognize(Bitmap bitmap){

        Log.i(TAG,"recognizing...");
        //Log.i(TAG,"bitmap.toString(): "+bitmap.toString());

        //为输入节点准备数据
        float[] pixelArray = bitmapToFloatArray(bitmap);
        //Log.i(TAG,"pixelArray:"+Arrays.toString(pixelArray));
        if(pixelArray.length != HEIGHT*WIDTH){
            throw new IllegalArgumentException("输入图片的像素矩阵的大小不对，传入的大小为:"+pixelArray.length +",需要的大小为："+(HEIGHT*WIDTH));
        }

        // 输入数据
        // Copy the input data into TensorFlow.
        Log.i(TAG,"feed:");

        //Tensor with shape [1, 28, 28, 3]
        //inferenceInterface.feed(INPUT_NODE, pixelArray, 1, HEIGHT, WIDTH, 3);
        //Tensor with shape [1, 28, 28]
        //Log.i(TAG,"before feed:" + inferenceInterface);
        inferenceInterface.feed(INPUT_NODE, pixelArray, 1, HEIGHT, WIDTH);
        inferenceInterface.feed(KEEP_PROB_NODE,new float[]{1.0f}, 1);
        //Log.i(TAG,"after feed:" + inferenceInterface);

        //进行模型的推理
        // Run the inference call.
        Log.i(TAG,"run:");
        String[] outputNames = new String[] {OUTPUT_NODE};
        inferenceInterface.run(outputNames, logStats);

        //获取输出节点的输出信息
        // Copy the output Tensor back into the output array.
        //用于存储模型的输出数据
        float[] outputs = new float[NUM_CLASSES];
        Log.i(TAG,"fetch:");
        //获取输出数据
        inferenceInterface.fetch(OUTPUT_NODE, outputs);

        return outputs;
    }

    /**
     * argmax()
     * 输入数组，获取数组的最大元素，返回对应的索引。
     * @param prob
     * @return　最大元素的索引
     */
    public static int argmax(float[] prob){
        int result = 0;
        for(int i=1;i<prob.length;i++) {
            if (prob[result] < prob[i]) {
                result = i;
            }
        }
        return result;
    }

    /**
     * max()
     * 输入数组，返回该数组的最大元素。
     * @param prob
     * @return　最大元素
     */
    public static float max(float[] prob){
        int result = 0;
        for(int i=1;i<prob.length;i++) {
            if (prob[result] < prob[i]) {
                result = i;
            }
        }
        return prob[result];
    }

    /**
     * bitmapToFloatArray()
     * 将bitmap转为（按行优先）一个float数组。其中的每个像素点都归一化到0~1之间。
     * @param bitmap 灰度图，r,g,b分量都相等。
     * @return
     */
    public static float[] bitmapToFloatArray(Bitmap bitmap){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        float[] result = new float[height*width];
        Log.i(TAG,"bitmap width:"+width+",height:"+height);
        //Log.i(TAG,"bitmap.getConfig():"+bitmap.getConfig());

        int k = 0;

        //行优先
        //for(int j = 0;j < height;j++){
            //for (int i = 0;i < width;i++){
        for(int j = 0;j < width;j++){
            for (int i = 0;i < height;i++){
                int argb = bitmap.getPixel(j,i);

                int r = Color.red(argb);
                int g = Color.green(argb);
                int b = Color.blue(argb);
                int a = Color.alpha(argb);

                //由于是灰度图，所以r,g,b分量是相等的。
                assert(r==g && g==b);

               //Log.i(TAG,i+","+j+" : argb = "+argb+", a="+a+", r="+r+", g="+g+", b="+b);
                //bitmaplist[k++] = r;
                result[k++] = r / 255.0f;
            }

        }

        //Log.d(TAG, "Bitmap to Model: " + Arrays.toString(result));

        return result;
    }

}
