//package org.example;
//
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.embed.swing.SwingNode;
//import javafx.scene.Scene;
//import javafx.scene.layout.StackPane;
//import javafx.stage.Stage;
//import org.jfree.chart.*;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.data.xy.XYSeries;
//import org.jfree.data.xy.XYSeriesCollection;
//import javax.swing.*;
//
//public class Fx extends Application {
//    private XYSeries series = new XYSeries("FFT Analysis");
//    private XYSeriesCollection dataset = new XYSeriesCollection(series);
//
//    @Override
//    public void start(Stage stage) {
//        FfmpegDecoderForQinHai ffmpegDecoderForQinHai = new FfmpegDecoderForQinHai();
//        Thread thread = new Thread(ffmpegDecoderForQinHai);
//        thread.start();
//        final SwingNode chartSwingNode = new SwingNode();
//        createChart(chartSwingNode);
//
//        StackPane pane = new StackPane();
//        pane.getChildren().add(chartSwingNode);
//
//        stage.setTitle("JavaFX / JFreeChart Integration");
//        stage.setScene(new Scene(pane, 800, 600));
//        stage.show();
//
//        // Start a new Thread which updates the chart
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(1000);  // Adjust based on your needs
//
//                    double[] newFrequencies = getNewFrequencies(ffmpegDecoderForQinHai);  // Get new frequencies
//                    double[] newMagnitudes = getNewMagnitudes(ffmpegDecoderForQinHai);  // Get new magnitudes
//
//                    Platform.runLater(() -> {
//                        series.clear();
//                        for (int i = 0; i < newFrequencies.length; i++) {
//                            series.add(newFrequencies[i], newMagnitudes[i]);
//                        }
//                    });
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
//
//    private void createChart(final SwingNode swingNode) {
//        SwingUtilities.invokeLater(() -> {
//            // 创建图表
//            JFreeChart chart = ChartFactory.createXYLineChart(
//                    "FFT Results",
//                    "Frequency (Hz)",
//                    "Magnitude",
//                    dataset,
//                    PlotOrientation.VERTICAL,
//                    true,
//                    true,
//                    false);
//
//            ChartPanel chartPanel = new ChartPanel(chart);
//            swingNode.setContent(chartPanel);
//        });
//    }
//
//    // Implement these methods to get real time data
//    private double[] getNewFrequencies(FfmpegDecoderForQinHai thread) {
//        // Implement this method to get the refreshed data
//        return magnitudes;
//    }
//
//    private double[] getNewMagnitudes(FfmpegDecoderForQinHai thread) {
//        // Implement this method to get the refreshed data
//        return new double[] {};
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}