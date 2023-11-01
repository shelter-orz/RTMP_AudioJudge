//package org.example;
//
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.scene.Scene;
//import javafx.scene.chart.LineChart;
//import javafx.scene.chart.NumberAxis;
//import javafx.scene.chart.XYChart;
//import javafx.stage.Stage;
//import org.apache.commons.math3.complex.Complex;
//import org.bytedeco.ffmpeg.avcodec.AVCodec;
//import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
//import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
//import org.bytedeco.ffmpeg.avcodec.AVPacket;
//import org.bytedeco.ffmpeg.avformat.AVFormatContext;
//import org.bytedeco.ffmpeg.avformat.AVStream;
//import org.bytedeco.ffmpeg.avutil.AVFrame;
//import org.bytedeco.javacpp.PointerPointer;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.DoubleBuffer;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//
//import static org.bytedeco.ffmpeg.global.avcodec.*;
//import static org.bytedeco.ffmpeg.global.avformat.*;
//import static org.bytedeco.ffmpeg.global.avutil.*;
//
///**
// * @author: Zhou Yujie
// * @date: 2023/5/18
// **/
//public class RTMPToFFTAAC2 extends Application {
//
//    private static double[] data = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//    private static double[] oldData = {0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//
//    public static void main(String[] args) throws Exception {
////        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", "rtmp://192.168.100.25:1935/rtmp_live/ch17", "-f", "s16le", "-ac", "1", "-ar", "44100", "-");
////        Process process = pb.start();
////        InputStream inputStream = process.getInputStream();
////        byte[] buffer = new byte[1024];
////        int len;
////        ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length * 2);
////        while ((len = inputStream.read(buffer)) != -1) {
////            byteBuffer.put(buffer, 0, len);
////        }
////        byteBuffer.flip();
////        DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
////        double[] audioData = new double[doubleBuffer.remaining()];
////        doubleBuffer.get(audioData);
////        System.out.println(Arrays.toString(audioData));
//        launch(args);
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
//    private static final double[][] pcmDataBuffer = new double[NUM_FRAMES][8192];
//    private static int pcmDataBufferIndex = 0;
//
//
//    private static void NoCMD(XYChart.Series<Number, Number> series) throws Exception {
//
//        Set<String> times = new HashSet<>();
//
//        AVFormatContext formatContext = new AVFormatContext(null);
////        String RTMPAddress = "rtmp://192.168.100.25:1935/rtmp_live/ch17";
//        String RTMPAddress = "rtmp://192.168.100.170:1935/hlsram/live30";
//        int ret = avformat_open_input(formatContext, RTMPAddress, null, null);
//        if (ret < 0) {
//            // 打开输入流失败
//            throw new Exception("打开输入流失败");
//        }
//        ret = avformat_find_stream_info(formatContext, (PointerPointer) null);
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
//        int sampleRate = codecParameters.sample_rate();
//        int channels = codecParameters.channels();
//        AVCodec codec = avcodec_find_decoder(codecParameters.codec_id());
//        if (codec == null) {
//            // 找不到解码器
//            throw new Exception("找不到解码器");
//        }
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
//            if (packet.stream_index() == audioStreamIndex) {
//
//                ret = avcodec_send_packet(codecContext, packet);
//                if (ret < 0) {
//                    // 发送数据包到解码器失败
//                    throw new Exception("发送数据包到解码器失败");
//                }
//                while (ret >= 0) {
//                    ret = avcodec_receive_frame(codecContext, frame);
//                    if (ret == AVERROR_EOF || ret == AVERROR_EAGAIN()) {
//                        break;
//                    } else if (ret < 0) {
//                        // 从解码器接收帧失败
//                        throw new Exception("从解码器接收帧失败");
//                    }
//
//                    // 将解码后的音频数据转换成 PCM 编码的音频数据
//                    int numSamples = frame.nb_samples();
//                    int numChannels = av_get_channel_layout_nb_channels(frame.channel_layout());
//                    int sampleSize = av_get_bytes_per_sample(frame.format());
//                    int format = frame.format();
//
//                    double[] sourcePcmData = convertToPCMData(frame);
//                    if (pcmDataBufferIndex + 1 > NUM_FRAMES){
//                        double[] pcmData = mergeFrames(pcmDataBuffer, 512);
//                        sendAudioDataToAudioEngine(pcmData, BYTES_PER_FRAME, series, times);
//
//                        pcmDataBufferIndex = 0;
//                    }
//                    System.arraycopy(sourcePcmData, 0, pcmDataBuffer[pcmDataBufferIndex],0,sourcePcmData.length);
//                    pcmDataBufferIndex ++;
//                }
//
////                System.out.println("===========间隔========");
//            }
//            av_packet_unref(packet);
//        }
//    }
//
//    private static double[] convertToPCMData(AVFrame frame) {
//        int numSamples = frame.nb_samples();
//        int numChannels = av_get_channel_layout_nb_channels(frame.channel_layout());
//        int sampleSize = av_get_bytes_per_sample(frame.format());
//        int format = frame.format();
//        byte[] pcmData = new byte[numSamples * numChannels * sampleSize];
//        int index = 0;
//        for (int i = 0; i < numSamples; i++) {
//            for (int j = 0; j < numChannels; j++) {
//                for (int k = 0; k < sampleSize; k++) {
//                    pcmData[index++] = frame.data(j).get(i * sampleSize + k);
//                }
//            }
//        }
//        double[] doubleData = new double[numSamples * numChannels];
//
//        for (int sample = 0; sample < numSamples; sample++) {
//            for (int channel = 0; channel < numChannels; channel++) {
//                int sampleOffset = sample * numChannels + channel;
//                int byteOffset = sample * numChannels * sampleSize + channel * sampleSize;
//
//                switch (sampleSize) {
//                    case 1:
//                        doubleData[sampleOffset] = pcmData[byteOffset] / 128.0;
//                        break;
//
//                    case 2:
//                        doubleData[sampleOffset] = ((short)(((pcmData[byteOffset + 1] & 0xff) << 8) | (pcmData[byteOffset] & 0xff))) / 32768.0;
//                        break;
//
//                    case 3:
//                        doubleData[sampleOffset] = ((pcmData[byteOffset + 2] << 16) | ((pcmData[byteOffset + 1] & 0xff) << 8) | (pcmData[byteOffset] & 0xff)) / 8388608.0;
//                        break;
//
//                    case 4:
//                        doubleData[sampleOffset] = (((int)pcmData[byteOffset + 3] & 0xff) << 24)
//                            | ((pcmData[byteOffset + 2] & 0xff) << 16)
//                            | ((pcmData[byteOffset + 1] & 0xff) << 8)
//                            | (pcmData[byteOffset] & 0xff);
//                        doubleData[sampleOffset] /= (double)0x80000000;
//                        break;
//                }
//            }
//        }
//
//        double[] result = new double[numSamples * numChannels];
//
//        for (int sample = 0; sample < numSamples; sample++) {
//            for (int channel = 0; channel < numChannels; channel++) {
//                result[sample * numChannels + channel] = doubleData[sample * numChannels + channel];
//            }
//        }
//
//        return result;
//    }
//
//   /**
//            * 合并语音帧，实现语音信号的平滑连接
// * @param frames 待合并的个语音帧
// * @param hopSize 帧移大小
// * @return 合并后的语音信号
// */
//    public static double[] mergeFrames(double[][] frames, int hopSize) {
//        int numFrames = NUM_FRAMES;
//        int frameSize = 1024 * 2 * 4;
//        double[] output = new double[(numFrames-1)*hopSize + frameSize];
//
//        // 对每个帧进行汉宁窗加窗处理
//        double[] window = getHanningWindow(frameSize);
//        for (int i = 0; i < numFrames; i++) {
//            for (int j = 0; j <frameSize; j++) {
//                frames[i][j] *= window[j];
//            }
//        }
//
//        // 对于相邻的两个帧，进行重叠-相加操作
//        for (int i = 0; i < numFrames-1; i++) {
//            for (int j = 0; j < hopSize; j++) {
//                output[i*hopSize + j] += 0.5*(frames[i][j] + frames[i+1][frameSize-hopSize+j]);
//                output[(i+1)*hopSize+frameSize-hopSize+j] += 0.5*(frames[i][j] + frames[i+1][frameSize-hopSize+j]);
//            }
//            for (int j = hopSize; j < frameSize - hopSize; j++) {
//                output[i*hopSize + j] += frames[i][j];
//                output[(i+1)*hopSize + j] += frames[i+1][j];
//            }
//        }
//
//        // 处理第一个帧
//        for (int j = 0; j < frameSize; j++) {
//            output[j] = frames[0][j];
//        }
//        // 处理最后一个帧
//        for (int j = 0; j < hopSize; j++) {
//            output[(numFrames-1)*hopSize+frameSize-hopSize+j] += frames[numFrames-1][frameSize-hopSize+j];
//        }
//        for (int j = hopSize; j < frameSize; j++) {
//            output[(numFrames-1)*hopSize+j] = frames[numFrames-1][j];
//        }
//
//        return output;
//    }
//
//    /**
//     * 获取汉宁窗
//     * @param size 窗口大小
//     * @return 汉宁窗
//     */
//    public static double[] getHanningWindow(int size) {
//        double[] window = new double[size];
//        for (int i = 0; i < size; i++) {
//            window[i] = 0.5 - 0.5*Math.cos(2*Math.PI*i/(size-1));
//        }
//        return window;
//    }
//
//    private static void sendAudioDataToAudioEngine(double[] audioData, int sampleSize, XYChart.Series<Number, Number> series, Set<String> times) {
//
////        byte[] pcmData，sampleSize为4，aac编码解码得到的pcmData byte数组如何处理才可以进行fft
//
//
////            double[] audioData = convertPcmToDouble(pcmData, sampleSize);
////        double[] audioData = applyHammingWindow(pcmData);
//
//        if (hasDCBias(audioData)){
//            audioData = removeDCBias(audioData);
//        }
//
//        AudioFFT audioFFT = new AudioFFT(audioData);
//        audioFFT.dft();
////        double[] fftData = audioFFT.getFFTData();
//        double[] dftData = audioFFT.getDftData();
//
//        double[] window = new double[dftData.length];
//        for (int i = 0; i < dftData.length; i++) {
//            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (dftData.length - 1)));
//        }
//        for (int i = 0; i < dftData.length; i++) {
//            dftData[i] *= window[i];
//        }
//
//        double[] reduceData = reduce(dftData);
//
//        double[] inverseDftData = audioFFT.getInverseDftData(reduceData);
//
//        AudioFFT audioFFTProcessed = new AudioFFT(inverseDftData);
//        audioFFTProcessed.fft();
//        double[] amplitude = audioFFTProcessed.getFFTAmplitude4SampleSize();
//        amplitude = audioFFT.normalizeAmplitude(amplitude);
//
//
//
////        amplitude = meanFilter(amplitude, 3);
//
////        amplitude = compress(amplitude, 5);
//
//
////        amplitude = smooth(amplitude, 50);
//
//        printAverage(amplitude);
//        oldData = data;
////        data = Arrays.copyOfRange(amplitude, 0, 4000);
//        data = amplitude;
//        System.out.println("欧几里得距离++++++++++++" + calculateDifference(oldData, data));
//
//            Platform.runLater(() -> series.setData(getSeriesData()));
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
//    private static double[] reduce(double[] fourierResult){
//        int n = fourierResult.length;
//        double[] absFourierResult = new double[n];
//        for (int i = 0; i < n; i++) {
//            absFourierResult[i] = Math.abs(fourierResult[i]);
//        }
//
//        double[] avgFourierResult = new double[n];
//        double mean = 0.0;
//        for (int i = 0; i < n; i++) {
//            mean += absFourierResult[i];
//        }
//        mean /= n;
//
//        double stdDev = 0.0;
//        for (int i = 0; i < n; i++) {
//            stdDev += Math.pow(absFourierResult[i] - mean, 2);
//        }
//        stdDev /= n;
//        stdDev = Math.sqrt(stdDev);
//
//        double threshold = mean - stdDev;
//
//        for (int i = 0; i < n; i++) {
//            if (absFourierResult[i] < threshold) {
//                fourierResult[i] = 0.0;
//            }
//        }
//        return fourierResult;
//    }
//
//    private static void printAverage(double[] data){
//        double sum = 0;
//        for (double datum : data) {
//            sum += datum;
//        }
//
//        System.out.println("总平均值------------" + sum / data.length);
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
//        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
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
//        System.out.println("平均值===========" + sum / data.length);
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
