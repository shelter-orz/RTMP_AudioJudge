package org.example;


import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class AudioFFT {
    private DoubleFFT_1D fft;
    private double[] pcmData;   // PCM音频数据
    private double[] fftData;   // 变换后的频域数据
    private double[] dftData;

    /**
     * 构造函数
     * @param pcmData PCM音频数据
     */
    public AudioFFT(double[] pcmData) {
        this.pcmData = pcmData;
        this.fft = new DoubleFFT_1D(pcmData.length);
        this.fftData = new double[pcmData.length];
        this.dftData = new double[2 * pcmData.length];
    }

    /**
     * 将PCM音频数据进行FFT变换
     */
    public void fft() {
        // 对PCM音频数据进行FFT变换
        fft.realForward(pcmData);

        // 将变换后的频域数据保存到fftData数组中
        System.arraycopy(pcmData, 0, fftData, 0, pcmData.length);
    }

    public void dft() {
        System.arraycopy(pcmData, 0, dftData, 0, pcmData.length);
        fft.realForwardFull(dftData);
    }

    public double[] getDftData(){
        return dftData;
    }


    public double[] getInverseDftData(double[] fourierResult) {
        // 创建一个新的、长度为原始的傅里叶变换结果数组的复数数组
    double[] complexArray = new double[fourierResult.length];
    for (int i = 0; i < fourierResult.length; i += 2) {
        complexArray[i] = fourierResult[i];
        complexArray[i + 1] = fourierResult[i + 1];
    }

    // 创建一个新的复数数组
    DoubleFFT_1D fft = new DoubleFFT_1D(fourierResult.length / 2);
    double[] inverseArray = new double[fourierResult.length];

    // 计算反傅里叶变换
    fft.complexInverse(complexArray, true);

    // 获取傅里叶变换结果对应的时域数据
    for (int i = 0; i < complexArray.length; i += 2) {
        inverseArray[i] = complexArray[i] / (fourierResult.length / 2);
        inverseArray[i + 1] = complexArray[i + 1] / (fourierResult.length / 2);
    }

    // 仅返回实部数据
    double[] result = new double[inverseArray.length / 2];
    for (int i = 0; i < result.length; i++) {
        result[i] = inverseArray[2 * i];
    }

    return result;
}


    /**
     * 获取FFT变后的频域数据
     * @return 频域数据
     */
    public double[] getFFTData() {
        return fftData;
    }

    /**
     * 获取FFT变换后的频域数据的幅度值
     * @return 频域数据的幅度值
     */
    public double[] getFFTAmplitude() {
        double[] amplitude = new double[fftData.length / 4 + 1];

        for (int i = 0; i < amplitude.length - 1; i++) {   // 处理实部和虚部数据，计算幅度
            double re = fftData[i * 2];
            double im = fftData[i * 2 + 1];

            amplitude[i] = Math.sqrt(re * re + im * im);    // 使用欧几里德距离计算幅度值
        }
        amplitude[amplitude.length - 1] = Math.abs(fftData[1]);   // 处理Nyquist频率

        return amplitude;
    }

    public double[] getFFTAmplitude4SampleSize() {
        double[] amplitude = new double[fftData.length / (2 * 4) + 1];
        for (int i = 0; i < amplitude.length - 1; i++) {
            double re = fftData[i * 2 * 4];
            double im = fftData[i * 2 * 4 + 1];
            amplitude[i] = Math.sqrt(re * re + im * im);// 使用欧几里德距离计算幅度值
        }
        amplitude[amplitude.length - 1] = Math.abs(fftData[1]);   // 处理Nyquist频率
        return amplitude;
    }

    /**
     * 将幅度值进行归一化
     * @param amplitude 幅度值
     * @return 归一化后的幅度值
     */
    public double[] normalizeAmplitude(double[] amplitude) {
        double max = -Double.MAX_VALUE;

        // 找到最大的幅度值
        for (double a : amplitude) {
            if (a > max) {
                max = a;
            }
        }

        // 对幅度值进行归一化
        for (int i = 0; i < amplitude.length; i++) {
            amplitude[i] = amplitude[i] / max;
        }

        return amplitude;
    }

    public static void main(String[] args) {
//        AudioFFT audioFFT = new AudioFFT(pcmBuffer);
//        audioFFT.fft();
//        double[] fftData = audioFFT.getFFTData();
//        double[] amplitude = audioFFT.getFFTAmplitude();
//        amplitude = audioFFT.normalizeAmplitude(amplitude);
    }
}
