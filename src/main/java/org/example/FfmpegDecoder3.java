package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FfmpegDecoder3 extends Application implements Runnable {
    private boolean running;

    private int maxRestartAttempts = 5;
    private int restartDelay = 3000;
    private int restartAttempts = 0;
    private final int sleepTime = 1000; // 停顿时间

    private static final int NUM_FRAMES = 25;
    private static final int NUM_SAMPLES = 1024;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_SIZE = 4;
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
//        launch(args);
        FfmpegDecoder3 decoder = new FfmpegDecoder3();
        decoder.setRunning(true);
        Thread thread = new Thread(decoder);
        thread.start();


        // 重启线程
        int attempt = 0;
        while (true) {
            if (thread.isInterrupted()){
//                if (attempt >= decoder.maxRestartAttempts) {
//                    System.out.println("达到最大重启次数，已停止尝试重启！");
//                    break;
//                } else {
//                    attempt++;
//                }

                attempt ++;

                System.out.println("正在尝试第" + attempt + "次重启......");
                decoder = new FfmpegDecoder3();
                decoder.setRunning(true);
                thread = new Thread(decoder);
                thread.start();

            }
            try {
                Thread.sleep(500); // 休眠500毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }

    @Override
    public void run() {

        // 控制外部调用setRunning()方法
        while (!running) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }

        while (running) {
            try {
                // 命令行参数以数组形式传入
                String[] cmd = {
                        "C:\\Develop\\Ffmpeg\\bin\\ffmpeg.exe",
                        "-i", // 输入流为标准输入
                        "rtmp://192.168.100.53:1935/hlsram/live5",
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

            sendAudioDataToAudioEngine(buffer, null, 257);

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

    public void start(Stage stage) {
        // 创建x轴和y轴
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("X");
        yAxis.setLabel("Y");

        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(1d);
        yAxis.setLowerBound(-1d);
        yAxis.setTickUnit(0.05);


        // 设置x轴范围为1到10
//        xAxis.setAutoRanging(false);
//        xAxis.setUpperBound(2000);

        // 创建折线图
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("频域能量图");
        lineChart.createSymbolsProperty().set(false);

        // 创建数据系列
        series.setName("频域能量值");

        // 向数据系列中添加数据
        for (int i = 0; i < data.length; i++) {
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, data[i]);
            series.getData().add(dataPoint);
        }


        // 将数据系列添加到折线图上
        lineChart.getData().add(series);

        // 创建场景
        Scene scene = new Scene(lineChart, 1600, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // 设置窗口标题并显示窗口
        stage.setTitle("音频流频域能量图");
        stage.setScene(scene);
        stage.show();

        FfmpegDecoder3 decoder = new FfmpegDecoder3();
        decoder.setRunning(true);
        decoder.setSeries(series);
        Thread thread = new Thread(decoder);
        thread.start();

        // 等待一定时间后终止线程
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        decoder.setRunning(false);
        thread.interrupt();
    }

    public static void sendAudioDataToAudioEngine(byte[] pcmData, XYChart.Series<Number, Number> series, int endIndex) {
        double[] audioData = applyHammingWindow(pcmData);
        if (hasDCBias(audioData)) {
            audioData = removeDCBias(audioData);
        }

        AudioFFT audioFFT = new AudioFFT(audioData);
        audioFFT.fft();
        double[] fftData = audioFFT.getFFTData();
        double[] amplitude = audioFFT.getFFTAmplitude4SampleSize();

        amplitude = audioFFT.normalizeAmplitude(amplitude);

        amplitude = meanFilter(amplitude, 3);

        amplitude = compress(amplitude, 10);

        amplitude = smooth(amplitude, 30);

        double[] tempData = Arrays.copyOfRange(amplitude, 10, 70);

        if (containsNaN(tempData)) {
            return;
        }

        oldData = data;

        data = tempData;

        double rmseValue = calculateRMSE(oldData, data);

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

        if (humanVoiceCount >= 6) {
            System.out.println("该音频为人声，RMSEs值为" + Arrays.toString(RMSEs));
        } else {
            System.out.println("该音频为白噪声，RMSEs值为" + Arrays.toString(RMSEs));
        }


        if (series != null) {
            javafx.application.Platform.runLater(() -> {
                series.getData().clear();
                series.setData(getSeriesData());
            });
        }
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

}
