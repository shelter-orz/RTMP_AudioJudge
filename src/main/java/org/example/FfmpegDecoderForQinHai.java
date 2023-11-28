package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.PcmAnalyzeResultEnum.*;

public class FfmpegDecoderForQinHai extends Application implements Runnable {
    private final int sleepTime = 1000; // 停顿时间
    private static final int NUM_FRAMES = 25;
    private static final int NUM_SAMPLES = 1024;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_SIZE = 2;
    private static final int BYTES_PER_FRAME = NUM_CHANNELS * SAMPLE_SIZE * NUM_SAMPLES;
//    private static final int BYTES_PER_FRAME = 941;

    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static double[] oldData = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};


    private static ArrayList<byte[]> pcmList = new ArrayList<>();// 你的PCM数据列表
    private static ArrayList<Double> diffList = new ArrayList<>(); // 差异结果列表

    private static final int[] largeDropCounts = new int[NUM_FRAMES];
    private static int largeDropCountsIndex = 0;

    private static final double[] RMSEs = new double[NUM_FRAMES];
    private static int RMSEIndex = 0;
    private static String result = null;
    private static String lastResult = null;
    private static String rtmpUrl = "rtmp://192.168.8.147:1935/hlsram/live21";
//    private static String rtmpUrl = "rtmp://192.168.100.170:1935/hlsram/live16";
//    rtmp://192.168.100.170:1935/hlsram/live16
//    rtmp://123.112.214.255/live/3000122134_0_2_17740000
//    rtmp://123.112.214.255/live/3000122134_0_4_747000
//    rtmp://123.112.214.255/live/3000122134_0_0_15000000
//    rtmp://123.112.214.255/live/3000122134_0_1_6046000
//    rtmp://123.112.214.255/live/3000122134_0_6_1008000

    private static String ffmpegLocation = "D:\\软件\\测试工具\\Ffmpeg\\bin\\ffmpeg.exe";
//    private static String ffmpegLocation = "C:\\Develop\\Ffmpeg\\bin\\ffmpeg.exe";

    private static String potPlayerPath = "C:\\工具\\PotPlayer\\PotPlayer64\\PotPlayerMini64.exe";

    private static TimeBasedCache<Long, PcmAnalyzeResultEnum> timeBasedCache = new TimeBasedCache<>(5, TimeUnit.MINUTES);


    private volatile boolean running = true;





    private static double sampleRate = 48000;
    private static int fftSize;
    private static volatile double[] magnitudes;
    private static volatile double[] frequencies;

    public static double[] getMagnitudes() {
        return magnitudes;
    }

    public static double[] getFrequencies() {
        return frequencies;
    }

    public static void setRtmpUrl(String rtmpUrl) {
        FfmpegDecoderForQinHai.rtmpUrl = rtmpUrl;
    }

    public static void setFfmpegLocation(String ffmpegLocation) {
        FfmpegDecoderForQinHai.ffmpegLocation = ffmpegLocation;
    }

    public static void setPotPlayerPath(String potPlayerPath) {
        FfmpegDecoderForQinHai.potPlayerPath = potPlayerPath;
    }

    public void stopRunning() {
        this.running = false;
    }
    public static void main(String[] args) {
        launch(args);
    }

    private XYSeries series = new XYSeries("FFT Analysis");
    private XYSeriesCollection dataset = new XYSeriesCollection(series);

    @Override
    public void start(Stage stage) {
        Thread thread = new Thread(this);
        thread.start();
        final SwingNode chartSwingNode = new SwingNode();
        createChart(chartSwingNode);

        StackPane pane = new StackPane();
        pane.getChildren().add(chartSwingNode);

        stage.setTitle("JavaFX / JFreeChart Integration");
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();

        // Start a new Thread which updates the chart
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);  // Adjust based on your needs

                    double[] newFrequencies = frequencies;  // Get new frequencies
                    double[] newMagnitudes = magnitudes;  // Get new magnitudes

                    Platform.runLater(() -> {
                        series.clear();
                        for (int i = 0; i < newFrequencies.length; i++) {
                            series.add(newFrequencies[i], newMagnitudes[i]);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void createChart(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            // 创建图表
            JFreeChart chart = ChartFactory.createXYLineChart(
                    "FFT Results",
                    "Frequency (Hz)",
                    "Magnitude",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);

            XYPlot plot = chart.getXYPlot();  // Get the plot object from the chart
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();  // Get the y-axis from the plot
            yAxis.setAutoRangeIncludesZero(false);
            yAxis.setTickUnit(new NumberTickUnit(0.5));  // Set the interval to 10 or whatever value you want
            yAxis.setRange(0.0, 30.0);

            ChartPanel chartPanel = new ChartPanel(chart);
            swingNode.setContent(chartPanel);
        });
    }


    @Override
    public void run() {
        boolean connectionTest = false;
        StringBuilder errorMessage = new StringBuilder();

        while (running) {
            try {
                // 命令行参数以数组形式传入
//                String[] cmd = {
//                        ffmpegLocation,
//                        "-i", // 输入流为标准输入
//                        rtmpUrl,
////                        "rtmp://192.168.100.121:1935/live/str1",
////                        "C:\\资料\\青海回听监测\\audio\\23-101000-101300.ts",
//                        "-vn", // 覆盖输出文件
//                        "-acodec", "pcm_s16le", // 音频解码器
//                        "-ac", "2", // 声道数量
//                        "-ar", "48000", // 采样率
//                        "-f", "wav",
//                        "-"
//                };

                String[] cmd = {
                        ffmpegLocation,
                        "-i", // 输入流为标准输入
                        rtmpUrl,
                        "-vn", // 覆盖输出文件
                        "-acodec", "pcm_s16le", // 音频解码器
                        "-ac", "2", // 声道数量
                        "-ar", "48000", // 采样率
                        "-f", "wav",
                        "-"
                };
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                InputStream in = process.getInputStream();

                //处理音频数据
                processAudio(in);

                in.close();
                process.waitFor();
                // 等待一定时间再继续解码
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("进程异常中止，准备重启......");
                running = false;
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);

            }
        }
    }

    public static class LowPassFilter {
        private final double alpha;
        private double state;

        public LowPassFilter(double cutoffFrequency, double dt) {
            alpha = dt / (dt + 1.0 / (2.0 * Math.PI * cutoffFrequency));
            state = 0;
        }

        public short apply(short value) {
            state = alpha * value + (1.0 - alpha) * state;
            return (short) state;
        }
    }

    static void processPCMData(byte[] pcmData, double cutoffFrequency, double sampleRate) {
        // Create separate filters for each channel
        LowPassFilter filter1 = new LowPassFilter(cutoffFrequency, 1/sampleRate);
        LowPassFilter filter2 = new LowPassFilter(cutoffFrequency, 1/sampleRate);

        ByteBuffer bb = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < pcmData.length/4; ++i) {
            short sample1 = bb.getShort();
            short sample1Filtered = filter1.apply(sample1);
            bb.position(bb.position() - 2);
            bb.putShort(sample1Filtered);

            short sample2 = bb.getShort();
            short sample2Filtered = filter2.apply(sample2);
            bb.position(bb.position() - 2);
            bb.putShort(sample2Filtered);
        }
    }

    private void processAudio(InputStream in) throws IOException {


        // 创建一个缓冲区，用于存储一帧的数据
        byte[] buffer = new byte[BYTES_PER_FRAME];
        while (in.read(buffer) != -1 && running) {
//            System.out.println(Arrays.toString(buffer));
            sendAudioToEngine(buffer);
        }
    }

    FfmpegDecoderForQinHai updateRunnable = null;
    Thread updateThread = null;
    Process process = null;

    private void freshStatistic(
            TextField totalCount,
            TextField noInfoCountText,
            TextField littleInfoText,
            TextField infoText,
            TextField finalScoreText,
            TextField continuousScoreText){

        totalCount.setText("0");
        noInfoCountText.setText("0");
        littleInfoText.setText("0");
        infoText.setText("0");
        finalScoreText.setText("0");
        continuousScoreText.setText("0");


        //遍历缓存数据并统计次数
        AtomicInteger noInfoCount = new AtomicInteger();
        AtomicInteger littleInfoCount = new AtomicInteger();
        AtomicInteger infoCount = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        //连续统计
        AtomicInteger continuousInfoCount = new AtomicInteger();
        AtomicInteger continuousLittleInfoOrNoInfoCount = new AtomicInteger();
        AtomicReference<Double> continuousScore = new AtomicReference<>((double) 5);

        BigDecimal finalScore = new BigDecimal(0);

        for (Iterator<Map.Entry<Long, PcmAnalyzeResultEnum>> it = timeBasedCache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, PcmAnalyzeResultEnum> entry = it.next();
            PcmAnalyzeResultEnum resultEnum = entry.getValue();
            total.getAndIncrement();

            int intType = resultEnum.getValue();
            if (intType == MUTE_OR_LIGHT_WHITE_NOISE.getValue() || intType == STRONG_WHITE_NOISE.getValue()) {
                noInfoCount.getAndIncrement();

                //连续性处理
                continuousLittleInfoOrNoInfoCount.getAndIncrement();

                if (continuousLittleInfoOrNoInfoCount.get() > 7){
                    continuousScore.set(1.5);
                }

                if (continuousLittleInfoOrNoInfoCount.get() > 15){
                    continuousScore.set(0.0);
                }


                continuousInfoCount.set(0);

            } else if (intType == LITTLE_HUMAN_VOICE.getValue()) {
                littleInfoCount.getAndIncrement();

                //连续性处理
                continuousLittleInfoOrNoInfoCount.getAndIncrement();

                if (continuousLittleInfoOrNoInfoCount.get() > 10){
                    continuousScore.set(1.5);
                }

                if (continuousLittleInfoOrNoInfoCount.get() > 15){
                    continuousScore.set(0.0);
                }

                continuousInfoCount.set(0);


            } else if (intType == SPEAK_VOICE.getValue() || intType == MUSIC.getValue()) {
                infoCount.getAndIncrement();

                //连续性处理
                continuousInfoCount.getAndIncrement();
                continuousLittleInfoOrNoInfoCount.set(0);

            } else {
                System.out.println("redis中存储的pcmAnalyzeResult存在错误");
            }
        }


        double score = convertToScoreDetailForPcm(infoCount.intValue(), littleInfoCount.intValue(), noInfoCount.intValue(), total.intValue());
        if (continuousScore.get() != 5) {
            score = continuousScore.get() + score / 5;
            continuousScoreText.setText(String.valueOf(continuousScore));
        }

        double noInfoOrLittleInfoPercent = (noInfoCount.intValue() + littleInfoCount.intValue()) / total.doubleValue();
        if (noInfoOrLittleInfoPercent <= 0.2){
            score = score / 5 + 4.0;
        }

        finalScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        totalCount.setText(String.valueOf(total.intValue()));
        noInfoCountText.setText(String.valueOf(noInfoCount.intValue()));
        littleInfoText.setText(String.valueOf(littleInfoCount.intValue()));
        infoText.setText(String.valueOf(infoCount.intValue()));
        finalScoreText.setText(String.valueOf(finalScore));
    }

    private double convertToScoreDetailForPcm(int fiveTypeCount, int halfTypeCount, int zeroTypeCount, int total) {
        return (fiveTypeCount * 5 + halfTypeCount * 2.5 + zeroTypeCount * 0) * 1.0 / total;
    }

    private void drawLineGraph(GraphicsContext gc, Canvas canvas) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.DARKORANGE);
        gc.setLineWidth(2);
        gc.beginPath();

        if (data == null || data.length == 0) {
            return;
        }

        for (int i = 0; i < data.length; i++) {
            double x = i * (canvas.getWidth() - 100) / (data.length - 1) + 50;
            double y = ((-data[i] + 1) / 2) * (canvas.getHeight() - 100) + 50;
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }

        gc.stroke();
    }

    public static void sendAudioToEngine(byte[] pcmData){
        double[] audioData = applyHammingWindow(pcmData);


        double[] tempData = Arrays.copyOfRange(audioData, 0, audioData.length);
//        tempData = smooth(tempData, 10);

//        if (hasDCBias(audioData)) {
//            audioData = removeDCBias(audioData);
//        }

        AudioFFT audioFFT = new AudioFFT(audioData);
        audioFFT.fft();
        double[] fftData = audioFFT.getFFTData();
//        double[] amplitude = audioFFT.getFFTAmplitude4SampleSize();


        fftSize = fftData.length / 2;
        magnitudes = new double[fftSize];
        frequencies = new double[fftSize];

// DC component
        magnitudes[0] = Math.abs(fftData[0]);
        double frequency = 0;  // 频率为0的幅度
        frequencies[0] = frequency;
//        System.out.println("Frequency: " + frequency + " Hz, Magnitude: " + magnitudes[0]);

// Nyquist component
        if (fftData.length % 2 == 0) {
            magnitudes[fftSize - 1] = Math.abs(fftData[1]);
            frequency = sampleRate / 2;  // Nyquist频率的幅度
            frequencies[fftSize - 1] = frequency;
//            System.out.println("Frequency: " + frequency + " Hz, Magnitude: " + magnitudes[fftSize - 1]);
        }

// Other frequencies
        for (int i = 1; i < fftSize - 1; i++) {
            magnitudes[i] = Math.hypot(fftData[2 * i], fftData[2 * i + 1]);
            frequency = i * sampleRate / (2.0 * fftSize);  // 显示当前频率（Hz）
            frequencies[i] = frequency;
//            System.out.println("Frequency: " + frequency + " Hz, Magnitude: " + magnitudes[i]);
        }


//        tempData = normalizeFFTData(tempData);
//        amplitude = audioFFT.normalizeAmplitude(amplitude);

//        amplitude = meanFilter(amplitude, 3);

//        amplitude = compress(amplitude, 10);

//        amplitude = smooth(amplitude, 30);

//        double[] tempData = Arrays.copyOfRange(amplitude, 10, 70);
//        double[] tempData = Arrays.copyOfRange(amplitude, 0, amplitude.length);

//        if (containsNaN(tempData)) {
//            return;
//        }
//
//        oldData = data;
//
//        data = tempData;
    }

    public static void sendAudioDataToAudioEngine(byte[] pcmData) {
//        processPCMData(pcmData, 4000, 1 / 48000.0);
//
//        byte[] pcmNewData = new byte[pcmData.length];
//        pcmNewData = Arrays.copyOfRange(pcmData, 0, pcmData.length);
//        if (pcmList.size() < 25){
//            pcmList.add(pcmNewData);
//            return;
//        }else {
//            pcmList.add(pcmNewData);
//            pcmList.remove(0);
//        }
//
//        for (int j = 0; j < pcmList.size() - 1; j++) {
//            byte[] pcm1 = pcmList.get(j);
//            byte[] pcm2 = pcmList.get(j+1);
//
//            // 假设pcm数据是16位的，每个样本2字节
//            int numSamples = pcm1.length / 2;
//
//            double diff = 0;
//            for (int i = 0; i < numSamples; i++) {
//                // 转换成16位的整数
//                short sample1 = (short) ((pcm1[2*i] & 0xFF) | (pcm1[2*i+1] << 8));
//                short sample2 = (short) ((pcm2[2*i] & 0xFF) | (pcm2[2*i+1] << 8));
//
//                // 计数差异值
//                diff += Math.pow(sample1 - sample2, 2);
//            }
//
//            // 计算欧氏距离
//            diff = Math.sqrt(diff / numSamples);
//
//            // 放入结果列表
//            diffList.add(diff);
//        }
//
//
//
//        System.out.println(diffList);
//
//        diffList.clear();

        double[] audioData = applyHammingWindow(pcmData);

        double[] tempData = Arrays.copyOfRange(audioData, 0, audioData.length);
//        double bigDropCount = countLargeDropWindows(tempData, 5, 0.1);
//        if (bigDropCount > 500){
//            return;
//        }

        if (containsNaN(tempData)) {
            return;
        }

       tempData = meanFilter(tempData, 15);
//
        oldData = data;
        data = tempData;
        double rmse = calculateRMSE(oldData, data);
//
//
//        int largeDrop = countLargeDropWindows(tempData, 15, 0.1);
        if (largeDropCountsIndex < largeDropCounts.length) {
//            largeDropCounts[largeDropCountsIndex] = largeDrop;
            RMSEs[largeDropCountsIndex] = rmse;

            largeDropCountsIndex++;
            return;
        }

        largeDropCountsIndex = 0;

//        System.out.println("largeDropCounts = " + Arrays.toString(largeDropCounts));
        System.out.println("RMSEs = " + Arrays.toString(RMSEs));


//        countSoundTypes(largeDropCounts);
    }


    public static double calculateRMSE(double[] arr1, double[] arr2) {
        // 两个数组长度必须，否则无法比较
        if (arr1.length != arr2.length) {
            return -1;
        }
        double sum = 0;
        double mean1 = 0;
        double mean2 = 0;
        for (int i = 0; i < arr1.length; i++) {
            mean1 += arr1[i];
            mean2 += arr2[i];
        }
        mean1 /= arr1.length;
        mean2 /= arr2.length;
        double diff = mean2 - mean1; // 计算两个平均值的差值
        for (int i = 0; i < arr2.length; i++) {
            arr2[i] -= diff; // 对波形2的每个元素进行平移
            double error = arr1[i] - arr2[i];
            sum += error * error;
        }
        double mean = sum / arr1.length;
        return Math.sqrt(mean);
    }


    public static int countLargeDropWindows(double[] data, int windowSize, double minDrop) {
        int count = 0;

        for (int i = 0; i <= data.length - windowSize; i++) {
            double min = data[i];
            double max = data[i];

            for (int j = i; j < i + windowSize; j++) {
                if (data[j] < min) {
                    min = data[j];
                }
                if (data[j] > max) {
                    max = data[j];
                }
            }

            if (max - min > minDrop) {
                count++;
            }
        }

        return count;
    }

    public static double[] applyHammingWindow(byte[] pcmData) {
        // 将PCM数据转换为double数组
        double[] pcmDataDouble = new double[pcmData.length / 4];
        for (int i = 0; i < pcmDataDouble.length; i++) {
            short left = (short) ((pcmData[4 * i] & 0xFF) | (pcmData[4 * i + 1] << 8));
            short right = (short) ((pcmData[4 * i + 2] & 0xFF) | (pcmData[4 * i + 3] << 8));
            pcmDataDouble[i] = (left + right) / 2.0 / 32768.0;  // 将平均值转换为double类型，范围为-1.0到1.0
        }

        // 应用汉宁窗
        double[] hammingWindow = new double[pcmDataDouble.length];
        for (int i = 0; i < pcmDataDouble.length; i++) {
            hammingWindow[i] = pcmDataDouble[i] * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (pcmDataDouble.length - 1)));
        }

        return hammingWindow;
//        return pcmDataDouble;
    }

    public static double[] meanFilter(double[] arr, int n) {
        double[] result = new double[arr.length];
        int half = n / 2;
        double sum = 0.0;
        for (int i = 0; i < arr.length; i++) {
            if (i >= half) {
                sum -= arr[i - half];
            }
            if (i + half < arr.length) {
                sum += arr[i + half];
            }
            if (i + half >= arr.length) {
                result[i] = sum / (arr.length - i + half);
            } else if (i >= half) {
                result[i] = sum / n;
            }
        }
        return result;
    }


    public static boolean containsNaN(double[] arr) {
        for (double d : arr) {
            if (Double.isNaN(d)) {
                return true;
            }
        }
        return false;
    }


//        1.静音或轻微白噪声：largeDropCounts中所有数据都为0

//        2.少量人声：largeDropCounts中几乎所有数据都为0，少量数据为100以内的数据

//        3.人声：largeDropCounts中大量数据处于0-300之间，少量处于300-500，含较多连续的0，不断浮动，连续的0表明停顿静音

//        × 不同的音乐表现不同4.歌声：largeDropCounts中几乎没有0出现，浮动较大，一段时间内所有数据处于0-300之间浮动无规律

//        5.强烈白噪声：largeDropCounts全部处于100以上

    public static void countSoundTypes(int[] largeDropCounts){
        int serialZeroCount = 0;
        int zeroCount = 0;
        int zeroToOneHundredCount = 0;
        int zeroToThreeHundredCount = 0;
        int threeHundredToFiveHundredCount = 0;
        int overOneHundredAndEightyCount = 0;
        int overOneHundredCount = 0;
        int serialZeroCountForLoop = 0;
        int zeroToFiftyCount = 0;

        for (int largeDropCount : largeDropCounts) {
            if (largeDropCount == 0){
                zeroCount ++;
                zeroToFiftyCount ++;
                zeroToOneHundredCount ++;
                zeroToThreeHundredCount ++;
                serialZeroCountForLoop ++;
            }else if (largeDropCount > 0 && largeDropCount <= 100){

                if (largeDropCount <= 50){
                    zeroToFiftyCount ++;
                }

                zeroToOneHundredCount ++;
                zeroToThreeHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else if (largeDropCount > 100 && largeDropCount <= 300){
                zeroToThreeHundredCount ++;
                overOneHundredCount ++;
                if (largeDropCount > 180){
                    overOneHundredAndEightyCount ++;
                }
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else if (largeDropCount > 300 && largeDropCount <= 500){
                threeHundredToFiveHundredCount ++;
                overOneHundredAndEightyCount ++;
                overOneHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else {
                overOneHundredAndEightyCount++;
                overOneHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }
        }


        double zeroPercent = 0;
        double zeroToOneHundredPercent = 0;
        double zeroToThreeHundredPercent = 0;
        double threeHundredToFiveHundredPercent = 0;
        double overOneHundredAndEightyPercent = 0;
        double overOneHundredPercent = 0;
        double zeroToFiftyPercent = 0;

        int length = largeDropCounts.length;
        zeroPercent = zeroCount  * 1.0/ length;
        zeroToFiftyPercent = zeroToFiftyCount  * 1.0/ length;
        zeroToOneHundredPercent = zeroToOneHundredCount  * 1.0/ length;
        zeroToThreeHundredPercent = zeroToThreeHundredCount  * 1.0/ length;
        threeHundredToFiveHundredPercent = threeHundredToFiveHundredCount  * 1.0/ length;
        overOneHundredAndEightyPercent = overOneHundredAndEightyCount  * 1.0/ length;
        overOneHundredPercent = overOneHundredCount * 1.0/ length;

        long time = System.currentTimeMillis();
        if (zeroPercent >= 0.95 || zeroToFiftyPercent >= 0.99){
            System.out.println("静音或轻微白噪声");
            result = LocalDateTime.now() + "-----静音或轻微白噪声";
            timeBasedCache.put(time, MUTE_OR_LIGHT_WHITE_NOISE);
            return;
        }
        if (zeroPercent >= 0.3 && zeroToOneHundredPercent > 0 && overOneHundredPercent < 0.125){
            System.out.println("少量人声");
            result = LocalDateTime.now() + "-----少量人声";
            timeBasedCache.put(time, LITTLE_HUMAN_VOICE);
            return;
        }
        if (zeroToThreeHundredPercent > 0.6){
            System.out.println("人声");
            result = LocalDateTime.now() + "-----人声";
            timeBasedCache.put(time, SPEAK_VOICE);
            return;
        }
        if (overOneHundredAndEightyPercent >= 0.95){
            System.out.println("强烈白噪声");
            result = LocalDateTime.now() + "-----强烈白噪声";
            timeBasedCache.put(time, STRONG_WHITE_NOISE);
            return;
        }
        System.out.println("音乐");
        result = LocalDateTime.now() + "-----音乐";
        timeBasedCache.put(time, STRONG_WHITE_NOISE);

    }
}
