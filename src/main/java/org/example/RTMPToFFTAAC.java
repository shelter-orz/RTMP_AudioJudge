//package org.example;
//
//import javafx.application.Application;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.scene.Scene;
//import javafx.scene.chart.LineChart;
//import javafx.scene.chart.NumberAxis;
//import javafx.scene.chart.XYChart;
//import javafx.stage.Stage;
//import org.bytedeco.ffmpeg.avcodec.AVCodec;
//import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
//import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
//import org.bytedeco.ffmpeg.avcodec.AVPacket;
//import org.bytedeco.ffmpeg.avformat.AVFormatContext;
//import org.bytedeco.ffmpeg.avformat.AVStream;
//import org.bytedeco.ffmpeg.avutil.AVDictionary;
//import org.bytedeco.ffmpeg.avutil.AVFrame;
//import org.bytedeco.javacpp.BytePointer;
//import org.bytedeco.javacpp.PointerPointer;
//
//import java.util.*;
//
//import static org.bytedeco.ffmpeg.global.avcodec.*;
//import static org.bytedeco.ffmpeg.global.avformat.*;
//import static org.bytedeco.ffmpeg.global.avutil.*;
//
///**
// * @author: Zhou Yujie
// * @date: 2023/5/18
// **/
//public class RTMPToFFTAAC extends Application{
//
//    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//    private static double[] oldData = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//
//    public static void main(String[] args) throws InterruptedException {
////        launch(args);
//        try {
//            NoCMD(null);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }
//
//    // 定义缓冲区的大小
//
//    private static final int NUM_FRAMES = 16; // 合并的帧数
//    private static final int BYTES_PER_FRAME = 2 * 4; // 2 表示 16-bit 采样，即 short 型
//    private static final int BUFFER_SIZE = 48000 * BYTES_PER_FRAME * NUM_FRAMES; // 缓冲区大小
////    private static final int BUFFER_SIZE = 16 * 2 * 4 * 1024;
//    private static final byte[] audioBuffer = new byte[BUFFER_SIZE];
//    private static int audioBufferIndex = 0;
//
//    private static void NoCMD(XYChart.Series<Number, Number> series) throws Exception {
//
//        AVDictionary options = new AVDictionary(null);
//
//        AVFormatContext formatContext = new AVFormatContext(options);
////        String RTMPAddress = "rtmp://192.168.100.25:1935/rtmp_live/ch17";
////        String RTMPAddress = "rtmp://192.168.100.170:1935/hlsram/live10";
////        String RTMPAddress = "rtmp://dev2.idmakers.cn:1935/hlsram/live10";
//        String RTMPAddress = "rtmp://192.168.100.51:1935/live/str1";
//        int ret = avformat_open_input(formatContext, RTMPAddress, null, options);
//        if (ret < 0) {
//            // 打开输入流失败
//            throw new Exception("打开输入流失败");
//        }
//        PointerPointer<AVStream> streams = new PointerPointer<>(0);
//
//        ret = avformat_find_stream_info(formatContext, streams);
//        if (ret < 0) {
//            // 获取流信息失败
//            throw new Exception("获取流信息失败");
//        }
//        int audioStreamIndex = -1;
//        for (int i = 0; i < formatContext.nb_streams(); i++) {
//            AVStream stream = formatContext.streams(i);
//            if (stream.codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
//                audioStreamIndex = i;
//                break;
//            }
//        }
//        if (audioStreamIndex == -1) {
//            // 没有找到音频流
//            throw new Exception("没有找到音频流");
//        }
//        AVCodecParameters codecParameters = formatContext.streams(audioStreamIndex).codecpar();
//        printParams(codecParameters);
//        int sampleRate = codecParameters.sample_rate();
//        int channels = codecParameters.channels();
//        AVCodec codec = avcodec_find_decoder(codecParameters.codec_id());
//        if (codec == null) {
//            // 找不到解码器
//            throw new Exception("找不到解码器");
//        }
//        System.out.println(codec.long_name().getString());
//        AVCodecContext codecContext = avcodec_alloc_context3(codec);
//        if (codecContext == null) {
//            // 分配解码器上下文失败
//            throw new Exception("分配解码器上下文失败");
//        }
//        ret = avcodec_parameters_to_context(codecContext, codecParameters);
//        if (ret < 0) {
//            // 将解码器参数复制到解码器上下文失败
//            throw new Exception("将解码器参数复制到解码器上下文失败");
//        }
//        ret = avcodec_open2(codecContext, codec, (PointerPointer) null);
//        if (ret < 0) {
//            // 打开解码器失败
//            throw new Exception("打开解码器失败");
//        }
//
//        AVPacket packet = av_packet_alloc();
//        AVFrame frame = av_frame_alloc();
//        while (av_read_frame(formatContext, packet) >= 0) {
//            av_frame_get_buffer(frame, 0);
//            if (packet.stream_index() == audioStreamIndex) {
//                ret = avcodec_send_packet(codecContext, packet);
//                if (ret < 0) {
//                    // 发送数据包到解码器失败
//                    throw new Exception("发送数据包到解码器失败");
//                }
//                while (ret >= 0) {
//                    ret = avcodec_receive_frame(codecContext, frame);
//                    if (ret == AVERROR_EOF || ret == AVERROR_EAGAIN() || ret == AVERROR_INPUT_CHANGED || ret == AV_CODEC_FLAG_DROPCHANGED) {
//                        break;
//                    } else if (ret < 0) {
//                        // 从解码器接收帧失败
//                        throw new Exception("从解码器接收帧失败");
//                    }
//
//                    int numSamples = frame.nb_samples(); //1024
//                    int numChannels = av_get_channel_layout_nb_channels(frame.channel_layout());//2
//                    int sampleSize = av_get_bytes_per_sample(frame.format());//4
////                    System.out.println("frame numSamples: " + numSamples);
////                    System.out.println("frame numChannels: " + numChannels);
////                    System.out.println("frame sampleSize: " + sampleSize);
//
////                    int numSamples = 1024; //1024
////                    int numChannels = 2;//2
////                    int sampleSize = 4;//4
//
//
//                    // 将多个音频帧合并为一个更大的音频帧
//                    byte[] mergedFrame = new byte[numSamples * NUM_FRAMES * BYTES_PER_FRAME];
//                    for (int i = 0; i < NUM_FRAMES; i++) {//16
//                        int dataOffset = i * numSamples * numChannels * sampleSize;
//
//                        for (int j = 0; j < numSamples; j++) {//1024
//
//                            for (int k = 0; k < numChannels; k++) {//2
//
//                                BytePointer frameData = frame.data(k);
//                                if (frameData.isNull()){
//                                    continue;
//                                }
//
////                                frameData.capacity(126976);
////                                ByteBuffer byteBuffer = frameData.asBuffer();
////                                System.out.println(frameData.address());
////                                System.out.println(ByteBuffer.wrap(byteBuffer.array()));
//                                for (int l = 0; l < sampleSize; l++) {//4
//                                    int mergedOffset = (i * numSamples + j) * BYTES_PER_FRAME + k * sampleSize + l;
//                                    long index = dataOffset + (long) j * sampleSize + l;
////                                    System.out.println(byteBuffer.get((int) index));
////                                    System.out.println(frameData.get(index));
////                                    mergedFrame[mergedOffset] = byteBuffer.get((int) index);
//                                    mergedFrame[mergedOffset] = frameData.get(index);
////                                    System.out.println("mergedOffset:" + mergedOffset);
////                                    System.out.println("index:" + index);
//                                }
//                            }
//                        }
//                    }
//
//                    // 追加到音频缓存区
//                    int dataSize = mergedFrame.length;
//                    if (audioBufferIndex + dataSize > BUFFER_SIZE) {
//                        // 缓冲区已满，向音频处理引擎发送数据并清空缓冲区
//                        sendAudioDataToAudioEngine(audioBuffer, series);
//                        audioBufferIndex = 0;
//                    }
//                    System.arraycopy(mergedFrame, 0, audioBuffer, audioBufferIndex, dataSize);
//                    audioBufferIndex += dataSize;
//                }
//            }
//            av_packet_unref(packet);
//        }
//    }
//
//    private static void printParams(AVCodecParameters codecParameters) {
//        int codecType = codecParameters.codec_type();
//        int codecId = codecParameters.codec_id();
//        int sampleRate = codecParameters.sample_rate();
//        int channels = codecParameters.channels();
//        long channelLayout = codecParameters.channel_layout();
//        long bitRate = codecParameters.bit_rate();
//        int frameSize = codecParameters.frame_size();
//        int bitsPerCodedSample = codecParameters.bits_per_coded_sample();
//        int bitsPerRawSample = codecParameters.bits_per_raw_sample();
//        int profile = codecParameters.profile();
//        int level = codecParameters.level();
//        int width = codecParameters.width();
//        int height = codecParameters.height();
//
//        int numDelay = codecParameters.initial_padding();
//        int denDelay = codecParameters.trailing_padding();
//
//
//        System.out.println("Codec Type: " + codecType);
//        System.out.println("Codec ID: " + codecId);
//        System.out.println(" Rate: " + sampleRate);
//        System.out.println("Channels: " + channels);
//        System.out.println("Channel Layout: " + channelLayout);
//        System.out.println("Bit Rate: " + bitRate);
//        System.out.println("Frame Size: " + frameSize);
//        System.out.println("Bits Per Coded Sample: " + bitsPerCodedSample);
//        System.out.println("Bits Per Raw Sample: " + bitsPerRawSample);
//        System.out.println("Profile: " + profile);
//        System.out.println("Level: " + level);
//        System.out.println("Width: " + width);
//        System.out.println("Height: " + height);
//        System.out.println("Delay: " + (double) numDelay / denDelay);
//    }
//
//
//    public static void sendAudioDataToAudioEngine(byte[] pcmData, XYChart.Series<Number, Number> series) {
//        double[] audioData = applyHammingWindow(pcmData);
//        if (hasDCBias(audioData)){
//            audioData = removeDCBias(audioData);
//        }
//
//        AudioFFT audioFFT = new AudioFFT(audioData);
//        audioFFT.fft();
//        double[] fftData = audioFFT.getFFTData();
//        double[] amplitude = audioFFT.getFFTAmplitude4SampleSize();
//
//        amplitude = audioFFT.normalizeAmplitude(amplitude);
//
//
//        amplitude = meanFilter(amplitude, 3);
//
//        amplitude = compress(amplitude, 10);
//
//
//        amplitude = smooth(amplitude, 50);
//
//        printAverage(amplitude);
//        oldData = data;
//        data = Arrays.copyOfRange(amplitude, 0, 257);
////        System.out.println("欧几里得距离++++++++++++" + calculateDifference(oldData, data));
//        double rmseValue = calculateRMSE(oldData, data);
//
//
//        if (rmseValue >= 0.04){
//            System.out.println("该音频为人声，RMSE值为" + rmseValue);
//        } else if (rmseValue >= 0.02) {
//            System.out.println("该音频为白噪声，RMSE值为" + rmseValue);
//        } else {
//            System.out.println("该音频为静音，RMSE值为" + rmseValue);
//        }
//
//
//
//
//        if (series != null){
//            javafx.application.Platform.runLater(() -> {series.getData().clear();series.setData(getSeriesData());});
//        }
//    }
//
//    public static double[] convertPcmToDouble(byte[] pcmData, int bytesPerSample) {
//        int samples = pcmData.length / bytesPerSample;
//        double[] signal = new double[samples];
//        for (int i = 0; i < samples; i++) {
//            int sampleValue = 0;
//            for (int j = 0; j < bytesPerSample; j++) {
//                int aByte = pcmData[i * bytesPerSample + j];
//                sampleValue += aByte << (j * 8);
//            }
//            signal[i] = sampleValue / (double) (1L << (8 * bytesPerSample - 1));
//        }
//        return signal;
//    }
//
//    public static double calculateRMSE(double[] arr1, double[] arr2) {
//        // 两个数组长度必须，否则无法比较
//        if (arr1.length != arr2.length) {
//            return -1;
//        }
//        double sum = 0;
//        double mean1 = 0;
//        double mean2 = 0;
//        for (int i = 0; i < arr1.length;i++) {
//            mean1 += arr1[i];
//            mean2 += arr2[i];
//        }
//        mean1 /= arr1.length;
//        mean2 /= arr2.length;
//        double diff = mean2 - mean1; // 计算两个平均值的差值
//        for (int i = 0; i < arr2.length; i++) {
//            arr2[i] -= diff; // 对波形2的每个元素进行平移
//            double error = arr1[i] - arr2[i];
//            sum += error * error;
//        }
//        double mean = sum /arr1.length;
//        return Math.sqrt(mean);
//    }
//
//    public static double[] pcmToSignal(byte[] pcmData, int bytesPerSample) {
//        int samples = pcmData.length / bytesPerSample;
//        double[] signal = new double[samples];
//        for (int i = 0; i < samples; i++) {
//            short sampleValue = 0;
//            for (int j = 0; j < bytesPerSample; j++) {
//                int aByte = pcmData[i * bytesPerSample + j];
//                sampleValue += aByte << (j * 8);
//            }
//                signal[i] = sampleValue / (double) (1L << 15);
//        }
//        return signal;
//    }
//
//
//    private static void printIsNoise(double[] data){
//        double[] amplitude = new double[data.length];
//        amplitude = Arrays.copyOf(data, data.length);
//        Arrays.sort(amplitude); // 对幅度数组升序排序
//
//        // 计算均值和标准差
//        double mean = 0;
//        for (double a : amplitude) {
//            mean += a;
//        }
//        mean /= amplitude.length;
//
//        double variance = 0;
//        for (double a : amplitude) {
//            variance += (a - mean) * (a - mean);
//        }
//        variance /= (amplitude.length - 1);
//        double stddev = Math.sqrt(variance);
//
//        // 计算3sigma范围
//        double upperLimit = mean + 3 * stddev;
//        double lowerLimit = mean - 3 * stddev;
//
//        // 统计3sigma范围之外的数值个数
//        int count = 0;
//        for (double a : amplitude) {
//            if (a < lowerLimit || a > upperLimit) {
//                count++;
//            }
//        }
//        double ratio = (double) count / amplitude.length;
//        if (ratio < 0.019) {
//            System.out.println("该音频为白噪声====" + ratio);
//        } else {
//            System.out.println("该音频为人声====" + ratio);
//        }
//    }
//
//    private static void printAverage(double[] data){
//        double sum = 0;
//        for (double datum : data) {
//            sum += datum;
//        }
//
////        System.out.println("总平均值------------" + sum / data.length);
//    }
//
//
//    @Override
//    public void start(Stage stage) {
//        // 创建x轴和y轴
//        final NumberAxis xAxis = new NumberAxis();
//        final NumberAxis yAxis = new NumberAxis();
//        xAxis.setLabel("X");
//        yAxis.setLabel("Y");
//
//        yAxis.setAutoRanging(false);
//        yAxis.setUpperBound(1d);
//        yAxis.setLowerBound(-1d);
//        yAxis.setTickUnit(0.05);
//
//
//        // 设置x轴范围为1到10
////        xAxis.setAutoRanging(false);
////        xAxis.setUpperBound(2000);
//
//        // 创建折线图
//        final javafx.scene.chart.LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
//        lineChart.setTitle("频域能量图");
//        lineChart.createSymbolsProperty().set(false);
//
//        // 创建数据系列
//        XYChart.Series<Number, Number> series = new XYChart.Series<>();
//        series.setName("频域能量值");
//
//        // 向数据系列中添加数据
//        for (int i = 0; i < data.length; i++) {
//            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, data[i]);
////            dataPoint.setNode(new Circle(2));
//            series.getData().add(dataPoint);
//        }
//
//
//        // 将数据系列添加到折线图上
//        lineChart.getData().add(series);
//
//        // 创建场景
//        Scene scene = new Scene(lineChart, 1600, 600);
//        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
//
//        // 设置窗口标题并显示窗口
//        stage.setTitle("音频流频域能量图");
//        stage.setScene(scene);
//        stage.show();
//        new Thread(() -> {
//            try {
//                NoCMD(series);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }).start();
//
//
//    }
//
//
//    private static ObservableList<XYChart.Data<Number, Number>> getSeriesData() {
//        ObservableList<XYChart.Data<Number, Number>> seriesData = FXCollections.observableArrayList();
//        double sum = 0;
//        // 向数据系列中添加数据
//        for (int i = 0; i < data.length; i++) {
////        for (int i = 0; i < 1000; i++) {
//            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, data[i] + 0.1);
////            dataPoint.setNode(new Circle(2));
//            seriesData.add(dataPoint);
//            sum += (data[i] + 0.1);
//        }
//
////        System.out.println("平均值===========" + sum / data.length);
//        return seriesData;
//    }
//
//    public static double[] compress(double[] data, double compressionFactor) {
//        if (compressionFactor <= 0) {
//            throw new IllegalArgumentException("Compression factor must be greater than 0");
//        }
//
//        double[] compressedData = new double[data.length];
//
//        for (int i = 0; i < data.length; i++) {
//            compressedData[i] = Math.tanh(compressionFactor * data[i]);
//        }
//
//        return compressedData;
//    }
//
//    public static boolean hasDCBias(double[] pcmData) {
//        double mean = 0.0;
//        for(int i = 0; i < pcmData.length; i++) {
//            mean += pcmData[i];
//        }
//        mean /= pcmData.length;
//        return mean != 0.0;
//    }
//
//    public static double[] removeDCBias(double[] pcmData) {
//        double mean = 0.0;
//        for(int i = 0; i < pcmData.length; i++) {
//            mean += pcmData[i];
//        }
//         mean /= pcmData.length;
//        for(int i = 0; i < pcmData.length; i++) {
//            pcmData[i] -= mean;
//        }
//        return pcmData;
//    }
//
//    public static double[] meanFilter(double[] arr, int n) {
//        double[] result = new double[arr.length];
//        int half = n / 2;
//        double sum = 0.0;
//        for (int i = 0; i < arr.length; i++) {
//            if (i >= half) {
//                sum -= arr[i - half];
//            }
//            if (i + half < arr.length) {
//                sum += arr[i + half];
//            }
//            if (i + half >= arr.length) {
//                result[i] = sum / (arr.length - i + half);
//            } else if (i >= half) {
//                result[i] = sum / n;
//            }
//        }
//        return result;
//    }
//
//    public static double[] smooth(double[] data, int windowSize) {
//        double[] smoothed = new double[data.length];
//        for (int i = 0; i < data.length - windowSize + 1; i++) {
//            double sum = 0;
//            for (int j = i; j < i + windowSize; j++) {
//                sum += data[j];
//                smoothed[i + windowSize / 2] = sum / windowSize;
//            }
//        }
//        return smoothed;
//    }
//
//    public static double[] applyHammingWindow(byte[] pcmData) {
//        // 将PCM数据转换为double数组
//        double[] pcmDataDouble = new double[pcmData.length / 4];
//        for (int i = 0; i < pcmDataDouble.length; i++) {
//            short left = (short) ((pcmData[4 * i] & 0xFF) | (pcmData[4 * i + 1] << 8));
//            short right = (short) ((pcmData[4 * i + 2] & 0xFF) | (pcmData[4 * i + 3] << 8));
//            pcmDataDouble[i] = (left + right) / 2.0 / 32768.0;  // 将平均值转换为double类型，范围为-1.0到1.0
//        }
//
//        // 应用汉宁窗
//        double[] hammingWindow = new double[pcmDataDouble.length];
//        for (int i = 0; i < pcmDataDouble.length; i++) {
//            hammingWindow[i] = pcmDataDouble[i] * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (pcmDataDouble.length - 1)));
//        }
//
//        return hammingWindow;
//    }
//
//    public static double calculateDifference(double[] data1, double[] data2) {
//        double[] differences = new double[data1.length - 1];
//        double distance = 0.0;
//
//        for (int i = 0; i < differences.length; i++) {
//            double a = data1[i];
//            double b = data2[i];
//            differences[i] = Math.abs(a - b);
//        }
//
//        for (double difference : differences) {
//            distance += difference * difference;
//        }
//
//        return Math.sqrt(distance);
//    }
//
//
//}
