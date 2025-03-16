package com.tquant.algorithm.algo.hsimain;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tquant.algorithm.algo.KlineChartViewer;
import com.tquant.algorithm.algos.entity.TradeTimeRange;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.algorithm.algos.utils.TradeTimeUtils;
import com.tquant.algorithm.algos.utils.WeChatMessageUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.tquant.algorithm.algos.utils.TradeTimeUtils.toUnixTime;
import static com.tquant.algorithm.constants.HsimainAlgoConstants.*;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class HsimainHistoryData {

    public static void main(String[] args) throws InterruptedException {
        // drawKineCanvas();
        getHistoryData();
    }

    /**
     * 1.通过老虎证券的 Java open API 获取HSImain股指期货每分钟k线数据，获取时间段通过参数指定如 2025-03-01 09:30:00 - 2025-03-14 12:00:00
     */
    public static void drawKineCanvas() throws InterruptedException {
        List<String> symbols = Lists.newArrayList();
        symbols.add(HSIMAIN);
        FutureKType kType = FutureKType.hour3;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);

        int counter = 0;
        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList(YEAR + MONTH + "01", YEAR + MONTH + "10", "00:00", "23:59");
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间：" + endTime);

            List<FutureKlineBatchItem> kLineItems = KlineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime(beginTime), toUnixTime(endTime), 800);
            System.out.println("kLineItems.size() = " + kLineItems.size());
        }
    }


    /**
     * 写一个方法获取恒生指数2024年日k线数据
     * 通过对日k线数据，统计出
     * 1. 每天的最大振幅点数，平均振幅点数
     * 2. 收盘点数-开盘点数 取绝对值的平均值
     */
    public static void getHistoryData() {
        List<String> symbols = Lists.newArrayList();
        symbols.add(HSIMAIN);
        FutureKType kType = FutureKType.day;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);

        List<FutureKlineBatchItem> kLineItems = KlineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime("2025-03-13 00:00:00"), toUnixTime("2025-03-16 00:00:00"), 800);
        System.out.println("kLineItems.size() = " + kLineItems.size());

        // 每天的最大振幅点数，平均振幅点数
        BigDecimal totalAmplitude = BigDecimal.ZERO;
        BigDecimal totalAmplitudeCount = BigDecimal.ZERO;
        // 收盘点数-开盘点数 取绝对值的平均值
        BigDecimal totalCloseOpen = BigDecimal.ZERO;
        BigDecimal totalCloseOpenCount = BigDecimal.ZERO;

        // 创建data目录
        String dataDir = "data";
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        // 创建CSV文件
        String fileName = String.format("%s/hsimain_kline_%s.csv", dataDir, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        try (FileWriter writer = new FileWriter(fileName)) {
            // 写入CSV头
            writer.write("时间,开盘价,最高价,最低价,收盘价,振幅,开收盘价差\n");

            for (FutureKlineBatchItem kLineItem : kLineItems) {
                List<FutureKlineItem> dayKlineData = kLineItem.getItems();
                System.out.println("dayKlineData.size() = " + dayKlineData.size());
                // 按时间排序
                dayKlineData.sort(Comparator.comparing(FutureKlineItem::getTime));

                for (FutureKlineItem item : dayKlineData) {
                    BigDecimal high = item.getHigh();
                    BigDecimal low = item.getLow();
                    BigDecimal open = item.getOpen();
                    BigDecimal close = item.getClose();

                    BigDecimal amplitude = high.subtract(low);
                    totalAmplitude = totalAmplitude.add(amplitude);
                    totalAmplitudeCount = totalAmplitudeCount.add(BigDecimal.ONE);

                    BigDecimal closeOpen = close.subtract(open).abs();
                    totalCloseOpen = totalCloseOpen.add(closeOpen);
                    totalCloseOpenCount = totalCloseOpenCount.add(BigDecimal.ONE);

                    // 将数据写入CSV文件
                    // 使用正确的时间戳转换方式
                    LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(item.getTime()/1000),
                            ZoneOffset.ofHours(8)
                    );
                    System.out.println("item.getTime() = " + item.getTime());
                    String time = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            time, open, high, low, close, amplitude, closeOpen));
                }
            }

            System.out.println("数据已保存到文件: " + fileName);
        } catch (IOException e) {
            System.err.println("保存数据到文件时发生错误: " + e.getMessage());
        }

        BigDecimal averageAmplitude = totalAmplitude.divide(totalAmplitudeCount, 2, RoundingMode.HALF_UP);
        BigDecimal averageCloseOpen = totalCloseOpen.divide(totalCloseOpenCount, 2, RoundingMode.HALF_UP);

        System.out.println("averageAmplitude = " + averageAmplitude);
        System.out.println("averageCloseOpen = " + averageCloseOpen);
    }
}
