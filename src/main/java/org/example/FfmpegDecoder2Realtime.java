package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FfmpegDecoder2Realtime extends Application implements Runnable {
    private boolean running = true;
    private final int sleepTime = 1000; // 停顿时间
    private static final int NUM_FRAMES = 25;
    private static final int NUM_SAMPLES = 1024;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_SIZE = 2;
    private static final int BYTES_PER_FRAME = NUM_CHANNELS * SAMPLE_SIZE * NUM_SAMPLES;

    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final int[] largeDropCounts = new int[NUM_FRAMES];
    private static int largeDropCountsIndex = 0;


    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void run() {

        while (running) {
            try {
                // 命令行参数以数组形式传入
                String[] cmd = {
                        "C:\\Develop\\Ffmpeg\\bin\\ffmpeg.exe",
                        "-i", // 输入流为标准输入
                        "rtmp://192.168.100.53:1935/hlsram/live0",
//                        "rtmp://192.168.100.121:1935/live/str1",
//                        "C:\\资料\\青海回听监测\\audio\\t1(0.83).ts",
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

    private void processAudio(InputStream in) throws IOException {


        // 创建一个缓冲区，用于存储一帧的数据
        byte[] buffer = new byte[BYTES_PER_FRAME];

        while (in.read(buffer) != -1) {

            sendAudioDataToAudioEngine(buffer);

        }
    }

    @Override
    public void start(Stage primaryStage) {
        data = new double[1024];  // Initialise your data with 1024 value

        final Canvas canvas = new Canvas(1024, 600);   // Expanded canvas width for 1024 data points
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Draw in the JavaFX Application Thread
                drawLineGraph(gc, canvas);
            }
        }.start();

        // Start a new Thread that will update 'data'
        new Thread(this).start();

        Pane root = new Pane();
        root.getChildren().add(canvas);

        // Draw axes
        Line xAxis = new Line(50, 300, 1074, 300); // Shifted x axis to center
        Line yAxis = new Line(50, 50, 50, 550);

        // Create labels for axes
        Text xLabel = new Text(1074, 320, "x-axis");
        Text yLabel = new Text(20, 300, "0"); // Added '0' label
        Text yMinLabel = new Text(20, 560, "-1");
        Text yMaxLabel = new Text(20, 60, "1");

        root.getChildren().addAll(xAxis, yAxis, xLabel, yLabel, yMinLabel, yMaxLabel);

        // Draw x axis grid lines
        for (int i = 70; i <= 1070; i += 20) {
            Line gridLine = new Line(i, 50, i, 550);
            gridLine.setStroke(Color.GRAY);
            root.getChildren().add(gridLine);
        }

        // Draw y axis grid lines
        double yIncrement = (550 - 50) / 40;   // (yMax - yMin) / (1 / 0.05)
        for (int i = 0; i <= 40; i++) {
            Line gridLine = new Line(50, 50 + i * yIncrement, 1074, 50 + i * yIncrement);
            gridLine.setStroke(Color.GRAY);
            root.getChildren().add(gridLine);
        }

        Scene scene = new Scene(root);

        primaryStage.setTitle("Real Time Line Graph");
        primaryStage.setScene(scene);
        primaryStage.show();
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
    public static void sendAudioDataToAudioEngine(byte[] pcmData) {
        double[] audioData = applyHammingWindow(pcmData);

        double[] tempData = Arrays.copyOfRange(audioData, 0, audioData.length);
        double bigDropCount = countLargeDropWindows(tempData, 5, 0.1);
        if (bigDropCount > 500){
            return;
        }

        if (containsNaN(tempData)) {
            return;
        }

        data = tempData;

        int largeDrop = countLargeDropWindows(tempData, 15, 0.1);
        if (largeDropCountsIndex < largeDropCounts.length) {
            largeDropCounts[largeDropCountsIndex] = largeDrop;
            largeDropCountsIndex++;
            return;
        }

        largeDropCountsIndex = 0;

//        System.out.println("largeDropCounts = " + Arrays.toString(largeDropCounts));



        countSoundTypes(largeDropCounts);
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
        int overOneHundredCount = 0;
        int serialZeroCountForLoop = 0;
        for (int largeDropCount : largeDropCounts) {
            if (largeDropCount == 0){
                zeroCount ++;
                zeroToOneHundredCount ++;
                zeroToThreeHundredCount ++;
                serialZeroCountForLoop ++;
            }else if (largeDropCount > 0 && largeDropCount <= 100){
                zeroToOneHundredCount ++;
                zeroToThreeHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else if (largeDropCount > 100 && largeDropCount <= 300){
                zeroToThreeHundredCount ++;
                overOneHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else if (largeDropCount > 300 && largeDropCount <= 500){
                threeHundredToFiveHundredCount ++;
                overOneHundredCount ++;
                if (serialZeroCountForLoop >= 3){
                    serialZeroCountForLoop = 0;
                    serialZeroCount ++ ;
                }
            }else {
                overOneHundredCount++;
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
        double overOneHundredPercent = 0;

        int length = largeDropCounts.length;
        zeroPercent = zeroCount  * 1.0/ length;
        zeroToOneHundredPercent = zeroToOneHundredCount  * 1.0/ length;
        zeroToThreeHundredPercent = zeroToThreeHundredCount  * 1.0/ length;
        threeHundredToFiveHundredPercent = threeHundredToFiveHundredCount  * 1.0/ length;
        overOneHundredPercent = overOneHundredCount  * 1.0/ length;

        if (zeroPercent >= 0.95){
            System.out.println("静音或轻微白噪声");
            return;
        }
        if (zeroPercent >= 0.8 && zeroToOneHundredPercent > 0){
            System.out.println("少量人声");
            return;
        }
        if (zeroToThreeHundredPercent > 0.6 && serialZeroCount > 0){
            System.out.println("人声");
            return;
        }
        if (overOneHundredPercent >= 0.95){
            System.out.println("强烈白噪声");
            return;
        }
        System.out.println("音乐");

    }
}
