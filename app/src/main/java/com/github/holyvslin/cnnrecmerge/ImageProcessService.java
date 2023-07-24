package com.github.holyvslin.cnnrecmerge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by holyvslin on 2020/01/01.
 */

public class ImageProcessService {

    public static final String TAG = "ImageProcessService";
    //全局动态二值阈值
    public int bestThreshold;
    //二值化图像中黑点和白点总数
    public static int finalBlackCount, finalWhiteCount;

    //子图对应行号
    ArrayList<Integer> subBitmapArrayListIndex;

    // 子图最小外接矩形在分割之前图像上的坐标列表, 外层列表是子图，内层列表是子图最小外接矩形坐标
    ArrayList<ArrayList<Integer>> allMinRectBoundingPositionArrayList = new ArrayList<>();

    public ImageProcessService(Context context) {
    }


    /**
     * toGrayscale()
     * 输入一张原始图片及灰度方法，返回灰度图片。
     * @param originalBitmap
     * @return grayscaleBitmap
     */
    public Bitmap toGrayscale(Bitmap originalBitmap, int grayWay) {

        Log.d(TAG, "toGrayscale START");
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        //新建图片
        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        int grayColor = 0, alpha = 0, gray = 0;
        int[] rgbArray = new int[3];
        Log.d(TAG, "grayWay: " + grayWay);
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                //获得每一个位点的颜色
                grayColor = originalBitmap.getPixel(w, h);
                //获得三原色 R, 灰度图 R=B=G
                alpha = Color.alpha(grayColor);
                rgbArray[0] = Color.red(grayColor);
                rgbArray[1] = Color.green(grayColor);
                rgbArray[2] = Color.blue(grayColor);
                //Log.d(TAG, "rgbArray[0]:"+rgbArray[0]+" rgbArray[1]:"+rgbArray[1]+" rgbArray[2]:"+rgbArray[2]);
                if(grayWay == 1) {
                    // 1. 最大值灰度
                    int result = 0;
                    for (int k = 1; k < rgbArray.length; k++) {
                        if (rgbArray[result] < rgbArray[k]) {
                            result = k;
                        }
                    }
                    gray = rgbArray[result];
                }
                if(grayWay == 2) {
                    // 2.平均值
                    gray = (rgbArray[0] + rgbArray[1] + rgbArray[2])/3;
                }
                if(grayWay == 3) {
                    // 3.人眼感知灰度 green多
                    gray = (int) (rgbArray[0] * 0.299 + rgbArray[1] * 0.587 + rgbArray[2] * 0.114);
                }
                if(grayWay == 4) {
                    // 4.人眼感知灰度增强/加权平均法
                    rgbArray[0] = (int) (rgbArray[0] * 1.1) + 20;
                    rgbArray[1] = (int) (rgbArray[1] * 1.1) + 20;
                    rgbArray[2] = (int) (rgbArray[2] * 1.1) + 20;
                    if (rgbArray[0] >= 255) {
                        rgbArray[0] = 255;
                    }
                    if (rgbArray[1] >= 255) {
                        rgbArray[1] = 255;
                    }
                    if (rgbArray[2] >= 255) {
                        rgbArray[2] = 255;
                    }

                    //此处根据实际需要进行设定权重
                    gray = (int) Math.pow((
                            Math.pow(rgbArray[0], 2) * 0.2973
                                    + Math.pow(rgbArray[1], 2) * 0.6274
                                    + Math.pow(rgbArray[2], 2) * 0.0753), 1.0 / 2);
                    //Log.d(TAG, "rgbArray[0]:"+rgbArray[0]+" rgbArray[1]:"+rgbArray[1]+" rgbArray[2]:"+rgbArray[2]+" gray:" + gray);
                }

                grayscaleBitmap.setPixel(w, h, Color.argb(alpha, gray, gray, gray));
            }
        }
        Log.d(TAG, "toGrayscale END");
        return grayscaleBitmap;
    }

    /**
     * histogramArray()
     * 输入一张灰色图片，获取灰度直方图，返回该直方图数组。
     * @param grayBitmap
     * @return　处理后的图片。
     */
    public int[] getHistogramArray(Bitmap grayBitmap) {
        Log.d(TAG, "getHistogramArray START");
        int width = grayBitmap.getWidth();
        int height = grayBitmap.getHeight();

        //求出灰度数组，最大灰度值和最小灰度值
        int grayColor = 0, gray = 0;
        int maxGray,minGray;
        maxGray = minGray = Color.red(grayBitmap.getPixel(0,0));

        //获取灰度直方图
        int[] histogram = new int[256];
        for(int i=0; i<histogram.length; i++) {
            histogram[i] = 0;
        }

        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                //获得每一个位点的颜色
                grayColor = grayBitmap.getPixel(w, h);
                //获得三原色 R, 灰度图 R=B=G
                gray = Color.red(grayColor);
                //Log.d(TAG, " gray:" + gray);
                //生成灰度直方图数组
                histogram[gray]++;

            }
        }

        Log.d(TAG, "histogram: " + Arrays.toString(histogram));
        Log.d(TAG, "getHistogramArray END");
        return histogram;
    }

    /**
     * getHorBlackPointsHistogram()
     * 输入一张二值图片，获取水平方向累积黑点直方图数组。
     * @param binariBitmap
     * @return　数组。
     */
    public int[] getHorBlackPointsHistogram(Bitmap binariBitmap) {
        Log.d(TAG, "getHorBlackPointsHistogram START");
        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();

        //求出灰度数组，最大灰度值和最小灰度值
        int grayColor = 0, gray = 0;

        //获取水平累积灰度值直方图
        int[] HorBlackPointsHistogram = new int[width];
        for (int w = 0; w < width; w++) {
            HorBlackPointsHistogram[w] = 0;
            for (int h = 0; h < height; h++) {
                //获得每一个位点的颜色
                grayColor = binariBitmap.getPixel(w, h);
                //获得三原色 R, 灰度图 R=B=G
                gray = Color.red(grayColor);
                //Log.d(TAG, " gray:" + gray);
                // 计算某一个w位置垂直方向黑点总个数
                if(gray == 0){
                    HorBlackPointsHistogram[w]++;
                }
            }
        }

        Log.d(TAG, "HorBlackPointsHistogram: " + Arrays.toString(HorBlackPointsHistogram));
        Log.d(TAG, "getHorBlackPointsHistogram END");
        return HorBlackPointsHistogram;
    }

    /**
     * getHorizontalDividePositionArray()
     * 输入水平方向累积黑点直方图数组，返回水平方向分割点。
     * @param HorBlackPointsHistogram
     * @return　列表。
     */
    public ArrayList<Integer> getHorizontalDividePositionArray(int[] HorBlackPointsHistogram) {
        Log.d(TAG, "getHorizontalDividePositionArray START");
        // 水平方向分割点列表
        ArrayList<Integer> HorDividePositionlist = new ArrayList<Integer>();

        // 取水平方向分割点
        // 分割点阈值
        int NoiseNumber=0;
        //第一个黑点数如果大于阈值，即为第一个分割点
        if( HorBlackPointsHistogram[0] > NoiseNumber) {
            HorDividePositionlist.add(0);
            Log.d(TAG, "水平方向第一个点是分割点");
        }
        for (int w = 1; w < HorBlackPointsHistogram.length; w++) {
                //前白点转变成后黑点，取转变前的白点
                if (HorBlackPointsHistogram[w-1] <= NoiseNumber && HorBlackPointsHistogram[w] > NoiseNumber) {
                    HorDividePositionlist.add(w-1);
                    Log.d(TAG, "w-1: " + (w-1));
                }
                //前黑点转变成后白点，取转变后的黑点
                else if (HorBlackPointsHistogram[w-1] > NoiseNumber && HorBlackPointsHistogram[w] <= NoiseNumber) {
                    HorDividePositionlist.add(w);
                    Log.d(TAG, "w: " + w);
                }
        }

        //最后一个黑点数如果大于阈值，即为最后一个分割点
        if (HorBlackPointsHistogram[HorBlackPointsHistogram.length-1] > NoiseNumber) {
            HorDividePositionlist.add(HorBlackPointsHistogram.length-1);
            Log.d(TAG, "水平方向最后一个点是分割点");
        }
        Log.d(TAG, "HorBlackPointsHistogram: " + Arrays.toString(HorBlackPointsHistogram));
        Log.d(TAG, "HorDividePositionlist: " + HorDividePositionlist);
        Log.d(TAG, "getHorizontalDividePositionArray END");
        return HorDividePositionlist;
    }

    /**
     * getVerBlackPointsHistogram()
     * 输入一张二值图片，获取垂直方向累积黑点直方图数组。
     * @param binariBitmap
     * @return　数组。
     */
    public int[] getVerBlackPointsHistogram(Bitmap binariBitmap) {

        Log.d(TAG, "getVerBlackPointsHistogram START");
        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        //Log.d(TAG, " width:" + width+" height:"+height);
        //求出灰度数组，最大灰度值和最小灰度值
        int grayColor = 0, gray = 0;

        //获取垂直方向累积灰度值直方图
        int[] VerBlackPointsHistogram = new int[height];
        for (int h = 0; h < height; h++) {
            VerBlackPointsHistogram[h] = 0;
            for (int w = 0; w < width; w++) {
                //Log.d(TAG, " h:" + h + " w:" + w);

                //获得每一个位点的颜色
                grayColor = binariBitmap.getPixel(w, h);
                //获得三原色 R, 灰度图 R=B=G
                gray = Color.red(grayColor);
                //Log.d(TAG, " gray:" + gray);
                // 计算某一个w位置垂直方向黑点总个数
                if(gray == 0){
                    VerBlackPointsHistogram[h]++;
                }
            }
        }

        Log.d(TAG, "VerBlackPointsHistogram: " + Arrays.toString(VerBlackPointsHistogram));
        Log.d(TAG, "getVerBlackPointsHistogram END");
        return VerBlackPointsHistogram;
    }

    /**
     * getVerticalDividePositionArray()
     * 输入垂直方向累积黑点直方图数组，返回垂直方向分割点。
     * @param VerBlackPointsHistogram
     * @return　列表。
     */
    public ArrayList<Integer> getVerticalDividePositionArray(int[] VerBlackPointsHistogram) {

        Log.d(TAG, "getVerticalDividePositionArray START");
        // 垂直方向分割点列表
        ArrayList<Integer> VerDividePositionlist = new ArrayList<Integer>();

        // 取垂直方向分割点
        // 分割点阈值
        int NoiseNumber=0;
        //第一个黑点数如果大于阈值，即为第一个分割点
        if( VerBlackPointsHistogram[0] > NoiseNumber) {
            VerDividePositionlist.add(0);
            Log.d(TAG, "垂直方向第一个点是分割点");
        }

        for (int h = 1; h < VerBlackPointsHistogram.length; h++) {
            //前白点转变成后黑点，取转变前的白点
            if (VerBlackPointsHistogram[h - 1] <= NoiseNumber && VerBlackPointsHistogram[h] > NoiseNumber) {
                VerDividePositionlist.add(h - 1);
                Log.d(TAG, "h-1: " + (h - 1));
            }
            //前黑点转变成后白点，取转变后的黑点
            else if (VerBlackPointsHistogram[h - 1] > NoiseNumber && VerBlackPointsHistogram[h] <= NoiseNumber) {
                VerDividePositionlist.add(h);
                Log.d(TAG, "h: " + h);
            }

        }

        //最后一个黑点数如果大于阈值，即为最后一个分割点
        if (VerBlackPointsHistogram[VerBlackPointsHistogram.length-1] > NoiseNumber) {
            VerDividePositionlist.add(VerBlackPointsHistogram.length-1);
            Log.d(TAG, "垂直方向最后一个点是分割点");
        }

        Log.d(TAG, "VerDividePositionlist: " + VerDividePositionlist);
        Log.d(TAG, "getVerticalDividePositionArray END");
        return VerDividePositionlist;
    }

    /**
     * getVerticalDividePositionArray()
     * 输入一张二值图片，获取水平方向累积灰度值直方图数组，返回水平方向分割点。
     * @param binariBitmap
     * @return　数组。
     */
    public boolean isValidDigit(Bitmap binariBitmap) {
        Log.d(TAG, "isValidDigit START");

        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        Log.d(TAG, "width:" + width + " height:" + height);
        int grayColor = 0, gray = 0;

        // 像素总数
        int totalPixel = width*height;
        // 黑点总数
        int totalPixelBlack = 0;
        // 图包含有效数字黑点比例
        double bitmapBlackPercentage = 0.0;
        // 图包含有效数字黑点阈值
        double ValidDigitBlackThreshold = 0.01;
        boolean isDigit;
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                //获得每一个位点的颜色
                grayColor = binariBitmap.getPixel(w, h);
                //获得三原色 R, 灰度图 R=B=G
                gray = Color.red(grayColor);
                //Log.d(TAG, " gray:" + gray);
                // 计算某一个w位置垂直方向黑点总个数
                if(gray == 0){
                    totalPixelBlack++;
                }
            }
        }
        // 计算子图中黑点占比
        bitmapBlackPercentage = (double)totalPixelBlack/ (double)totalPixel;

        // 黑点比例大于阈值，认为子图包含有效数字
        if ( bitmapBlackPercentage > ValidDigitBlackThreshold)
            isDigit = true;
        else
            isDigit = false;
        Log.d(TAG, "totalPixelBlack:" + totalPixelBlack + " totalPixel:" + totalPixel+ " bitmapBlackPercentage:" + bitmapBlackPercentage + " ValidDigitBlackThreshold:" + ValidDigitBlackThreshold  + " isDigit:" + isDigit);

        Log.d(TAG, "isValidDigit END");
        return  isDigit;
    }

    /**
     * addPaddingLeftBitmap()
     * 输入一张灰色图片，填充宽度，填充颜色，返回左边填充图像。
     * @param bitmap，paddingLeft，color
     * @return　左边填充图像。
     */
    public Bitmap addPaddingLeftBitmap(Bitmap bitmap, int paddingLeft, int color) {
        Log.d(TAG, "addPaddingLeftBitmap START");

        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth() + paddingLeft, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(color);
        canvas.drawBitmap(bitmap, paddingLeft, 0, null);

        Log.d(TAG, "addPaddingLeftBitmap END");
        return outputBitmap;
    }

    /**
     * addPaddingRightBitmap()
     * 输入一张灰色图片，填充宽度，填充颜色，返回右边填充图像。
     * @param bitmap，paddingRight，color
     * @return　右边填充图像。
     */
    public Bitmap addPaddingRightBitmap(Bitmap bitmap, int paddingRight, int color) {
        Log.d(TAG, "addPaddingRightBitmap START");

        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth() + paddingRight, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(color);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Log.d(TAG, "addPaddingRightBitmap END");
        return outputBitmap;
    }

    /**
     * addPaddingTopBitmap()
     * 输入一张灰色图片，填充宽度，填充颜色，返回上边填充图像。
     * @param bitmap，paddingTop，color
     * @return　上边填充图像。
     */
    public Bitmap addPaddingTopBitmap(Bitmap bitmap, int paddingTop, int color) {
        Log.d(TAG, "addPaddingRightBitmap START");

        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight() + paddingTop, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(color);
        canvas.drawBitmap(bitmap, 0, paddingTop, null);

        Log.d(TAG, "addPaddingRightBitmap END");
        return outputBitmap;
    }

    /**
     * addPaddingBottomBitmap()
     * 输入一张灰色图片，填充宽度，填充颜色，返回下边填充图像。
     * @param bitmap，paddingBottom，color
     * @return　下边填充图像。
     */
    public Bitmap addPaddingBottomBitmap(Bitmap bitmap, int paddingBottom, int color) {
        Log.d(TAG, "addPaddingBottomBitmap START");

        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight() + paddingBottom, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(color);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Log.d(TAG, "addPaddingBottomBitmap END");
        return outputBitmap;
    }

    /**
     * toPaddingBoundary()
     * 输入一张灰色图片，返回周围填充正方形图像。
     * @param grayBitmap
     * @return　周围填充正方形图像。
     */
    public Bitmap toPaddingBoundaryBitmap(Bitmap grayBitmap, double paddingRate) {

        Log.d(TAG, "toPaddingBoundaryBitmap START");
        int width = grayBitmap.getWidth();
        int height = grayBitmap.getHeight();

        // 左右填充宽度
        int StartX = 0;
        // 上下填充宽度
        int StartY = 0;
        // 填充后正方形边长
        int length = 0;
        //最长边填充宽度（单边）
        int padding = 0;
        // 填充比例，MNIST 数据集特点，数字外接矩形20x20，画布28x28 填充率为0.2
        //double paddingRate = 0.1;
        //double paddingRate = 0.2;
        // 填充保持居中位置
        if(height > width){
            padding = (int) (height*paddingRate);
            length = height + 2*padding;
            StartY = padding;
            StartX = (length - width)/2;
        }
        else{
            padding = (int) (width*paddingRate);
            length = width + 2*padding;
            StartX = padding;
            StartY = (length - height)/2;
        }

        Log.d(TAG, "width: "+width + " height: "+height + " StartY: "+StartY + " StartX: "+StartX + " length:"+length);

        int color=0;
        // 原图是白底黑字，周围填充白色
        // white
        color = Color.WHITE;

        // 新建填充后的尺寸图像
        Bitmap paddingBoundaryBitmap = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888);
        // 绘制填充颜色
        Canvas canvas = new Canvas(paddingBoundaryBitmap);
        canvas.drawColor(color);
        // 在新起点上绘制原图，从而得到周围填充新图
        canvas.drawBitmap(grayBitmap, StartX, StartY, null);

        Log.d(TAG, "toPaddingBoundaryBitmap END");
        return paddingBoundaryBitmap;

    }

    /**
     * getValidsubBitmapArrayList()
     * 输入分割后的所有子图列表，通过面积平均值求出阈值，高宽比，有效数字。比较保留所有有效子图列表。
     * @param subBitmapArrayList
     * @return　所有有效子图列表。
     */

    public ArrayList<Bitmap> getValidsubBitmapArrayList(ArrayList<Bitmap> subBitmapArrayList, double paddingRate) {

        Log.d(TAG, "getValidsubBitmapArrayList START");

        if( subBitmapArrayList.size() != 0) {

            ArrayList<Bitmap> validSubBitmapArrayList = new ArrayList<Bitmap>();

            //宽高比阈值
            double maxHWRateThreshold = 8.0;
            double minHWRateThreshold = 0.2;

            // 求所有剩下子图面积和均值
            int areaSum = 0;
            int areaAvg = 0;

            for (int i = 0; i < subBitmapArrayList.size(); i++) {

                int widthTemp = subBitmapArrayList.get(i).getWidth();
                int heightTemp = subBitmapArrayList.get(i).getHeight();

                //宽高比
                double HWRate = 0.00;
                HWRate = (double) heightTemp/ (double) widthTemp;

                // 面积小于阈值，或者宽高比大于最大阈值，小于最小阈值认为是无效子图

                Log.d(TAG, " 高："+heightTemp+"宽："+widthTemp+" 高宽比：" + HWRate + " 高宽比阈值：" + minHWRateThreshold+","+maxHWRateThreshold);
                if( HWRate > maxHWRateThreshold || HWRate < minHWRateThreshold) {
                    Log.d(TAG, "i："+i+", before remove subBitmapArrayList.size()"+subBitmapArrayList.size());
                    Log.d(TAG, "删除无效子图、行号和最小外接矩形坐标，序号："+i);

                    subBitmapArrayList.remove(i);
                    subBitmapArrayListIndex.remove(i);
                    allMinRectBoundingPositionArrayList.remove(i);
                    i--;
                    Log.d(TAG, "i："+i+", after remove subBitmapArrayList.size()"+subBitmapArrayList.size());
                }

                // 有效子图做周围填充
                else {

                    areaSum += widthTemp*heightTemp;
                    Log.d(TAG, "高宽比判断之后，有效子图");
                    // 有效子图周围填充成正方形图像
                    //validSubBitmapArrayList.add(toPaddingBoundaryBitmap(subBitmapArrayList.get(i), paddingRate));
                    //validsubBitmapArrayList.add(subBitmapArrayList.get(i));
                }

            }

            for (int j = 0; j < subBitmapArrayList.size(); j++) {

                int widthTemp = subBitmapArrayList.get(j).getWidth();
                int heightTemp = subBitmapArrayList.get(j).getHeight();
                int areaTemp = widthTemp*heightTemp;
                areaSum += areaTemp;
                //Log.d(TAG, "序号："+j+" 宽："+subBitmapArrayList.get(j).getWidth()+" 高："+subBitmapArrayList.get(j).getHeight() + " 面积为" + subBitmapArrayList.get(j).getWidth()*subBitmapArrayList.get(j).getHeight()+" 总面积为：" + areaSum);
            }

            areaAvg = areaSum/subBitmapArrayList.size();
            //面积阈值
            double validAreaRate = 0.15;
            int validThreshold = (int) (areaAvg*validAreaRate);
            Log.d(TAG, "平均面积为："+areaAvg+", 有效比例:"+validAreaRate+", 有效阈值:"+validThreshold);

            for (int k = 0; k < subBitmapArrayList.size(); k++) {

                int widthTemp = subBitmapArrayList.get(k).getWidth();
                int heightTemp = subBitmapArrayList.get(k).getHeight();
                int areaTemp = widthTemp * heightTemp;

                Log.d(TAG, " 高："+heightTemp+" 宽："+widthTemp+ " 面积为：" + areaTemp + " 面积有效阈值：" + validThreshold);

                if (areaTemp < validThreshold) {
                    Log.d(TAG, "k：" + k + ", before remove subBitmapArrayList.size()" + subBitmapArrayList.size());
                    Log.d(TAG, "删除无效子图、行号和最小外接矩形坐标，序号：" + k);

                    subBitmapArrayList.remove(k);
                    subBitmapArrayListIndex.remove(k);
                    allMinRectBoundingPositionArrayList.remove(k);
                    k--;
                    Log.d(TAG, "k：" + k + ", after remove subBitmapArrayList.size()" + subBitmapArrayList.size());
                }

                // 有效子图做周围填充
                else {
                    Log.d(TAG, "有效面积判断之后，有效子图");
                    // 有效子图周围填充成正方形图像
                    validSubBitmapArrayList.add(toPaddingBoundaryBitmap(subBitmapArrayList.get(k), paddingRate));
                    //validsubBitmapArrayList.add(subBitmapArrayList.get(i));
                }
            }

            Log.d(TAG, "getValidsubBitmapArrayList END");
            return validSubBitmapArrayList;
        }
        else {

            Log.d(TAG, "getValidsubBitmapArrayList END");
            return subBitmapArrayList;
        }
    }



    /**
     * getSubBitmapArray()
     * 输入一张二值图片，获取水平方向累积灰度值直方图数组，返回每行和子图Map数组。
     * @param binariBitmap
     * @return　Map数组。
     */
    public ArrayList<Bitmap> getSubBitmapArrayList(Bitmap binariBitmap) {

        Log.d(TAG, "getSubBitmapArrayList START");

        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        Log.d(TAG, "width:"+width+ " height:" + height);

        // 子图包含有效数字黑点阈值
        double subBitmapBlackThreshold = 0.01;

        // 先水平后垂直方向子图序号，即行号，初始值0为第1行
        int subVerBitmapIndex = 0;

        // 垂直方向的子图列表
        //ArrayList<Bitmap> subVerBitmapArray = new ArrayList<Bitmap>();

        // 子图行号列表
        subBitmapArrayListIndex = new ArrayList<Integer>();
        ArrayList<Bitmap> subBitmapArrayList = new ArrayList<Bitmap>();

        // 针对原二值化图像获取垂直方向累积黑点直方图数组
        int[] VerBlackPointsHistogram = getVerBlackPointsHistogram(binariBitmap);
        // 针对垂直方向累积黑点直方图数组获取 垂直方向分割点
        ArrayList<Integer> VerDividePositionlist = getVerticalDividePositionArray(VerBlackPointsHistogram);
        Log.d(TAG, "VerDividePositionlist.size():"+VerDividePositionlist.size());

        // 垂直方向没有分割点,无对应阈值黑点,即为纯白色图像
        if (VerDividePositionlist.size() == 0){
            Log.d(TAG, "原图垂直方向不需要分割,无对应阈值黑点,即为纯白色图像!");
            Log.d(TAG, "getSubBitmapArrayList END");
            return subBitmapArrayList;
        }
        // 垂直方向有分割点
        else{
            //分割子图，先垂直方向分割，再水平方向分割
            for (int i = 0; i < VerDividePositionlist.size()-1; i++) {

                int VerSubXStart=0;
                int VerSubYStart=0;
                int VerSubYEnd=0;
                int VerSubWidth=width;
                int VerSubHeight=0;
                // 下标为偶数，即奇数为子图开始点
                if(i%2 == 0) {
                    //Log.d(TAG, "VerDividePositionlist i:" +i);

                    //子图的开始和结束点
                    VerSubYStart = VerDividePositionlist.get(i);
                    VerSubYEnd = VerDividePositionlist.get(i + 1);
                    //子图的高度
                    VerSubHeight = VerSubYEnd - VerSubYStart;

                    // 分割绘制垂直方向的子图
                    Bitmap subVerBitmap = Bitmap.createBitmap(binariBitmap, VerSubXStart, VerSubYStart, VerSubWidth, VerSubHeight);
                    //subVerBitmapArray.add(subVerBitmap);
                    //Log.d(TAG, "subVerBitmapIndex:"+subVerBitmapIndex+" VerSubXStart: "+VerSubXStart+" VerSubYStart: " +VerSubYStart +" VerSubWidth: "+VerSubWidth+" VerSubHeight: " +VerSubHeight);

                    // 获取垂直方向对应子图的 水平方向分割点
                    ArrayList<Integer> HorDividePositionlist = getHorizontalDividePositionArray(getHorBlackPointsHistogram(subVerBitmap));
                    //Log.d(TAG, "HorDividePositionlist.size():"+HorDividePositionlist.size());
                    //水平方向子图序号，即列号
                    int subHorBitmapIndex = 0;
                    // 水平方向有分割点
                    if (HorDividePositionlist.size() == 0) {
                        //Log.d(TAG, "子图水平方向不需要再分割");

                        if (isValidDigit(subVerBitmap)) {
                            // RectF(0-left, 1-top, 2-right, 3-bottom)
                            //子图最小外接矩形坐标

                            int minRectLeft = 0;
                            int minRectTop = VerSubYStart;
                            int minRectRight = VerSubWidth;
                            int minRectBottom = VerSubYStart + VerSubHeight;
                            //Log.d(TAG, "最小外接矩形在原图的位置：minRectLeft:" + minRectLeft + " minRectTop: " + minRectTop + " minRectRight: " + minRectRight + " minRectBottom: " + minRectBottom);

                            //保存子图最小外接矩形坐标
                            ArrayList<Integer> minRectBoundingPositionArrayList = new ArrayList<Integer>();
                            minRectBoundingPositionArrayList.add(minRectLeft);
                            minRectBoundingPositionArrayList.add(minRectTop);
                            minRectBoundingPositionArrayList.add(minRectRight);
                            minRectBoundingPositionArrayList.add(minRectBottom);
                            allMinRectBoundingPositionArrayList.add(minRectBoundingPositionArrayList);

                            //ArrayList<Integer> temp = allMinRectBoundingPositionArrayList.get(0);
                            //canvas.drawRect(new RectF(temp.get(0), temp.get(1), temp.get(2), temp.get(3)), paint);

                            // 存放行号和子图
                            subBitmapArrayListIndex.add(subVerBitmapIndex + 1);
                            subBitmapArrayList.add(subVerBitmap);
                        }
                    }
                    else {
                        // 基于垂直方向的子图，再进行水平方向分割子图
                        for (int j = 0; j < HorDividePositionlist.size()-1; j++) {

                            int HorSubXStart = 0;
                            int HorSubYStart = 0;
                            int HorSubXEnd = 0;
                            int HorSubWidth = 0;
                            int HorSubHeight = VerSubHeight;
                            // 下标为偶数为子图开始点
                            if (j % 2 == 0) {
                                //Log.d(TAG, "HorDividePositionlist j: " + j);
                                //水平方向子图的开始和结束点
                                HorSubXStart = HorDividePositionlist.get(j);
                                HorSubXEnd = HorDividePositionlist.get(j+1);
                                //子图的宽度
                                HorSubWidth = HorSubXEnd - HorSubXStart;
                                //Log.d(TAG, "subHorBitmapIndex:"+subHorBitmapIndex+" HorSubXStart:"+HorSubXStart+" HorSubYStart: "+HorSubYStart+ " HorSubWidth: "+HorSubWidth +" HorSubHeight: "+ HorSubHeight);

                                // 绘制水平方向的子图
                                Bitmap subHorBitmap = Bitmap.createBitmap(subVerBitmap, HorSubXStart, HorSubYStart, HorSubWidth, HorSubHeight);

                                // 子图 针对原二值化图像获取垂直方向累积黑点直方图数组
                                int[] subVerBlackPointsHistogram = getVerBlackPointsHistogram(subHorBitmap);
                                // 子图 针对垂直方向累积黑点直方图数组获取 垂直方向分割点
                                ArrayList<Integer> subVerDividePositionlist = getVerticalDividePositionArray(subVerBlackPointsHistogram);
                                //Log.d(TAG, "subVerDividePositionlist.size():"+subVerDividePositionlist.size());

                                if(subVerDividePositionlist.size() == 0) {
                                    Log.d(TAG, "子图垂直方向不需要再分割");

                                    if (isValidDigit(subHorBitmap)) {
                                        // RectF(0-left, 1-top, 2-right, 3-bottom)
                                        int minRectLeft = HorSubXStart;
                                        int minRectTop = VerSubYStart;
                                        int minRectRight = HorSubXStart + HorSubWidth;
                                        int minRectBottom = VerSubYStart + HorSubHeight;
                                        //Log.d(TAG, "最小外接矩形在原图的位置：minRectLeft:" + minRectLeft + " minRectTop: " + minRectTop + " minRectRight: " + minRectRight + " minRectBottom: " + minRectBottom);
                                        //保存子图最小外接矩形坐标
                                        ArrayList<Integer> minRectBoundingPositionArrayList = new ArrayList<Integer>();
                                        minRectBoundingPositionArrayList.add(minRectLeft);
                                        minRectBoundingPositionArrayList.add(minRectTop);
                                        minRectBoundingPositionArrayList.add(minRectRight);
                                        minRectBoundingPositionArrayList.add(minRectBottom);
                                        allMinRectBoundingPositionArrayList.add(minRectBoundingPositionArrayList);

                                        //canvas.drawRect(new RectF(minRectLeft, minRectTop, minRectRight, minRectBottom), paint);

                                        // 存放行号和子图
                                        subBitmapArrayListIndex.add(subVerBitmapIndex + 1);
                                        subBitmapArrayList.add(subHorBitmap);
                                    }
                                    subHorBitmapIndex++;
                                }
                                else {
                                    //分割子图，子图再垂直方向分割，取出最小外接矩形
                                    for (int h = 0; h < subVerDividePositionlist.size() - 1; h++) {
                                        int subVerSubXStart = 0;
                                        int subVerSubXEnd = 0;
                                        int subVerSubWidth = 0;
                                        int subVerSubYStart = 0;
                                        int subVerSubYEnd = 0;
                                        int subVerSubHeight = 0;
                                        // 下标为偶数，即奇数为子图开始点
                                        if (h % 2 == 0) {
                                            //Log.d(TAG, "subVerDividePositionlist h:" + h);

                                            //最小外接矩形子图的开始和结束点
                                            subVerSubXStart = 0;
                                            subVerSubWidth = HorSubWidth;
                                            subVerSubYStart = subVerDividePositionlist.get(h);
                                            subVerSubYEnd = subVerDividePositionlist.get(h + 1);
                                            //子图的高度
                                            subVerSubHeight = subVerSubYEnd - subVerSubYStart;

                                            //Log.d(TAG, "最小外接矩形, subVerSubXStart:" + subVerSubXStart + " subVerSubYStart: " + subVerSubYStart + " subVerSubWidth: " + subVerSubWidth + " subVerSubHeight: " + subVerSubHeight);

                                            // 最小外接矩形边框
                                            Bitmap subMinBitmapTemp = Bitmap.createBitmap(subHorBitmap, subVerSubXStart, subVerSubYStart, subVerSubWidth, subVerSubHeight);

                                            // 判断是否有效数字
                                            if (isValidDigit(subMinBitmapTemp)) {

                                                // 在原二值图像上，绘制每个子图的红颜色最小外接矩形边框
                                                // RectF(0-left, 1-top, 2-right, 3-bottom)
                                                int minRectLeft = HorSubXStart;
                                                int minRectTop = VerSubYStart + subVerSubYStart;
                                                int minRectRight = HorSubXStart + subVerSubWidth;
                                                int minRectBottom = VerSubYStart + subVerSubYStart + subVerSubHeight;
                                                //Log.d(TAG, "最小外接矩形在原图的位置：minRectLeft:" + minRectLeft + " minRectTop: " + minRectTop + " minRectRight: " + minRectRight + " minRectBottom: " + minRectBottom);
                                                //保存子图最小外接矩形坐标
                                                ArrayList<Integer> minRectBoundingPositionArrayList = new ArrayList<Integer>();
                                                minRectBoundingPositionArrayList.add(minRectLeft);
                                                minRectBoundingPositionArrayList.add(minRectTop);
                                                minRectBoundingPositionArrayList.add(minRectRight);
                                                minRectBoundingPositionArrayList.add(minRectBottom);
                                                allMinRectBoundingPositionArrayList.add(minRectBoundingPositionArrayList);
                                                //canvas.drawRect(new RectF(minRectLeft, minRectTop, minRectRight, minRectBottom), paint);

                                                // 存放行号和子图
                                                subBitmapArrayListIndex.add(subVerBitmapIndex + 1);
                                                subBitmapArrayList.add(subMinBitmapTemp);

                                                //Log.d(TAG, "subHorBitmapIndex:" + subHorBitmapIndex + " subBitmapTemp：" + subMinBitmapTemp + " subBitmapArrayList:" + subBitmapArrayList.get(subHorBitmapIndex));

                                                //Log.d(TAG, "子图垂直方向序号：" + subVerBitmapIndex + " ，水平方向序号: " + subHorBitmapIndex + " 分割结束！");
                                                subHorBitmapIndex++;

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        subVerBitmapIndex++;
                    }

                }
            }


            Log.d(TAG, "getSubBitmapArrayList END");
            // 返回有效子图
            //return getValidsubBitmapArrayList(subBitmapArrayList);
            // 返回所有分割后子图列表
            return subBitmapArrayList;
        }
    }


    /**
     * drawMinRectBoundingBitmap
     * 输入一张二值图片，输入所有子图最小外接矩形坐标列表，在输入图像上绘制所有子图的最小外接矩形。
     * @param binariBitmap
     * @return　Map数组。
     */
    public Bitmap drawMinRectBoundingBitmap(Bitmap binariBitmap, ArrayList<ArrayList<Integer>> allMinRectBoundingArrayList) {

        Log.d(TAG, "drawMinRectBoundingBitmap START");

        Log.d(TAG, "allMinRectBoundingArrayList.size(): " + allMinRectBoundingArrayList.size());
        if(allMinRectBoundingArrayList.size() == 0) {
            //直接返回输入的图像
            return binariBitmap;
        }
        else{

            int width = binariBitmap.getWidth();
            int height = binariBitmap.getHeight();


            //复制图片
            Bitmap rectBoundingBitmap = binariBitmap.copy(Bitmap.Config.ARGB_8888, true);
            // 绘制红颜色最小外接矩形边框，空心 红色 宽度
            Canvas canvas = new Canvas(rectBoundingBitmap);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            int strokeWidth=2;
            if (width > height) {
                strokeWidth = (int) (width*0.005);
            }
            else{
                strokeWidth = (int) (height*0.005);
            }
            if (strokeWidth == 0)
                strokeWidth =1;

            Log.d(TAG, "strokeWidth:"+strokeWidth);
            paint.setStrokeWidth(strokeWidth);

            for (int index = 0; index < allMinRectBoundingArrayList.size(); index++) {
                //获取每个子图的最小外接矩形坐标
                ArrayList<Integer> minRectBoundingArrayList = allMinRectBoundingArrayList.get(index);
                // RectF(0-left, 1-top, 2-right, 3-bottom)
                int minRectLeft = minRectBoundingArrayList.get(0);
                int minRectTop = minRectBoundingArrayList.get(1);
                int minRectRight = minRectBoundingArrayList.get(2);
                int minRectBottom = minRectBoundingArrayList.get(3);
                canvas.drawRect(new RectF(minRectLeft, minRectTop, minRectRight, minRectBottom), paint);
            }

            return rectBoundingBitmap;

        }
    }

    /**
     * toEqualizedHist()
     * 输入一张灰色图片，获取灰度直方图，返回直方图均衡化图。
     * @param grayBitmap
     * @return　处理后的图片。
     */
    public Bitmap toEqualizedHist(Bitmap grayBitmap) {

        Log.d(TAG, "toEqualizedHist START");
        int width = grayBitmap.getWidth();
        int height = grayBitmap.getHeight();

        Bitmap equalizedHistBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        //获取灰度直方图
        int[] histogram = new int[256];
        double[] equalizeHistogram = new double[256];
        for(int i=0; i<histogram.length; i++) {
            histogram[i] = 0;
            equalizeHistogram[i] = 0;
        }

        int grayColor = 0, gray = 0;
        int[][] grayArray = new int[width][height];
        int[][] alphaArray = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //获得每一个位点的颜色
                grayColor = grayBitmap.getPixel(i, j);
                //获得三原色 R, 灰度图 R=B=G
                gray = Color.red(grayColor);
                alphaArray[i][j] = Color.alpha(grayColor);
                grayArray[i][j] = gray;
                //Log.d(TAG, " gray:" + red);
                histogram[gray]++;
            }
        }

        // 直方图均衡化
        int gray_temp1 = 0;
        int gray_temp2 = 0;
        double a = (double)255/(width*height);
        equalizeHistogram[0] = (a*histogram[0]);
        for(int i=1; i<256; i++){
            equalizeHistogram[i] = equalizeHistogram[i-1] + (int)(a*histogram[i]);
        }

        for(int h=0;h<height;h++){
            for(int w=0;w<width;w++)
            {
                gray_temp1 = grayArray[w][h];
                gray_temp2 = (int)(equalizeHistogram[gray_temp1]);
                equalizedHistBitmap.setPixel(w, h, Color.argb(alphaArray[w][h], gray_temp2, gray_temp2, gray_temp2));
            }
        }
        Log.d(TAG, "toEqualizedHist END");
        return equalizedHistBitmap;

    }


    /**
     * getBestThreshold()
     * 输入灰度直方图，迭代求得最佳全局阈值，返回最佳阈值
     * @param histogram
     * @return　最佳阈值
     */
    public static int getBestThreshold(int[] histogram)
    {
        Log.d(TAG, "getBestThreshold START");
        int i, index = 0;
        int fgroudCount=0, bgroundCount=0;
        int fgroundAvg=0, bgroundAvg=0;

        // 对比较大的图像，可能会超过int最大值，导致出现异常，所以定义成long
        long fgroundSum, bgroundSum;
        int minGray, maxGray;
        int threshold, newThreshold;

        // 从左找第一个非0，即最小值
        for (minGray = 0; minGray < 256 && histogram[minGray] == 0; minGray++) ;

        // 从右找第一个非0，即最大值
        for (maxGray = 255; maxGray > minGray && histogram[maxGray] == 0; maxGray--) ;
        //Log.d(TAG, "minGray: "+minGray+"  maxGray:"+maxGray);

        // 图像中只有一个颜色
        if (maxGray == minGray)
            return maxGray;
        // 图像中只有二个颜色
        if (minGray + 1 == maxGray)
            return minGray;

        // 如果偏纯色，无色差，认为二值化阈值为128
        if( minGray >= 200 || maxGray <= 50 )
            return 128;

        // 最小值
        threshold = minGray;
        // 取最大最小均值
        newThreshold = (maxGray + minGray) >> 1;
        // 当前后两次迭代的获得阈值相同时，结束迭代
        while (threshold != newThreshold) {
            //Log.d(TAG, "threshold: "+threshold+" newThreshold: "+newThreshold);
            bgroundCount = 0;
            bgroundSum = 0;
            fgroudCount = 0;
            fgroundSum = 0;

            threshold = newThreshold;
            //Log.d(TAG, "threshold: "+threshold);
            //根据阈值将图像分割成前景目标和背景两部分，求出两部分的平均灰度值
            for (i = minGray; i <= threshold; i++)
            {
                //前景像素点的灰度总值
                fgroundSum += histogram[i] * i;
                //前景像素点的总个数
                fgroudCount += histogram[i];

                //Log.d(TAG, "down minGray i: "+i+" backgroudCount:"+backgroudCount+" backgroundSum:"+backgroundSum);

            }
            //排除除零异常
            if(fgroudCount > 0) {
                //前景像素点的平均灰度值
                fgroundAvg = (int) (fgroundSum / fgroudCount);
            }
            for (i = threshold + 1; i <= maxGray; i++)
            {
                //背景像素点的灰度总值
                bgroundSum += histogram[i] * i;
                //背景像素点的总个数
                bgroundCount += histogram[i];

                //Log.d(TAG, "up minGray i: "+i+" foregroundCount:"+foregroundCount+" foregroundSum:"+foregroundSum);
            }
            //排除除零异常
            if(bgroundCount > 0) {
                //背景像素点的平均灰度值
                bgroundAvg = (int) (bgroundSum / bgroundCount);
            }
            //求出新的阈值 (f+b)/2
            newThreshold = (fgroundAvg + bgroundAvg) >> 1;
            index++;
            //Log.d(TAG, "fgroundAvg:"+fgroundAvg+" bgroundAvg:"+bgroundAvg+" newThreshold:"+newThreshold+" index:"+index);

            if (index >= 1000)
                return -1;
        }
        finalBlackCount = fgroudCount;
        finalWhiteCount = bgroundCount;

        Log.d(TAG, "finalBlackCount:"+finalBlackCount+" finalWhiteCount："+finalWhiteCount+" threshold: "+threshold);

        Log.d(TAG, "getBestThreshold END");
        return threshold;
    }

    /**
     * toBinarization()
     * 输入一张灰色图片，获取直方图，前景背景，求最佳阈值，二值化，返回白底黑字二值化图片。
     * @param grayBitmap
     * @return　处理后的图片。
     */
    public Bitmap toBinarization(Bitmap grayBitmap) {

        Log.d(TAG, "toBinarization START");

        // Turn the picture black and white
        int width = grayBitmap.getWidth();
        int height = grayBitmap.getHeight();

        Bitmap binariBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] histogram = getHistogramArray(grayBitmap);
        bestThreshold = getBestThreshold(histogram); //最佳阈值
        Log.d(TAG, "bestThreshold: "+bestThreshold);

        //二值化
        //int white_c=0, black_c=0;
        int currentPixel=0, gray=0, alpha =0;

        //二值化时统一成白底黑字图像
        if(finalWhiteCount > finalBlackCount) {
            Log.i(TAG,"原图是白底黑字，不需要反色");
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {

                    //获得每一个位点的颜色
                    currentPixel = grayBitmap.getPixel(w, h);
                    //获得三原色
                    gray = Color.red(currentPixel);
                    alpha = Color.alpha(currentPixel);
                    //Log.d(TAG, "gray：" + gray);

                    if (gray >= bestThreshold) {
                        //设置颜色
                        // white
                        binariBitmap.setPixel(w, h, Color.argb(alpha, 255, 255, 255));
                        //white_c++;
                        //Log.d(TAG, "gray >= bestThreshold");
                    } else {
                        // black
                        binariBitmap.setPixel(w, h, Color.argb(alpha, 0, 0, 0));
                        //black_c++;
                        //Log.d(TAG, "gray < bestThreshold");
                    }

                    //Log.d(TAG, "binariBitmap w: "+w+" h: "+h+" "+binariBitmap.getPixel(w,h));
                }
            }
        }
        else {
            Log.i(TAG,"原图是黑底白字，需要反色");
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {

                    //获得每一个位点的颜色
                    currentPixel = grayBitmap.getPixel(w, h);
                    //获得三原色
                    gray = Color.red(currentPixel);
                    alpha = Color.alpha(currentPixel);
                    //Log.d(TAG, "gray：" + gray);

                    if (gray < bestThreshold) {
                        //设置颜色
                        // white
                        binariBitmap.setPixel(w, h, Color.argb(alpha, 255, 255, 255));
                        //white_c++;
                        //Log.d(TAG, "gray >= bestThreshold");
                    } else {
                        // black
                        binariBitmap.setPixel(w, h, Color.argb(alpha, 0, 0, 0));
                        //black_c++;
                        //Log.d(TAG, "gray < bestThreshold");
                    }

                    //Log.d(TAG, "binariBitmap w: "+w+" h: "+h+" "+binariBitmap.getPixel(w,h));
                }
            }
        }

        //finalBlackCount = black_c;
        //finalWhiteCount = white_c;

        //Log.d(TAG, " Count of White: "+white_c+" Count of black: "+black_c);
        //消除孤立点
        //return toRemoveNoise(binariBitmap);
        Log.d(TAG, "toBinarization END");
        return binariBitmap;

    }

    /**
     * getRoundPixel()
     * 输入一张二值化图片，返回当前像素点周围的8个是否包含黑色数组，则黑点设置为true，白点为false
     * @param binariBitmap, x, y
     * @return isBlackPixels
     */
    public boolean[] getRoundPixel(Bitmap binariBitmap, int x, int y)
    {
        //Log.d(TAG, "getRoundPixel START");

        boolean[] isBlackPixels = new boolean[8];
        // 0 3 5
        // 1 x 6
        // 2 4 7
        int currentPixel = 0, gray = 0;
        int num = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                currentPixel = binariBitmap.getPixel(x+i,y+j);

                //获得三原色 R, 灰度图 R=B=G
                //由于经过了二值化，所以仅仅要检查RGB中一个属性的值
                gray = Color.red(currentPixel);

                if (i != 0 || j != 0) {

                    if (gray == 0) {
                        //为黑色，设置为true
                        isBlackPixels[num] = true;
                    }
                    else if(gray == 255) {
                        //为白色，设置为false
                        isBlackPixels[num] = false;
                    }
                    num++;
                }
            }
        }
        //Log.d(TAG, "getRoundPixel END");
        return isBlackPixels;
    }

    /**
     * getRoundFourPixel()
     * 输入一张二值化图片，返回当前像素点周围的上下左右4个是否包含黑色数组，则黑点设置为true，白点为false
     * @param binariBitmap, x, y
     * @return isBlackPixels
     */
    public boolean[] getRoundFourPixel(Bitmap binariBitmap, int x, int y)
    {
        //Log.d(TAG, "getRoundFourPixel START");

        boolean[] isBlackPixels = new boolean[4];
        // 0 3 5
        // 1 x 6
        // 2 4 7
        int currentPixel = 0, gray = 0;
        int num = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                currentPixel = binariBitmap.getPixel(x+i,y+j);

                //获得三原色 R, 灰度图 R=B=G
                //由于经过了二值化，所以仅仅要检查RGB中一个属性的值
                gray = Color.red(currentPixel);
                if ( i==0 && j==0 || i==-1 && j==-1 || i == 1 && j == 1 || i == 1 && j == -1 || i == -1 && j == 1) {

                }
                //if ((i != -1 && j != -1) || (i != 1 && j != 1) || (i != 1 && j != -1) || (i != -1 && j != 1)) {
                else {
                    //Log.d(TAG, "i:"+i+" j:" +j);
                    if (gray == 0) {
                        //为黑色，设置为true
                        isBlackPixels[num] = true;
                    }
                    else if(gray == 255) {
                        //为白色，设置为false
                        isBlackPixels[num] = false;
                    }
                    num++;
                }
            }
        }
        //Log.d(TAG, "getRoundFourPixel END");
        return isBlackPixels;
    }

    /**
     * getRoundXXYPixel()
     * 输入一张二值化图片，返回当前像素点周围的上下2个点 左右4个点，包含黑色点总数，
     * @param binariBitmap, x, y
     * @return isBlackPixels
     */
    public int getRoundXXYPixel(Bitmap binariBitmap, int x, int y)
    {
        //Log.d(TAG, "getRoundFourPixel START");
        int BlackPixels = 0;

        //  0  0  1  0  0
        // -1 -1  0 -1 -1
        //  0  0  1  0  0

        for (int i = -1; i <= 1; i++) {
            //垂直方向累加
            if (Color.red(binariBitmap.getPixel(x, y + i)) == 0) {
                BlackPixels++;
            }
        }

        for (int i = -2; i <= 2; i++) {
                //水平方向累加
                if (Color.red(binariBitmap.getPixel(x+i, y)) == 0) {
                    BlackPixels--;
                }
        }

        //Log.d(TAG, "getRoundFourPixel END");
        return BlackPixels;
    }

    /**
     * toRemoveNoise()
     * 输入一张二值化图片，找到孤立黑点（周围超过3个白点），把该黑点修改成白点，返回处理后的图片
     * @param binariBitmap
     * @return isBlackPixels
     */
    public Bitmap toRemoveNoise(Bitmap binariBitmap)
    {
        Log.d(TAG, "toRemoveNoise START");

        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        int currentPixel = 0, gray = 0, alpha = 0;
        //复制图片
        Bitmap toRemoveNoiseBitmap = binariBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //Bitmap expendBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // only this line is for white
        //Log.i(TAG,"expendBitmap Config:"+expendBitmap+" Width:"+expendBitmap.getWidth()+" Heigth:"+expendBitmap.getHeight());

        boolean[] isBlackPixels;

        for (int w = 1; w < width - 1; w++) {
            for (int h = 1; h < height - 1; h++) {
                currentPixel = binariBitmap.getPixel(w, h);
                alpha = Color.alpha(currentPixel);
                gray = Color.red(currentPixel);

                // 如果当前点为黑色，判断周围是否有超过3个黑色点，有就把当前黑色改为白色
                if (gray == 0) {
                    //获取当前点周围的黑点
                    isBlackPixels = getRoundPixel(binariBitmap, w, h);
                    //Log.i(TAG,"isBlackPixels:"+Arrays.toString(isBlackPixels));
                    int numWhite = 0;
                    for (int k = 0; k < isBlackPixels.length; k++) {
                        // 8领域白点个数
                        if (isBlackPixels[k] == false) {
                            numWhite++;
                        }
                    }
                    //Log.i(TAG,"NumWhite:"+numWhite);
                    // 超过3个白点，认为是孤立点，删除即把当前黑点改为白色

                    if (numWhite > 3) {
                        //Log.i(TAG,"remove:");
                        toRemoveNoiseBitmap.setPixel(w, h, Color.argb(alpha, 255, 255, 255));

                    }
                }
            }
        }

        Log.i(TAG,"toRemoveNoiseBitmap Config:"+" Width:"+toRemoveNoiseBitmap.getWidth()+" Heigth:"+toRemoveNoiseBitmap.getHeight());
        Log.d(TAG, "toRemoveNoise END");
        return toRemoveNoiseBitmap;
    }


    /**
     * toExpend()
     * 输入一张二值化图片，返回膨胀后的图片
     * @param binariBitmap
     * @return isBlackPixels
     */
    public Bitmap toExpend(Bitmap binariBitmap)
    {
        Log.d(TAG, "toExpend START");

        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        int currentPixel = 0, gray = 0, alpha = 0;
        //复制图片
        Bitmap expendBitmap = binariBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //Bitmap expendBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // only this line is for white
        Log.i(TAG,"expendBitmap Config:"+" Width:"+expendBitmap.getWidth()+" Heigth:"+expendBitmap.getHeight());

        boolean[] isBlackPixels;
        for (int w = 1; w < width-1; w++)
        {
            //Log.i(TAG,"expendBitmap W:"+w);
            for (int h = 1; h < height-1; h++)
            {
                //Log.i(TAG,"expendBitmap H:"+h);
                currentPixel = binariBitmap.getPixel(w, h);
                alpha = Color.alpha(currentPixel);
                gray = Color.red(currentPixel);

                // 如果当前点为白色，判断周围是否有黑点，有就把当前白点改为黑色
                if (gray == 255)
                {
                    //获取当前点周围的黑点
                    isBlackPixels = getRoundPixel(binariBitmap, w, h);
                    //isBlackPixels = getRoundFourPixel(binariBitmap, w, h);
                    //Log.i(TAG,"isBlackPixels:"+Arrays.toString(isBlackPixels));
                    for (int k = 0; k < isBlackPixels.length; k++)
                    {
                        //Log.i(TAG,"expendBitmap isBlack? k:"+isBlackPixels[k]);
                        // 只要该像素点的八领域有黑点，即把该像素点修改成黑色
                        if (isBlackPixels[k] == true)
                        {
                            //set this piexl's color to black
                            expendBitmap.setPixel(w, h, Color.argb(alpha, 0, 0, 0));
                            //Log.i(TAG,"expendBitmap: isBlack! k:"+k);
                            break;
                        }
                    }
                }
            }
        }


        Log.d(TAG, "toExpend END");
        return expendBitmap;

    }

    /**
     * toExpendFour()
     * 输入一张二值化图片，返回膨胀后的图片
     * @param binariBitmap
     * @return isBlackPixels
     */
    public Bitmap toExpendFour(Bitmap binariBitmap)
    {
        Log.d(TAG, "toExpend START");

        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        int currentPixel = 0, gray = 0, alpha = 0;
        //复制图片
        Bitmap expendBitmap = binariBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //Bitmap expendBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // only this line is for white
        Log.i(TAG,"expendBitmap Config:"+" Width:"+expendBitmap.getWidth()+" Heigth:"+expendBitmap.getHeight());

        boolean[] isBlackPixels;
        for (int w = 1; w < width-1; w++)
        {
            //Log.i(TAG,"expendBitmap W:"+w);
            for (int h = 1; h < height-1; h++)
            {
                //Log.i(TAG,"expendBitmap H:"+h);
                currentPixel = binariBitmap.getPixel(w, h);
                alpha = Color.alpha(currentPixel);
                gray = Color.red(currentPixel);

                // 如果当前点为白色，判断周围是否有黑点，有就把当前白点改为黑色
                if (gray == 255)
                {
                    //获取当前点周围的黑点
                    //isBlackPixels = getRoundPixel(binariBitmap, w, h);
                    isBlackPixels = getRoundFourPixel(binariBitmap, w, h);
                    //Log.i(TAG,"isBlackPixels:"+Arrays.toString(isBlackPixels));
                    for (int k = 0; k < isBlackPixels.length; k++)
                    {
                        //Log.i(TAG,"expendBitmap isBlack? k:"+isBlackPixels[k]);
                        // 只要该像素点的八领域有黑点，即把该像素点修改成黑色
                        if (isBlackPixels[k] == true)
                        {
                            //set this piexl's color to black
                            expendBitmap.setPixel(w, h, Color.argb(alpha, 0, 0, 0));
                            //Log.i(TAG,"expendBitmap: isBlack! k:"+k);
                            break;
                        }
                    }
                }
            }
        }


        Log.d(TAG, "toExpend END");
        return expendBitmap;

    }

    /**
     * toCorrode()
     * 输入一张二值化图片，返回膨胀后的图片
     * @param expendBitmap
     * @return isBlackPixels
     */
    public Bitmap toCorrode(Bitmap expendBitmap)
    {
        Log.d(TAG, "toCorrode START");

        int width = expendBitmap.getWidth();
        int height = expendBitmap.getHeight();
        int currentColor = 0, gray = 0, alpha = 0;
        Bitmap corrodeBitmap = expendBitmap.copy(Bitmap.Config.ARGB_8888, true);
        boolean[] isBlackPixels;
        for (int w = 1; w < width-1; w++)
        {
            for (int h = 1; h < height-1; h++)
            {
                currentColor = expendBitmap.getPixel(w, h);
                alpha = Color.alpha(currentColor);
                gray = Color.red(currentColor);

                // 如果当前点为黑色，判断周围是否有白点，有就把当前黑点改为白色
                if (gray == 0)
                {
                    isBlackPixels = getRoundPixel(expendBitmap, w, h);
                    //Log.i(TAG,"isBlackPixels:"+Arrays.toString(isBlackPixels));
                    for (int k = 0; k < isBlackPixels.length; k++)
                    {
                        if (isBlackPixels[k] == false)
                        {
                            //set this piexl's color to black
                            corrodeBitmap.setPixel(w, h, Color.argb(alpha, 255, 255, 255));
                            break;
                        }
                    }
                }
            }
        }
        //消除孤立点
        //return toRemoveNoise(corrodeBitmap);
        Log.i(TAG,"corrodeBitmap Config:"+" Width:"+corrodeBitmap.getWidth()+" Heigth:"+corrodeBitmap.getHeight());

        Log.d(TAG, "toCorrode END");
        return corrodeBitmap;
    }

    /**
     * toInverse()
     * 输入一张二值化图片，进行反色处理，白色变成黑色，黑色变成白色，返回处理后的图片
     * @param binariBitmap
     * @return isBlackPixels
     */
    public Bitmap toInverse(Bitmap binariBitmap)
    {
        Log.d(TAG, "toInverse START");
        int width = binariBitmap.getWidth();
        int height = binariBitmap.getHeight();
        int currentColor = 0, gray = 0, alpha = 0, newValue = 0;
        Bitmap inverseBitmap = binariBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //boolean[] isBlackPixels;
        for (int w = 0; w < width; w++)
        {
            for (int h = 0; h < height; h++)
            {
                currentColor = binariBitmap.getPixel(w, h);
                alpha = Color.alpha(currentColor);
                gray = Color.red(currentColor);

                // 如果当前点为黑色，判断周围是否有白点，有就把当前黑点改为白色
                if (gray == 0)
                {
                    newValue = 255;
                }
                else if (gray == 255)
                {
                    newValue = 0;
                }
                inverseBitmap.setPixel(w, h, Color.argb(alpha, newValue, newValue, newValue));
            }
        }

        Log.d(TAG, "toInverse END");
        //消除孤立点
        //return toRemoveNoise(corrodeBitmap);
        return inverseBitmap;
    }

    /**
     * edgeDetectByRoberts()
     * 输入一张灰度图片，返回边缘检测图片。
     * @param grayBitmap
     * @return robertsEDBitmap
     */
    public Bitmap toDetectEdgeByRoberts(Bitmap grayBitmap) {

        Log.d(TAG, "toDetectEdgeByRoberts START");

        int width = grayBitmap.getWidth();
        int height = grayBitmap.getHeight();
        Log.d(TAG, " W,h:"+width+height);
        int currentColor, alpha = 0;
        Bitmap robertsEDBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        double Gx, Gy, tempSum = 0.00;
        int Gxy = 0;
        for ( int x = 0; x < width-1; x++) {
            for (int y = 0; y < height-1; y++) {
                //获得每一个位点的颜色
                currentColor = grayBitmap.getPixel(x, y);
                //获得三原色
                alpha = Color.alpha(currentColor);

                //计算GX = f(x,y)-f(x+1,y+1)) f(x,y)为x,y点的灰度值
                Gx = Color.red(grayBitmap.getPixel(x,y)) - Color.red(grayBitmap.getPixel(x+1,y+1));
                //计算GY = f(x,y+1)-f(x+1,y) f(x,y)为x,y点的灰度值
                Gy = Color.red(grayBitmap.getPixel(x,y+1)) - Color.red(grayBitmap.getPixel(x+1,y));
                tempSum = Math.pow(Gx,2) + Math.pow(Gy,2);
                Gxy = (int) Math.sqrt(tempSum);
                if(Gxy > 255) Gxy = 255;
                robertsEDBitmap.setPixel(x,y,Color.argb(alpha, Gxy, Gxy, Gxy));
                //Log.d(TAG, " x,y:"+x+","+y);
                //Log.d(TAG, " Gxy:"+Gxy);
            }
        }

        Log.d(TAG, "toDetectEdgeByRoberts END");
        return robertsEDBitmap;

    }

    /**
     * meanFilter()
     * 输入一张灰度图片，返回mean均值滤波图片。
     * @param bitmap
     * @return meanFilterBitmap
     */
    public Bitmap meanFilter(Bitmap bitmap) {

        Log.d(TAG, "meanFilter START");

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, " W,h:"+width+height);
        int currentColor, alpha = 0;
        Bitmap meanFilterBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int Gxy = 0;
        for ( int x = 1; x < width-1; x++) {
            for (int y = 1; y < height-1; y++) {
                //获得每一个位点的颜色
                currentColor = bitmap.getPixel(x, y);
                //获得三原色
                alpha = Color.alpha(currentColor);

                int[] meanArray = new int[9];
                meanArray[0] = Color.red(bitmap.getPixel(x-1,y-1));
                meanArray[1] = Color.red(bitmap.getPixel(x-1,y));
                meanArray[2] = Color.red(bitmap.getPixel(x-1,y+1));
                meanArray[3] = Color.red(bitmap.getPixel(x,y-1));
                meanArray[4] = Color.red(bitmap.getPixel(x,y));
                meanArray[5] = Color.red(bitmap.getPixel(x,y+1));
                meanArray[6] = Color.red(bitmap.getPixel(x+1,y-1));
                meanArray[7] = Color.red(bitmap.getPixel(x+1,y));
                meanArray[8] = Color.red(bitmap.getPixel(x+1,y+1));

                int sum = 0;
                int mean = 0;
                for (int i=0; i < 9; i++) {
                    sum += meanArray[i];
                }
                mean = sum/9;
                meanFilterBitmap.setPixel(x,y,Color.argb(alpha, mean, mean, mean));
            }
        }

        Log.d(TAG, "meanFilter END");
        return meanFilterBitmap;
    }

    /**
     * midlleFilter()
     * 输入一张灰度图片，返回中值滤波图片。
     * @param bitmap
     * @return midlleFilterBitmap
     */
    public Bitmap middleFilter(Bitmap bitmap) {

        Log.d(TAG, "middleFilter START");
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, " W,h:"+width+height);
        int currentColor, alpha = 0;
        Bitmap middleFilterBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int Gxy = 0;
        for ( int x = 1; x < width-1; x++) {
            for (int y = 1; y < height-1; y++) {
                //获得每一个位点的颜色
                currentColor = bitmap.getPixel(x, y);
                //获得三原色
                alpha = Color.alpha(currentColor);

                int[] middleArray = new int[9];
                middleArray[0] = Color.red(bitmap.getPixel(x-1,y-1));
                middleArray[1] = Color.red(bitmap.getPixel(x-1,y));
                middleArray[2] = Color.red(bitmap.getPixel(x-1,y+1));
                middleArray[3] = Color.red(bitmap.getPixel(x,y-1));
                middleArray[4] = Color.red(bitmap.getPixel(x,y));
                middleArray[5] = Color.red(bitmap.getPixel(x,y+1));
                middleArray[6] = Color.red(bitmap.getPixel(x+1,y-1));
                middleArray[7] = Color.red(bitmap.getPixel(x+1,y));
                middleArray[8] = Color.red(bitmap.getPixel(x+1,y+1));

                Arrays.sort(middleArray);
                int middle = middleArray[4];

                middleFilterBitmap.setPixel(x,y,Color.argb(alpha, middle, middle, middle));

            }
        }

        Log.d(TAG, "middleFilter END");
        return middleFilterBitmap;
    }

    /**
     * gaussFilter()
     * 输入一张灰度图片，返回mean均值滤波图片。
     * @param bitmap
     * @return midlleFilterBitmap
     */
    public Bitmap gaussFilter(Bitmap bitmap) {

        Log.d(TAG, "gaussFilter START");

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d(TAG, " W,h:"+width+height);
        int currentColor, alpha = 0;
        //Bitmap gaussFilterBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap gaussFilterBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int Gxy = 0;
        for ( int x = 1; x < width-1; x++) {
            for (int y = 1; y < height-1; y++) {
                //获得每一个位点的颜色
                currentColor = bitmap.getPixel(x, y);
                //获得三原色
                alpha = Color.alpha(currentColor);

                int[] gaussArray = new int[9];
                gaussArray[0] = Color.red(bitmap.getPixel(x-1,y-1));
                gaussArray[1] = Color.red(bitmap.getPixel(x-1,y));
                gaussArray[2] = Color.red(bitmap.getPixel(x-1,y+1));
                gaussArray[3] = Color.red(bitmap.getPixel(x,y-1));
                gaussArray[4] = Color.red(bitmap.getPixel(x,y));
                gaussArray[5] = Color.red(bitmap.getPixel(x,y+1));
                gaussArray[6] = Color.red(bitmap.getPixel(x+1,y-1));
                gaussArray[7] = Color.red(bitmap.getPixel(x+1,y));
                gaussArray[8] = Color.red(bitmap.getPixel(x+1,y+1));

                int gauss =   gaussArray[0] + 2*gaussArray[1] +   gaussArray[2] +
                            2*gaussArray[3] + 4*gaussArray[4] + 2*gaussArray[5] +
                              gaussArray[6] + 2*gaussArray[7] +   gaussArray[8];
                gauss = gauss/16;

                gaussFilterBitmap.setPixel(x,y,Color.argb(alpha, gauss, gauss, gauss));

            }
        }

        Log.d(TAG, "gaussFilter END");
        return gaussFilterBitmap;
    }

}
