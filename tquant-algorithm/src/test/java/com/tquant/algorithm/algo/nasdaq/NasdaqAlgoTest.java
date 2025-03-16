package com.tquant.algorithm.algo.nasdaq;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tquant.algorithm.algos.entity.TradeRecord;
import com.tquant.algorithm.algos.entity.TradeTimeRange;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.algorithm.algos.utils.TradeTimeUtils;
import org.ta4j.core.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.tquant.algorithm.algos.utils.TradeTimeUtils.toUnixTime;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2023/05/20
 */
public class NasdaqAlgoTest {

    private static final String SYMBOL = "NQmain";
    private static final int SHARE_PER_TRADE = 1;
    private static final BigDecimal SHARE_PER_TRADE_VOL = new BigDecimal(SHARE_PER_TRADE);
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.002);

    // 每点交易盈亏 50HKD
    private static final BigDecimal PROFIT_LOSS_FACTOR = new BigDecimal(10);
    // 1单位买卖手续费 25HKD
    private static final BigDecimal TRANSACTION_FEE = new BigDecimal(3);
    private static final BigDecimal PRICE_CHANGE_FACTOR_3MIN = new BigDecimal(10);

    // 初始资金为0
    private static BigDecimal balance = BigDecimal.ZERO;
    // 累计交易费用
    private static BigDecimal accumulateFee = BigDecimal.ZERO;

    // 累计实现盈亏
    private static BigDecimal accumulatePnl = BigDecimal.ZERO;
    // 累计交易笔数
    private static Integer tradeCount = 0;
    // 初始多头持仓为0
    private static int longPosition = 0;
    // 初始空头持仓为0
    private static int shortPosition = 0;


    /**
     * 多级别X-Min Kline 综合策略结果
     * <p>
     * 基本设计思想：
     * 1. 多种时间维度 k线数据综合决策
     * 2. 截断亏损
     * 3. 让利润奔跑
     * 4. 市场具有共振关联性标的
     * 5. 优秀的仓位控制策略
     * 6. 基于历史统计数据做好充分的回测
     */
    public static void mixedMinDailyStrategy() throws InterruptedException {

        List<String> symbols = Lists.newArrayList();
        symbols.add(SYMBOL);
        FutureKType kType = FutureKType.min3;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(-5);

        int counter = 0;
        // 实际时间映射 21:30->20:30
        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20230616", "20230617", "21:30", "23:00");
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间:" + endTime);

            longPosition = 0;
            shortPosition = 0;

            // 日内盈亏（不包含交易费）
            BigDecimal pnlWithoutFee = BigDecimal.ZERO;
            // 日内实现盈亏
            BigDecimal pnl = BigDecimal.ZERO;
            // 日内交易费用
            BigDecimal tradeFee = BigDecimal.ZERO;

            // 指标：日内最高、低点位
            BigDecimal dailyHigh = BigDecimal.ZERO;
            BigDecimal dailyLow = BigDecimal.ZERO;

            // 指标：最新kline止损点位，多仓为例：上一3min kline 最低点，或者最新3min kline 跌幅>=20
            BigDecimal stopLosePrice = BigDecimal.ZERO;
            BigDecimal lastTransactionPrice = BigDecimal.ZERO;
            BigDecimal transactionPrice = BigDecimal.ZERO;

            // 指标：连续上涨、下跌 xmin kline数
            int consecutiveRise = 0;
            int consecutiveFall = 0;

            // 记录日内交易记录
            List<TradeRecord> tradeRecords = new ArrayList<>();

            // 获取日内1-3-5分钟级别k线数据
            // List<FutureKlineItem> kLineItems1Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min1, toUnixTime(beginTime), toUnixTime(endTime), 400);
            List<FutureKlineItem> kLineItems3Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min3, toUnixTime(beginTime), toUnixTime(endTime), 800);
            // List<FutureKlineItem> kLineItems5Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min5, toUnixTime(beginTime), toUnixTime(endTime), 400);

            if (kLineItems3Min.size() == 0) {
                System.out.println("返回kline数据为空");
                continue;
            } else {
                System.out.println("返回kline数据:" + kLineItems3Min.size());
            }

            FutureKlineItem klineItem = kLineItems3Min.get(0);
            if (klineItem.getClose().compareTo(klineItem.getOpen()) > 0) {
                consecutiveRise = 1;
            } else {
                consecutiveFall = 1;
            }

            for (int i = 1; i < kLineItems3Min.size(); i++) {
                klineItem = kLineItems3Min.get(i);
                FutureKlineItem prevKlineItem = kLineItems3Min.get(i - 1);

                // 日内最高、最低点，用于做相对位置的参考
                dailyHigh = dailyHigh.compareTo(klineItem.getHigh()) > 0 ? dailyHigh : klineItem.getHigh();
                dailyLow = dailyLow.compareTo(klineItem.getLow()) < 0 ? dailyLow : klineItem.getLow();

                // 记录交易时间
                LocalDateTime tradeDateTime = LocalDateTime.ofEpochSecond(klineItem.getLastTime() / 1000, 0, offset);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String tradeTime = tradeDateTime.format(formatter);

                BigDecimal closePrice = klineItem.getClose();

                if (longSignal(klineItem, prevKlineItem, consecutiveFall)) {
                    if (longPosition == 0 && shortPosition == 0) {
                        transactionPrice = closePrice;
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        // 买入1单位多头仓位
                        longPosition += SHARE_PER_TRADE;

                        // 交易费用
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        // 多单止损价格
                        stopLosePrice = closePrice.subtract(new BigDecimal(25));
                    } else if (longPosition == 0 && shortPosition == SHARE_PER_TRADE) { // 当前持有一单位空头持仓
                        // 结合止损价设定实际交易价格
                        if (closePrice.compareTo(stopLosePrice) > 0) {
                            transactionPrice = stopLosePrice;
                        } else {
                            transactionPrice = closePrice;
                        }
                        // 先平仓空头1单位
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        shortPosition = 0;

                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        pnlWithoutFee = pnlWithoutFee.add(lastTransactionPrice.subtract(transactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(SHARE_PER_TRADE_VOL));

                        // 做多1单位
                        tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);

                        longPosition += SHARE_PER_TRADE;
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        // stopLosePrice = openPrice.subtract(new BigDecimal(5));
                        stopLosePrice = closePrice.subtract(new BigDecimal(25));
                    }
                } else if (shortSignal(klineItem, prevKlineItem, consecutiveRise)) {
                    if (longPosition == 0 && shortPosition == 0) {
                        transactionPrice = closePrice;
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        // 买入1单位多头仓位
                        shortPosition += SHARE_PER_TRADE;

                        // 交易费用
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        // 空单止损价格
                        stopLosePrice = closePrice.add(new BigDecimal(25));
                    } else if (longPosition == SHARE_PER_TRADE && shortPosition == 0) {
                        // 结合止损价设定实际交易价格
                        if (closePrice.compareTo(stopLosePrice) < 0) {
                            transactionPrice = stopLosePrice;
                        } else {
                            transactionPrice = closePrice;
                        }
                        // 先平仓多头1单位
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        longPosition = 0;

                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        pnlWithoutFee = pnlWithoutFee.add(transactionPrice.subtract(lastTransactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(SHARE_PER_TRADE_VOL));

                        // 做空1单位
                        tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);

                        shortPosition += SHARE_PER_TRADE;
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        stopLosePrice = closePrice.add(new BigDecimal(25));
                    }

                    if (klineItem.getClose().compareTo(prevKlineItem.getClose()) > 0) {
                        // 当前 k 线上涨
                        consecutiveRise++;
                        consecutiveFall = 0;
                    } else if (klineItem.getClose().compareTo(prevKlineItem.getClose()) < 0) {
                        // 当前 k 线下跌
                        consecutiveFall++;
                        consecutiveRise = 0;
                    } else {
                        // 当前 k 线平盘
                        consecutiveRise = 0;
                        consecutiveFall = 0;
                    }
                }

                // 最后一根k线, 平仓日内所有的仓位
                if (i == kLineItems3Min.size() - 1) {
                    if (longPosition > 0) {
                        transactionPrice = closePrice;
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, longPosition, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        // 交易费用
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(new BigDecimal(longPosition)));
                        pnlWithoutFee = pnlWithoutFee.add(transactionPrice.subtract(lastTransactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(new BigDecimal(longPosition)));
                        // 买入1单位多头仓位
                        longPosition = 0;
                    }

                    if (shortPosition > 0) {
                        transactionPrice = closePrice;
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, shortPosition, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        // 交易费用
                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(new BigDecimal(shortPosition)));
                        pnlWithoutFee = pnlWithoutFee.add(lastTransactionPrice.subtract(transactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(new BigDecimal(shortPosition)));
                        // 买入1单位多头仓位
                        shortPosition = 0;
                    }
                }
            }

            if (tradeRecords.isEmpty()) {
                // System.out.println("无交易记录");
                return;
            }

            // System.out.println("交易记录:");
            for (TradeRecord tradeRecord : tradeRecords) {
                System.out.println(tradeRecord);
            }

            pnl = pnlWithoutFee.subtract(tradeFee);
            accumulateFee = accumulateFee.add(tradeFee);
            accumulatePnl = accumulatePnl.add(pnl);

            System.out.println("当前持仓 longPosition: " + longPosition + ", shortPosition:" + shortPosition);
            System.out.println("当日交易记录数: " + tradeRecords.size() * SHARE_PER_TRADE + ", 当日交易成本:" + tradeFee);
            System.out.println("当日收益不包含手续费 pnlWithoutFee:" + pnlWithoutFee);
            System.out.println("当日收益净值 pnl:" + pnl);
            System.out.println("累计交易记录数 tradeCount:" + (tradeCount = tradeCount + tradeRecords.size()));
            System.out.println("累计交易成本 accumulateFee:" + accumulateFee);
            System.out.println("累计收益净值 accumulatePnl:" + accumulatePnl);

            Thread.sleep(1500);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        mixedMinDailyStrategy();
    }

    private static boolean klineRise(FutureKlineItem kLineItem) {
        if (kLineItem != null) {
            return kLineItem.getClose().compareTo(kLineItem.getOpen()) > 0;
        }
        return false;
    }

    private static boolean longSignal(FutureKlineItem klineItem, FutureKlineItem prevKlineItem, int consecutiveFall) {
        if (klineItem == null || prevKlineItem == null) {
            return false;
        }
        BigDecimal changePrice = klineItem.getClose().subtract(klineItem.getOpen());
        // 当前k线变化超过阈值
        if (changePrice.compareTo(PRICE_CHANGE_FACTOR_3MIN) >= 0) {
            return true;
        }
        // 收涨且前面存在连续2根下跌k线
        return klineRise(klineItem) && consecutiveFall >= 3;
    }

    private static boolean shortSignal(FutureKlineItem klineItem, FutureKlineItem prevKlineItem, int consecutiveRise) {
        if (klineItem == null || prevKlineItem == null) {
            return false;
        }
        BigDecimal changePrice = klineItem.getOpen().subtract(klineItem.getClose());
        if (changePrice.compareTo(PRICE_CHANGE_FACTOR_3MIN) >= 0) {
            return true;
        }
        // 收跌且前面存在连续2根上涨k线
        return !klineRise(klineItem) && consecutiveRise >= 3;
    }


    public static void averageTrueRangeStat() {
        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        symbols.add("HSImain");
        FutureKType kType = FutureKType.hour2;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);
        int counter = 0;

        BigDecimal sumRange = BigDecimal.ZERO;
        int count = 0;

        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20230601", "20230610", "09:15", "11:15");
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            BigDecimal range = BigDecimal.ZERO;
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间:" + endTime);

            List<FutureKlineBatchItem> kLineItems = kLineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime(beginTime), toUnixTime(endTime), 800);
            System.out.println(Arrays.toString(kLineItems.toArray()));
            if (kLineItems.size() == 0) {
                System.out.println("返回数据为空");
                continue;
            }

            List<FutureKlineItem> klinePoints = kLineItems.get(0).getItems();
            List<FutureKlineItem> sortedKlineList = klinePoints.stream()
                    .sorted(Comparator.comparingLong(FutureKlineItem::getTime))
                    .collect(Collectors.toList());
            for (FutureKlineItem futureKlineItem : sortedKlineList) {
                // range = futureKlineItem.getClose().subtract(futureKlineItem.getOpen()).abs();
                range = futureKlineItem.getHigh().subtract(futureKlineItem.getLow()).abs();
                System.out.println("Daily range:" + range);
                count++;
                sumRange = sumRange.add(range);
            }
        }
        System.out.println("Average range:" + sumRange.divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_UP));
    }

}

