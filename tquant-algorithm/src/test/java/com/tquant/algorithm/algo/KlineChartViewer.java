package com.tquant.algorithm.algo;

import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class KlineChartViewer extends JFrame {

    public KlineChartViewer(String title, List<FutureKlineItem> klineItems, List<String> buySignals, List<String> sellSignals) {
        super(title);
        JFreeChart chart = createChart(klineItems);
        
        // 添加买入卖出标记
        XYPlot plot = (XYPlot) chart.getPlot();
        for (String buySignal : buySignals) {
            String[] parts = buySignal.split(",");
            int index = Integer.parseInt(parts[0]);
            if (index < klineItems.size()) {
                FutureKlineItem item = klineItems.get(index);
                double price = item.getClose().doubleValue();
                org.jfree.chart.annotations.XYTextAnnotation annotation = new org.jfree.chart.annotations.XYTextAnnotation(
                        String.format("⬆B %.2f", price), new Date(item.getTime()).getTime(), price + (plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound()) * 0.01);
                annotation.setPaint(Color.RED);
                annotation.setFont(new Font("SansSerif", Font.BOLD, 10));
                annotation.setBackgroundPaint(new Color(255, 255, 255, 200));
                plot.addAnnotation(annotation);
            }
        }

        for (String sellSignal : sellSignals) {
            String[] parts = sellSignal.split(",");
            int index = Integer.parseInt(parts[0]);
            if (index < klineItems.size()) {
                FutureKlineItem item = klineItems.get(index);
                double price = item.getClose().doubleValue();
                org.jfree.chart.annotations.XYTextAnnotation annotation = new org.jfree.chart.annotations.XYTextAnnotation(
                        String.format("⬇S %.2f", price), new Date(item.getTime()).getTime(), price - (plot.getRangeAxis().getUpperBound() - plot.getRangeAxis().getLowerBound()) * 0.01);
                annotation.setPaint(new Color(0, 150, 0));
                annotation.setFont(new Font("SansSerif", Font.BOLD, 10));
                annotation.setBackgroundPaint(new Color(255, 255, 255, 200));
                plot.addAnnotation(annotation);
            }
        }
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        setContentPane(chartPanel);
    }

    private JFreeChart createChart(List<FutureKlineItem> klineItems) {
        int size = klineItems.size();
        Date[] dates = new Date[size];
        double[] highs = new double[size];
        double[] lows = new double[size];
        double[] opens = new double[size];
        double[] closes = new double[size];
        double[] volumes = new double[size];

        for (int i = 0; i < size; i++) {
            FutureKlineItem item = klineItems.get(i);
            dates[i] = new Date(item.getTime());
            highs[i] = item.getHigh().doubleValue();
            lows[i] = item.getLow().doubleValue();
            opens[i] = item.getOpen().doubleValue();
            closes[i] = item.getClose().doubleValue();
            volumes[i] = item.getVolume();
        }

        DefaultHighLowDataset dataset = new DefaultHighLowDataset(
                "HSI Futures",
                dates,
                highs,
                lows,
                opens,
                closes,
                volumes
        );

        String chartTitle = "HSI Futures K-Line Chart - " + new SimpleDateFormat("yyyy-MM-dd").format(dates[0]);
        JFreeChart chart = ChartFactory.createCandlestickChart(
                chartTitle,
                "Time",
                "Price",
                dataset,
                true
        );

        // 自定义图表样式
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        // 设置蜡烛图样式
        CandlestickRenderer renderer = (CandlestickRenderer) plot.getRenderer();
        renderer.setUpPaint(Color.RED);
        renderer.setDownPaint(Color.GREEN);
        renderer.setDrawVolume(true);

        // 设置时间轴格式
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));

        // 设置价格轴
        NumberAxis priceAxis = (NumberAxis) plot.getRangeAxis();
        priceAxis.setAutoRangeIncludesZero(false);

        return chart;
    }

    public static void showChart(List<FutureKlineItem> klineItems, List<String> buySignals, List<String> sellSignals) {
        SwingUtilities.invokeLater(() -> {
            KlineChartViewer viewer = new KlineChartViewer("HSI Futures K-Line Chart", klineItems, buySignals, sellSignals);
            viewer.pack();
            viewer.setLocationRelativeTo(null);
            viewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            viewer.setVisible(true);
        });
    }
}