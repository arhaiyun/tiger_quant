package com.tquant.core.chart;

import com.tquant.core.model.data.Bar;
import com.tquant.core.indicators.Indicators;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * K线图表显示组件
 *
 * @author arhaiyun
 * @date 2025/03/10
 */
public class ChartViewer extends Pane {
    private NumberAxis xAxis;
    private NumberAxis priceAxis;
    private NumberAxis volumeAxis;

    private XYChart.Series<Number, Number> candlestickSeries;
    private XYChart.Series<Number, Number> volumeSeries;
    private List<XYChart.Series<Number, Number>> indicatorSeries;

    private Indicators indicators;
    private double zoomFactor = 1.0;

    public ChartViewer() {
        initialize();
    }

    private void initialize() {
        xAxis = new NumberAxis();
        priceAxis = new NumberAxis();
        volumeAxis = new NumberAxis();

        candlestickSeries = new XYChart.Series<>();
        volumeSeries = new XYChart.Series<>();
        indicatorSeries = new ArrayList<>();

        indicators = new Indicators();

        // 设置缩放监听
        setOnScroll(this::handleScroll);
    }

    /**
     * 更新K线数据
     *
     * @param bars K线数据列表
     */
    public void updateChart(List<Bar> bars) {
        candlestickSeries.getData().clear();
        volumeSeries.getData().clear();

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);

            // 添加K线数据点
            XYChart.Data<Number, Number> candleData = new XYChart.Data<>(i, bar.getClose());
            candlestickSeries.getData().add(candleData);

            // 添加成交量数据
            XYChart.Data<Number, Number> volumeData = new XYChart.Data<>(i, bar.getVolume());
            volumeSeries.getData().add(volumeData);

            // 添加数据提示
            Tooltip tooltip = new Tooltip(
                    String.format("日期: %s\n开盘: %.2f\n最高: %.2f\n最低: %.2f\n收盘: %.2f\n成交量: %d",
                            bar.getTime(),
                            bar.getOpen(),
                            bar.getHigh(),
                            bar.getLow(),
                            bar.getClose(),
                            bar.getVolume()
                    )
            );
            Tooltip.install(candleData.getNode(), tooltip);
        }
    }

    /**
     * 添加技术指标
     *
     * @param type   指标类型
     * @param params 指标参数
     */
    public void addIndicator(String type, Map<String, Object> params) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        // TODO: 根据不同指标类型计算并添加指标数据
        indicatorSeries.add(series);
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            double delta = event.getDeltaY();
            if (delta > 0) {
                zoomFactor *= 1.1;
            } else {
                zoomFactor /= 1.1;
            }

            // 更新图表缩放
            xAxis.setAutoRanging(false);
            double currentLower = xAxis.getLowerBound();
            double currentUpper = xAxis.getUpperBound();
            double center = (currentLower + currentUpper) / 2;
            double newWidth = (currentUpper - currentLower) / zoomFactor;
            xAxis.setLowerBound(center - newWidth / 2);
            xAxis.setUpperBound(center + newWidth / 2);
        }
    }
}