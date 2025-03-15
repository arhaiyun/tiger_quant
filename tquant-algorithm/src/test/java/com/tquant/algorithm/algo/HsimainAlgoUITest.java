package com.tquant.algorithm.algo;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tquant.algorithm.algos.entity.TradeTimeRange;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.algorithm.algos.utils.TradeTimeUtils;
import com.tquant.algorithm.constants.HsimainAlgoConstants;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.tquant.algorithm.algos.utils.TradeTimeUtils.toUnixTime;
import static com.tquant.algorithm.constants.HsimainAlgoConstants.*;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class HsimainAlgoUITest {

    public static void main(String[] args) throws InterruptedException {
         drawKineCanvas();
    }

    /**
     * 1.通过老虎证券的 Java open API 获取HSImain股指期货每分钟k线数据，获取时间段通过参数指定如 2025-03-01 09:30:00 - 2025-03-14 12:00:00
     */
    public static void drawKineCanvas() throws InterruptedException {
        List<String> symbols = Lists.newArrayList();
        symbols.add(SYMBOL);
        FutureKType kType = FutureKType.min3;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);

        int counter = 0;
        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList(YEAR + MONTH + "04", YEAR + MONTH + "04", DAY_BEGIN_TIME, DAY_END_TIME);
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间：" + endTime);

            List<FutureKlineBatchItem> kLineItems = KlineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime(beginTime), toUnixTime(endTime), 800);
            if (kLineItems != null && !kLineItems.isEmpty()) {
                List<FutureKlineItem> dayKlineData = kLineItems.get(0).getItems();
                // 按时间排序
                dayKlineData.sort(Comparator.comparing(FutureKlineItem::getTime));

                // 初始化连续上涨、下跌计数和点数
                int consecutiveRise = 0;
                int consecutiveFall = 0;
                BigDecimal consecutiveRisePoint = BigDecimal.ZERO;
                BigDecimal consecutiveFallPoint = BigDecimal.ZERO;

                // 遍历K线数据，计算多空信号
                List<String> buySignals = new ArrayList<>();
                List<String> sellSignals = new ArrayList<>();

                for (int i = 1; i < dayKlineData.size(); i++) {
                    FutureKlineItem klineItem = dayKlineData.get(i);
                    FutureKlineItem prevKlineItem = dayKlineData.get(i - 1);

                    // 更新连续上涨下跌计数
                    if (klineItem.getClose().compareTo(klineItem.getOpen()) >= 0) {
                        consecutiveRise++;
                        consecutiveFall = 0;
                        consecutiveRisePoint = consecutiveRisePoint.add(klineItem.getClose().subtract(klineItem.getOpen()));
                        consecutiveFallPoint = BigDecimal.ZERO;
                    } else {
                        consecutiveFall++;
                        consecutiveRise = 0;
                        consecutiveFallPoint = consecutiveFallPoint.add(klineItem.getOpen().subtract(klineItem.getClose()));
                        consecutiveRisePoint = BigDecimal.ZERO;
                    }

                    // 判断多空信号
                    if (longSignal(klineItem, prevKlineItem, consecutiveFall, consecutiveFallPoint)) {
                        buySignals.add(String.format("%d,B", i));
                    } else if (shortSignal(klineItem, prevKlineItem, consecutiveRise, consecutiveRisePoint)) {
                        sellSignals.add(String.format("%d,S", i));
                    }
                }

                // 为每个交易日创建单独的K线图，并标记买卖信号
                KlineChartViewer.showChart(dayKlineData, buySignals, sellSignals);
                // 添加短暂延迟，确保窗口正确显示
                Thread.sleep(1000);
            }
        }
    }


    private static boolean longSignal(FutureKlineItem klineItem, FutureKlineItem prevKlineItem, int consecutiveFall, BigDecimal consecutiveFallPoint) {
        if (klineItem == null || prevKlineItem == null) {
            return false;
        }
        BigDecimal changePrice = klineItem.getClose().subtract(klineItem.getOpen());
        // 当前k线变化超过阈值
        if (changePrice.compareTo(PRICE_CHANGE_FACTOR) >= 0) {
            return true;
        }
        // 最低点拉升回超过阈值点数
        BigDecimal highChangePrice = klineItem.getClose().subtract(klineItem.getLow());
        if (highChangePrice.compareTo(PRICE_CHANGE_FACTOR_HL) >= 0) {
            return true;
        }
        // 收涨且前面存在连续n根下跌k线
        return klineRise(klineItem) && consecutiveFall >= ACCUMULATE_CNT && consecutiveFallPoint.compareTo(PRICE_CHANGE_FACTOR_CONSECUTIVE) >= 0;
    }

    private static boolean shortSignal(FutureKlineItem klineItem, FutureKlineItem prevKlineItem, int consecutiveRise, BigDecimal consecutiveRisePoint) {
        if (klineItem == null || prevKlineItem == null) {
            return false;
        }
        BigDecimal changePrice = klineItem.getOpen().subtract(klineItem.getClose());
        if (changePrice.compareTo(PRICE_CHANGE_FACTOR) >= 0) {
            return true;
        }
        BigDecimal highChangePrice = klineItem.getHigh().subtract(klineItem.getClose());
        if (highChangePrice.compareTo(PRICE_CHANGE_FACTOR_HL) >= 0) {
            return true;
        }

        // 收跌且前面存在连续n根上涨k线
        return !klineRise(klineItem) && consecutiveRise >= ACCUMULATE_CNT && consecutiveRisePoint.compareTo(PRICE_CHANGE_FACTOR_CONSECUTIVE) >= 0;
    }

    private static boolean klineRise(FutureKlineItem kLineItem) {
        if (kLineItem != null) {
            return kLineItem.getClose().compareTo(kLineItem.getOpen()) > 0;
        }
        return false;
    }

}
