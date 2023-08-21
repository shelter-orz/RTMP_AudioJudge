package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FfmpegDecoder2TsFile extends Application implements Runnable {
    private boolean running = true;

    private int maxRestartAttempts = 5;
    private int restartDelay = 3000;
    private int restartAttempts = 0;
    private final int sleepTime = 1000; // 停顿时间

    private static final int NUM_FRAMES = 25;
    private static final int NUM_SAMPLES = 1024;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_SIZE = 2;
    private static final int BYTES_PER_FRAME = NUM_CHANNELS * SAMPLE_SIZE * NUM_SAMPLES;

    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static double[] oldData = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final double[] RMSEs = new double[NUM_FRAMES];
    private static int RMSEIndex = 0;

    XYChart.Series<Number, Number> series = new XYChart.Series<>();

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setMaxRestartAttempts(int maxRestartAttempts) {
        this.maxRestartAttempts = maxRestartAttempts;
    }

    public void setRestartDelay(int restartDelay) {
        this.restartDelay = restartDelay;
    }

    public int getRestartAttempts() {
        return restartAttempts;
    }

    public void setSeries(XYChart.Series<Number, Number> series) {
        this.series = series;
    }


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
                        "C:\\资料\\青海回听监测\\audio\\live0-08141727.ts",
//                        "rtmp://192.168.100.121:1935/live/str1",
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

            sendAudioDataToAudioEngine(buffer, series, 257);

        }
    }

    private static byte[] overlapAndSave(byte[][] frames, int frameSize, int overlapSize) {
        // 计算音频总长度
        int totalLength = (frames.length - 1) * (frameSize - overlapSize) + frameSize;
        byte[] result = new byte[totalLength];

        // 重叠部分的长度
        int overlapLength = overlapSize;
        // 目标音频帧已经处理的采样点数
        int targetPos = 0;

        // 处理第一个音频帧
        System.arraycopy(frames[0], 0, result, 0, frameSize);
        targetPos = frameSize - overlapLength;

        for (int i = 1; i < frames.length; i++) {
            // 计算目标帧中不重叠部分和重叠部分的长度
            int nonOverlapLength = frameSize - overlapLength;
            int tailOverlapLength = overlapLength;
            if (i == frames.length - 1) {
                tailOverlapLength = 0;
                nonOverlapLength = frameSize;
            }

            // 将该音频帧后overlapSize个采样点与前一个音频帧的后overlapSize个采样点进行叠加
            for (int j = 0; j < tailOverlapLength; j++) {
                result[targetPos + j] = (byte) (frames[i - 1][j + nonOverlapLength]
                        + frames[i][j]);
            }
            // 将该音频帧的重叠部分作为目标音频帧的重叠部分
            System.arraycopy(frames[i], 0, result, targetPos, tailOverlapLength);
            // 将该音频帧的不重叠部分作为目标音频帧的不重叠部分
            System.arraycopy(frames[i], tailOverlapLength, result, targetPos + tailOverlapLength, nonOverlapLength);
            targetPos += nonOverlapLength;
        }

        return result;
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

        if (data == null || data.length == 0)
            return;

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
    public static void sendAudioDataToAudioEngine(byte[] pcmData, XYChart.Series<Number, Number> series, int endIndex) {
        double[] audioData = applyHammingWindow(pcmData);
        double[] tempData = Arrays.copyOfRange(audioData, 0, audioData.length);
        double bigDropCount = countLargeDropWindows(tempData, 5, 0.1);
        if (bigDropCount > 500){
            return;
        }

//        tempData = smooth(tempData, 10);

        if (hasDCBias(audioData)) {
            audioData = removeDCBias(audioData);
        }

        AudioFFT audioFFT = new AudioFFT(audioData);
        audioFFT.fft();
        double[] fftData = audioFFT.getFFTData();
        double[] amplitude = audioFFT.getFFTAmplitude4SampleSize();

//        tempData = normalizeFFTData(tempData);
        amplitude = audioFFT.normalizeAmplitude(amplitude);

//        amplitude = meanFilter(amplitude, 3);

//        amplitude = compress(amplitude, 10);

//        amplitude = smooth(amplitude, 30);

//        double[] tempData = Arrays.copyOfRange(amplitude, 10, 70);
//        double[] tempData = Arrays.copyOfRange(amplitude, 0, amplitude.length);

        if (containsNaN(tempData)) {
            return;
        }


        oldData = data;

        data = tempData;

//        double rmseValue = calculateRMSE(oldData, data);
        double rmseValue = countLargeDropWindows(tempData, 15, 0.1);
        if (RMSEIndex < RMSEs.length) {
            RMSEs[RMSEIndex] = rmseValue;
            RMSEIndex++;
            return;
        }

        RMSEIndex = 0;

        int humanVoiceCount = 0;
        for (double rms : RMSEs) {
            if (rms >= 0.06) {
                humanVoiceCount++;
            }
        }
        System.out.println("RMSEs = " + Arrays.toString(RMSEs));
//        if (humanVoiceCount >= 6) {
//            System.out.println("该音频为人声，RMSEs值为" + Arrays.toString(RMSEs));
//        } else {
//            System.out.println("该音频为白噪声，RMSEs值为" + Arrays.toString(RMSEs));
//        }
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


    public static boolean hasDCBias(double[] pcmData) {
        double mean = 0.0;
        for (int i = 0; i < pcmData.length; i++) {
            mean += pcmData[i];
        }
        mean /= pcmData.length;
        return mean != 0.0;
    }

    public static double[] removeDCBias(double[] pcmData) {
        double mean = 0.0;
        for (int i = 0; i < pcmData.length; i++) {
            mean += pcmData[i];
        }
        mean /= pcmData.length;
        for (int i = 0; i < pcmData.length; i++) {
            pcmData[i] -= mean;
        }
        return pcmData;
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

    public static double[] smooth(double[] data, int windowSize) {
        double[] smoothed = new double[data.length];
        for (int i = 0; i < data.length - windowSize + 1; i++) {
            double sum = 0;
            for (int j = i; j < i + windowSize; j++) {
                sum += data[j];
                smoothed[i + windowSize / 2] = sum / windowSize;
            }
        }
        return smoothed;
    }

    public static double[] compress(double[] data, double compressionFactor) {
        if (compressionFactor <= 0) {
            throw new IllegalArgumentException("Compression factor must be greater than 0");
        }

        double[] compressedData = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            compressedData[i] = Math.tanh(compressionFactor * data[i]);
        }

        return compressedData;
    }

    public static boolean containsNaN(double[] arr) {
        for (double d : arr) {
            if (Double.isNaN(d)) {
                return true;
            }
        }
        return false;
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


    private static ObservableList<XYChart.Data<Number, Number>> getSeriesData() {
        ObservableList<XYChart.Data<Number, Number>> seriesData = FXCollections.observableArrayList();
        double sum = 0;
        // 向数据系列中添加数据
        for (int i = 0; i < data.length; i++) {
//        for (int i = 0; i < 1000; i++) {
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, data[i] + 0.1);
//            dataPoint.setNode(new Circle(2));
            seriesData.add(dataPoint);
            sum += (data[i] + 0.1);
        }

//        System.out.println("平均值===========" + sum / data.length);
        return seriesData;
    }

    private static ObservableList<XYChart.Data<Number, Number>> getSeriesDataFft() {
        ObservableList<XYChart.Data<Number, Number>> seriesData = FXCollections.observableArrayList();
        double sum = 0;

        // 取data的前半部分并且绘制其绝对值
        int halfLength = data.length / 2;
        for (int i = 0; i < halfLength; i++) {
            double magnitude = Math.abs(data[i]);

            double frequency = (double)i * 48000 / data.length;
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(frequency, magnitude);
            seriesData.add(dataPoint);
            sum += magnitude;
        }

        // 输出平均值
//        System.out.println("Average magnitude: " + sum / halfLength);
        return seriesData;
    }

    private static double[] normalizeFFTData(double[] fftData) {
        double maxMagnitude = Arrays.stream(fftData).max().orElse(1.0);

        double[] normalizedData = new double[fftData.length];
        for (int i = 0; i < fftData.length; i++) {
            normalizedData[i] = fftData[i] / maxMagnitude;
        }

        return normalizedData;
    }

}
