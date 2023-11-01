package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.PcmAnalyzeResultEnum.*;

public class FfmpegDecoder2Realtime extends Application implements Runnable {
    private final int sleepTime = 1000; // 停顿时间
    private static final int NUM_FRAMES = 25;
    private static final int NUM_SAMPLES = 1024;
    private static final int NUM_CHANNELS = 2;
    private static final int SAMPLE_SIZE = 2;
    private static final int BYTES_PER_FRAME = NUM_CHANNELS * SAMPLE_SIZE * NUM_SAMPLES;
//    private static final int BYTES_PER_FRAME = 941;

    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final int[] largeDropCounts = new int[NUM_FRAMES];
    private static int largeDropCountsIndex = 0;
    private static String result = null;
    private static String lastResult = null;
    private static String rtmpUrl = "rtmp://192.168.100.170:1935/hlsram/live16";

    private static String ffmpegLocation = "C:\\Develop\\Ffmpeg\\bin\\ffmpeg.exe";
    private static String potPlayerPath = "C:\\工具\\PotPlayer\\PotPlayer64\\PotPlayerMini64.exe";

    private static TimeBasedCache<Long, PcmAnalyzeResultEnum> timeBasedCache = new TimeBasedCache<>(5, TimeUnit.MINUTES);


    private volatile boolean running = true;



    public static void setRtmpUrl(String rtmpUrl) {
        FfmpegDecoder2Realtime.rtmpUrl = rtmpUrl;
    }

    public static void setFfmpegLocation(String ffmpegLocation) {
        FfmpegDecoder2Realtime.ffmpegLocation = ffmpegLocation;
    }

    public static void setPotPlayerPath(String potPlayerPath) {
        FfmpegDecoder2Realtime.potPlayerPath = potPlayerPath;
    }

    public void stopRunning() {
        this.running = false;
    }
    public static void main(String[] args) {
        launch(args);

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

    private void processAudio(InputStream in) throws IOException {


        // 创建一个缓冲区，用于存储一帧的数据
        byte[] buffer = new byte[BYTES_PER_FRAME];
        while (in.read(buffer) != -1 && running) {
//            System.out.println(Arrays.toString(buffer));
            sendAudioDataToAudioEngine(buffer);
        }
    }

    FfmpegDecoder2Realtime updateRunnable = null;
    Thread updateThread = null;
    Process process = null;
    @Override
    public void start(Stage primaryStage) {
        data = new double[1024];  // Initialise your data with 1024 value

        final Canvas canvas = new Canvas(1024, 600);   // Expanded canvas width for 1024 data points
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        Pane root = new Pane();
        root.getChildren().add(canvas);


        TextArea consoleOut = new TextArea();  // Create a new textarea to display the output
        consoleOut.setEditable(false);
        consoleOut.setLayoutX(1100);
        consoleOut.setLayoutY(50);
        consoleOut.setPrefSize(300, 500);
        consoleOut.setPrefRowCount(30);
        root.getChildren().add(consoleOut); // Add the textarea to the root pane

//        consoleOut.textProperty().addListener(new ChangeListener<String>() {
//            @Override
//            public void changed(ObservableValue<? extends String> observableValue, String oldText, String newText) {
//                // Limit line count to 10
//
//            }
//        });

        Font font = new Font(8);
        Label totaolCountLabel = new Label("总量:");
        totaolCountLabel.setLayoutX(1060); // Adjust these according to your layout
        totaolCountLabel.setLayoutY(5);
        totaolCountLabel.setFont(font);
        root.getChildren().add(totaolCountLabel);

        TextField totalCount = new TextField();
        totalCount.setLayoutX(1080); // Set positions according to your layout
        totalCount.setLayoutY(2);
        totalCount.setPrefSize(30, 8);
        totalCount.setFont(font);
        totalCount.setText("0");
        root.getChildren().add(totalCount);


        Label noInfoCountLabel = new Label("无信息:");
        noInfoCountLabel.setLayoutX(1120); // Adjust these according to your layout
        noInfoCountLabel.setLayoutY(5);
        noInfoCountLabel.setFont(font);
        root.getChildren().add(noInfoCountLabel);

        TextField noInfoCountText = new TextField();
        noInfoCountText.setLayoutX(1150); // Set positions according to your layout
        noInfoCountText.setLayoutY(2);
        noInfoCountText.setPrefSize(30, 8);
        noInfoCountText.setFont(font);
        noInfoCountText.setText("0");
        root.getChildren().add(noInfoCountText);



        Label littleInfoCountLabel = new Label("少量信息:");
        littleInfoCountLabel.setLayoutX(1190); // Adjust these according to your layout
        littleInfoCountLabel.setLayoutY(5);
        littleInfoCountLabel.setFont(font);
        root.getChildren().add(littleInfoCountLabel);

        TextField littleInfoText = new TextField();
        littleInfoText.setLayoutX(1230); // Set positions according to your layout
        littleInfoText.setLayoutY(2);
        littleInfoText.setPrefSize(30, 8);
        littleInfoText.setFont(font);
        littleInfoText.setText("0");
        root.getChildren().add(littleInfoText);


        Label infoCountLabel = new Label("有信息:");
        infoCountLabel.setLayoutX(1270); // Adjust these according to your layout
        infoCountLabel.setLayoutY(5);
        infoCountLabel.setFont(font);
        root.getChildren().add(infoCountLabel);

        TextField infoText = new TextField();
        infoText.setLayoutX(1300); // Set positions according to your layout
        infoText.setLayoutY(2);
        infoText.setPrefSize(30, 8);
        infoText.setFont(font);
        infoText.setText("0");
        root.getChildren().add(infoText);


        Label scoreLabel = new Label("得分:");
        scoreLabel.setLayoutX(1060); // Adjust these according to your layout
        scoreLabel.setLayoutY(23);
        scoreLabel.setFont(font);
        root.getChildren().add(scoreLabel);

        TextField score = new TextField();
        score.setLayoutX(1080); // Set positions according to your layout
        score.setLayoutY(20);
        score.setPrefSize(30, 8);
        score.setFont(font);
        score.setText("0");
        root.getChildren().add(score);


//
//        Label continuousInfoLabel = new Label("有信息连续数量:");
//        continuousInfoLabel.setLayoutX(1220); // Adjust these according to your layout
//        continuousInfoLabel.setLayoutY(23);
//        continuousInfoLabel.setFont(font);
//        root.getChildren().add(continuousInfoLabel);
//
//        TextField continuousInfoText = new TextField();
//        continuousInfoText.setLayoutX(1270); // Set positions according to your layout
//        continuousInfoText.setLayoutY(20);
//        continuousInfoText.setPrefSize(30, 8);
//        continuousInfoText.setFont(font);
//        continuousInfoText.setText("0");
//        root.getChildren().add(continuousInfoText);
//
//
//
//        Label continuousNoInfoOrLittleInfoLabel = new Label("无或少量信息连续数量:");
//        continuousNoInfoOrLittleInfoLabel.setLayoutX(1120); // Adjust these according to your layout
//        continuousNoInfoOrLittleInfoLabel.setLayoutY(23);
//        continuousNoInfoOrLittleInfoLabel.setFont(font);
//        root.getChildren().add(continuousNoInfoOrLittleInfoLabel);
//
//        TextField continuousNoInfoOrLittleInfoText = new TextField();
//        continuousNoInfoOrLittleInfoText.setLayoutX(1170); // Set positions according to your layout
//        continuousNoInfoOrLittleInfoText.setLayoutY(20);
//        continuousNoInfoOrLittleInfoText.setPrefSize(30, 8);
//        continuousNoInfoOrLittleInfoText.setFont(font);
//        continuousNoInfoOrLittleInfoText.setText("0");
//        root.getChildren().add(continuousNoInfoOrLittleInfoText);



        Label continuousScoreLabel = new Label("连续数量得分:");
        continuousScoreLabel.setLayoutX(1120); // Adjust these according to your layout
        continuousScoreLabel.setLayoutY(23);
        continuousScoreLabel.setFont(font);
        root.getChildren().add(continuousScoreLabel);

        TextField continuousScoreText = new TextField();
        continuousScoreText.setLayoutX(1180); // Set positions according to your layout
        continuousScoreText.setLayoutY(20);
        continuousScoreText.setPrefSize(30, 8);
        continuousScoreText.setFont(font);
        continuousScoreText.setText("0");
        root.getChildren().add(continuousScoreText);
//
//
//
//        Label totaolCountLabel = new Label("总量:");
//        totaolCountLabel.setLayoutX(1200); // Adjust these according to your layout
//        totaolCountLabel.setLayoutY(5);
//        totaolCountLabel.setFont(font);
//        root.getChildren().add(totaolCountLabel);
//
//        TextField totalCount = new TextField();
//        totalCount.setLayoutX(1220); // Set positions according to your layout
//        totalCount.setLayoutY(2);
//        totalCount.setPrefSize(30, 8);
//        totalCount.setFont(font);
//        totalCount.setText("0");
//        root.getChildren().add(totalCount);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Draw in the JavaFX Application Thread
                drawLineGraph(gc, canvas);


                if (result != null && !result.equals(lastResult)) {
                    consoleOut.appendText(result + "\n");

                    freshStatistic(totalCount, noInfoCountText, littleInfoText, infoText, score, continuousScoreText);
                }
                String[] lines = consoleOut.getText().split("\n");  // or "\r\n" if you're using that
                if (lines.length > 30) {
                    // Remove first line when line count exceeds 10
                    String newTextLimited = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
                    newTextLimited = newTextLimited + "\n";
                    consoleOut.setText(newTextLimited);
                }
                lastResult = result;

            }
        }.start();

        // Start a new Thread that will update 'data'
        updateRunnable = this;
        updateThread = new Thread(this);
        updateThread.start();



        Label nameLabel = new Label("RTMP地址");
        nameLabel.setLayoutX(600); // Adjust these according to your layout
        nameLabel.setLayoutY(25);
        root.getChildren().add(nameLabel);

        TextField inputTextField = new TextField();
        inputTextField.setLayoutX(660); // Set positions according to your layout
        inputTextField.setLayoutY(20);
        inputTextField.setPrefSize(200, 30);
        inputTextField.setText(rtmpUrl);
        root.getChildren().add(inputTextField);

        Label ffmpegLabel = new Label("ffmpeg地址");
        ffmpegLabel.setLayoutX(10); // Adjust these according to your layout
        ffmpegLabel.setLayoutY(25);
        root.getChildren().add(ffmpegLabel);

        TextField ffmpeg = new TextField();
        ffmpeg.setLayoutX(80); // Set positions according to your layout
        ffmpeg.setLayoutY(20);
        ffmpeg.setPrefSize(200, 30);
        ffmpeg.setText(ffmpegLocation);
        root.getChildren().add(ffmpeg);

        Label potPlayerLabel = new Label("potPlayer地址");
        potPlayerLabel.setLayoutX(290); // Adjust these according to your layout
        potPlayerLabel.setLayoutY(25);
        root.getChildren().add(potPlayerLabel);

        TextField potPlayer = new TextField();
        potPlayer.setLayoutX(380); // Set positions according to your layout
        potPlayer.setLayoutY(20);
        potPlayer.setPrefSize(200, 30);
        potPlayer.setText(potPlayerPath);
        root.getChildren().add(potPlayer);

        // Create a button to trigger thread start
        Button button = new Button("重新分析");
        button.setLayoutX(880);
        button.setLayoutY(20);
        button.setOnAction(e -> {
            timeBasedCache.clear();
            if (updateThread != null && updateThread.isAlive()) {
                updateRunnable.stopRunning();  // Stop the current thread

            }
            updateRunnable = new FfmpegDecoder2Realtime();  // Create new runnable
            setRtmpUrl(inputTextField.getText());
            setFfmpegLocation(ffmpeg.getText());
            updateThread = new Thread(updateRunnable);  // Store thread reference to control it later

            updateThread.start();  // Start thread

            if (process != null) {  // 如果前一个进程还在运行，就结束它
                process.destroy();
            }
            setPotPlayerPath(potPlayer.getText());
            if (potPlayerPath != null && !potPlayerPath.isEmpty()){
                String[] command = { potPlayerPath, rtmpUrl };
                ProcessBuilder pb = new ProcessBuilder(command);

                // 合并标准错误流和标准输出流
                pb.redirectErrorStream(true);

                try {
                    if (process != null) {
                        process.destroy();
                    }
                    process = pb.start();

                    // 在单独的线程中处理子进程的输出流
                    new Thread(() -> {
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        try {
                            while ((line = inputReader.readLine()) != null) {
                                System.out.println(line);
                            }
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }).start();

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        });
        root.getChildren().add(button);

        Button showResults = new Button("5分钟内结果集");
        showResults.setLayoutX(960); // Adjust these according to your layout
        showResults.setLayoutY(20); // Adjust these according to your layout

// Generates a string representation of the map
        showResults.setOnAction(e -> {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Map.Entry<Long, PcmAnalyzeResultEnum>> it = timeBasedCache.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, PcmAnalyzeResultEnum> entry = it.next();
                Long time = entry.getKey();
                PcmAnalyzeResultEnum resultEnum = entry.getValue();

                Instant instant = Instant.ofEpochMilli(time);
                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, TimeZone.getTimeZone("Asia/Shanghai").toZoneId());
                sb.append(localDateTime).append("----").append(resultEnum.getName()).append("\n");
            }

            // Create a TextArea for your dialog text
            TextArea textArea = new TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(500); // Set the max width that you want

            // Create new window (Stage)
            Stage newWindow = new Stage();
            newWindow.setTitle("5分钟内结果集");

            // Create new scene with textArea as root
            Scene scene = new Scene(textArea, 500, 400);

            newWindow.setScene(scene);
            newWindow.show();
        });

        root.getChildren().add(showResults);



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

        System.out.println("largeDropCounts = " + Arrays.toString(largeDropCounts));



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
        int overOneHundredAndEightyCount = 0;
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

        int length = largeDropCounts.length;
        zeroPercent = zeroCount  * 1.0/ length;
        zeroToOneHundredPercent = zeroToOneHundredCount  * 1.0/ length;
        zeroToThreeHundredPercent = zeroToThreeHundredCount  * 1.0/ length;
        threeHundredToFiveHundredPercent = threeHundredToFiveHundredCount  * 1.0/ length;
        overOneHundredAndEightyPercent = overOneHundredAndEightyCount  * 1.0/ length;
        overOneHundredPercent = overOneHundredCount * 1.0/ length;

        long time = System.currentTimeMillis();
        if (zeroPercent >= 0.95){
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
        if (zeroToThreeHundredPercent > 0.6 && serialZeroCount > 0){
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
        timeBasedCache.put(time, MUSIC);

    }
}
