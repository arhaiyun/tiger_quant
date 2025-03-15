package com.tquant.algorithm.algo;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tquant.algorithm.algos.entity.TradeRecord;
import com.tquant.algorithm.algos.entity.TradeTimeRange;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.algorithm.algos.utils.TradeTimeUtils;
import com.tquant.algorithm.constants.HsimainAlgoConstants.*;
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
import static com.tquant.algorithm.constants.HsimainAlgoConstants.*;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class HsimainAlgo3MinTest {

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

    public static void main(String[] args) throws InterruptedException {
        // testMinDailyStrategy();
        // System.out.println(TradeTimeUtils.getTradeTimeList("20240101", "20240601", "09:30", "11:30"));
        System.out.println("Stop Lose Point:" + STOP_LOSE_POINT_3MIN);
        mixedMinDailyStrategy();
//        averageTrueRangeStat();
    }

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
        ZoneOffset offset = ZoneOffset.ofHours(8);

        int counter = 0;
//        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("2024" + month + "01", "2024" + month + "31", dayBeginTime, dayEndTime);
        // 获取回测数据范围
        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList(YEAR + "0304", YEAR + "0304", DAY_BEGIN_TIME, DAY_END_TIME);
        // 针对每天的交易数据做日内策略
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

            BigDecimal consecutiveRisePoint = BigDecimal.ZERO;
            BigDecimal consecutiveFallPoint = BigDecimal.ZERO;

            // 上下文：上一根xmin kilne数值
            FutureKlineItem last1MinKline = null;
            FutureKlineItem last3MinKline = null;
            FutureKlineItem last5MinKline = null;

            // 记录日内交易记录
            List<TradeRecord> tradeRecords = new ArrayList<>();

            // 获取日内1-3-5分钟级别k线数据
            List<FutureKlineItem> kLineItems1Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min1, toUnixTime(beginTime), toUnixTime(endTime), 800);
            List<FutureKlineItem> kLineItems3Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min3, toUnixTime(beginTime), toUnixTime(endTime), 800);
            // List<FutureKlineItem> kLineItems5Min = KlineUtils.getSortedFutureKlineItems(symbols, FutureKType.min5, toUnixTime(beginTime), toUnixTime(endTime), 800);

            if (kLineItems3Min.size() == 0) {
                System.out.println("返回kline数据为空");
                continue;
            } else {
                System.out.println("返回kline数据:" + kLineItems3Min.size());
            }

            FutureKlineItem klineItem = kLineItems3Min.get(0);
            if (klineItem.getClose().compareTo(klineItem.getOpen()) > 0) {
                consecutiveRise = 1;
                consecutiveRisePoint = klineItem.getClose().subtract(klineItem.getOpen());
            } else {
                consecutiveFall = 1;
                consecutiveFallPoint = klineItem.getOpen().subtract(klineItem.getClose());
            }

            for (int i = 1; i < kLineItems3Min.size(); i++) {
                // TODO: 获取3根对应的1minKline ... 细节化

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
                // 动态计算移动止损/止盈
                if (longPosition > 0) {
                    // 触发了止损
                    if (klineItem.getLow().compareTo(stopLosePrice) < 0) {
//                    if (klineItem.getClose().compareTo(stopLosePrice) < 0) {
                        System.out.println(tradeTime + " 触发止损价格A：" + stopLosePrice);
                        // TODO: 这里有个问题点，需要更精细化的数据去判断是否触发止损价格，然后及时止损
                        transactionPrice = stopLosePrice;
                        // 平仓多头
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        longPosition = 0;
                        shortPosition = 0;

                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        pnlWithoutFee = pnlWithoutFee.add(transactionPrice.subtract(lastTransactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(SHARE_PER_TRADE_VOL));
                    } else {
                        // 动态更新移动止损:  多仓最新止损 max(closePrice - STOP_LOSE_POINT,stopLosePrice)
                        if (stopLosePrice.compareTo(closePrice.subtract(STOP_LOSE_POINT_3MIN)) < 0) {
                            stopLosePrice = closePrice.subtract(STOP_LOSE_POINT_3MIN);
                        }
                    }
                } else if (shortPosition > 0) {
                    // 触发了止损
                    if (klineItem.getHigh().compareTo(stopLosePrice) > 0) {
//                    if (klineItem.getClose().compareTo(stopLosePrice) > 0) {
                        System.out.println(tradeTime + " 触发止损价格B：" + stopLosePrice);
                        transactionPrice = stopLosePrice;

                        // 先平仓空头
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        longPosition = 0;
                        shortPosition = 0;

                        tradeFee = tradeFee.add(TRANSACTION_FEE.multiply(SHARE_PER_TRADE_VOL));
                        pnlWithoutFee = pnlWithoutFee.add(lastTransactionPrice.subtract(transactionPrice).multiply(PROFIT_LOSS_FACTOR).multiply(SHARE_PER_TRADE_VOL));
                    } else {
                        // 动态更新移动止损
                        if (stopLosePrice.compareTo(closePrice.add(STOP_LOSE_POINT_3MIN)) > 0) {
                            stopLosePrice = closePrice.add(STOP_LOSE_POINT_3MIN);
                        }
                    }
                }

                // 多头 & 空头信号
                if (longSignal(klineItem, prevKlineItem, consecutiveFall, consecutiveFallPoint)) {
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
                        stopLosePrice = closePrice.subtract(STOP_LOSE_POINT_3MIN);
                    } else if (longPosition == 0 && shortPosition == SHARE_PER_TRADE) { // 当前持有一单位空头持仓
                        // 结合止损价设定实际交易价格
                        transactionPrice = closePrice;
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
                        stopLosePrice = closePrice.subtract(STOP_LOSE_POINT_3MIN);
                    }
                } else if (shortSignal(klineItem, prevKlineItem, consecutiveRise, consecutiveRisePoint)) {
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
                        stopLosePrice = closePrice.add(STOP_LOSE_POINT_3MIN);
                    } else if (longPosition == SHARE_PER_TRADE && shortPosition == 0) {
                        // 结合止损价设定实际交易价格
                        transactionPrice = closePrice;

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
                        stopLosePrice = closePrice.add(STOP_LOSE_POINT_3MIN);
                    }
                }

                if (klineItem.getClose().compareTo(klineItem.getOpen()) >= 0) {
                    // 当前 k 线上涨
                    consecutiveRise++;
                    consecutiveFall = 0;
                    consecutiveRisePoint = consecutiveRisePoint.add(klineItem.getClose().subtract(klineItem.getOpen()));
                    consecutiveFallPoint = new BigDecimal(0);
                } else if (klineItem.getClose().compareTo(klineItem.getOpen()) < 0) {
                    // 当前 k 线下跌
                    consecutiveFall++;
                    consecutiveRise = 0;
                    consecutiveFallPoint = consecutiveFallPoint.add(klineItem.getOpen().subtract(klineItem.getClose()));
                    consecutiveRisePoint = new BigDecimal(0);
                } /*else {
                    // 当前 k 线平盘
                    consecutiveRise = 0;
                    consecutiveFall = 0;
                }*/

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

            Thread.sleep(SLEEP_MILL_SEC);
        }
    }


    private static boolean klineRise(FutureKlineItem kLineItem) {
        if (kLineItem != null) {
            return kLineItem.getClose().compareTo(kLineItem.getOpen()) > 0;
        }
        return false;
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

        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20240601", "20240610", "09:15", "11:15");
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


    /**
     * 1.通过老虎证券的 Java open API 获取HSImain股指期货每分钟k线数据，获取时间段通过参数指定如
     * String beginTime = "2024-05-30 22:48:00";
     * String endTime = "2024-05-30 23:00:00";
     * <p>
     * 2.如果当前x分钟收涨
     * a.当前没有持仓则以收盘价买入1单位
     * b.如果当前持有1单位多头，则保持持仓不动
     * c.如果当前持有1单位空头，则以收盘价先买入1单位平仓，再以收盘价买入1单位多头持仓
     * <p>
     * 3.如果当前x分钟收跌
     * a.当前没有持仓则以收盘价卖出1单位做空
     * b.如果当前持有1单位空头，则保持持仓不动
     * c.如果当前持有1单位多头，则以收盘价先卖出1单位平仓，再以收盘价卖出1单位空头持仓
     * <p>
     * 4.每单位买或者卖手续费是50港币，初始账户资金为200000港币，做多或者做空会分别占据相应的购买力50000
     * <p>
     * 5.最终打印每笔交易记录，并计算盈亏金额，盈亏比率并打印结果
     */
    public static void testMinDailyStrategy() {

        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        symbols.add(SYMBOL);
        FutureKType kType = FutureKType.min5;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);
        int counter = 0;

        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20240501", "20240601", "09:30", "11:00");
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间:" + endTime);

            longPosition = 0;
            shortPosition = 0;

            // 实现盈亏不包含交易费
            BigDecimal pnlWithoutFee = BigDecimal.ZERO;
            // 实现盈亏
            BigDecimal pnl = BigDecimal.ZERO;
            // 交易费用
            BigDecimal tradeFee = BigDecimal.ZERO;
            BigDecimal lastTransactionPrice = BigDecimal.ZERO;

            // 记录交易记录
            List<TradeRecord> tradeRecords = new ArrayList<>();
            List<FutureKlineBatchItem> kLineItems = kLineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime(beginTime), toUnixTime(endTime), 800);
            // System.out.println(Arrays.toString(kLineItems.toArray()));
            if (kLineItems.size() == 0) {
                System.out.println("返回数据为空");
                continue;
            }

            List<FutureKlineItem> klinePoints = kLineItems.get(0).getItems();
            List<FutureKlineItem> sortedKlineList = klinePoints.stream()
                    .sorted(Comparator.comparingLong(FutureKlineItem::getTime))
                    .collect(Collectors.toList());
            // System.out.println(Arrays.toString(sortedKlineList.toArray()));

            for (FutureKlineItem kLineItem : sortedKlineList) {
                LocalDateTime tradeDateTime = LocalDateTime.ofEpochSecond(kLineItem.getLastTime() / 1000, 0, offset);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String tradeTime = tradeDateTime.format(formatter);

                BigDecimal openPrice = kLineItem.getOpen();
                BigDecimal closePrice = kLineItem.getClose();
                BigDecimal transactionPrice = closePrice;
                BigDecimal changePrice = closePrice.subtract(openPrice);

                if (changePrice.compareTo(new BigDecimal(30)) >= 0) { // 当前分钟收涨
                    if (longPosition == 0 && shortPosition == 0) { // 当前没有持仓
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        longPosition = 1;
                        shortPosition = 0;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                    } else if (longPosition == 0 && shortPosition == 1) { // 当前持有一单位空头持仓
                        // 先平仓空头1单位
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        shortPosition = 0;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                        pnlWithoutFee = pnlWithoutFee.add(lastTransactionPrice.subtract(transactionPrice).multiply(PROFIT_LOSS_FACTOR));
                        // System.out.println("lastTransactionPrice:" + lastTransactionPrice + " transactionPrice:" +
                        // transactionPrice);

                        // 做多1单位
                        tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        longPosition = 1;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                    }
                } else if (changePrice.compareTo(new BigDecimal(-30)) <= 0) { // 当前分钟收跌
                    if (shortPosition == 0 && longPosition == 0) { // 当前没有持仓
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        shortPosition = 1;
                        longPosition = 0;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                    } else if (shortPosition == 0 && longPosition == 1) { // 当前持有一单位多头持仓
                        // 先平仓空头1单位
                        TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        tradeRecords.add(tradeRecord);
                        longPosition = 0;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                        pnlWithoutFee = pnlWithoutFee.add(transactionPrice.subtract(lastTransactionPrice).multiply(PROFIT_LOSS_FACTOR));
                        // System.out.println("lastTransactionPrice:" + lastTransactionPrice + " transactionPrice:" +
                        // transactionPrice);

                        // 做空1单位
                        tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, transactionPrice, SHARE_PER_TRADE, TRANSACTION_FEE);
                        lastTransactionPrice = transactionPrice;
                        tradeRecords.add(tradeRecord);
                        shortPosition = 1;
                        tradeFee = tradeFee.add(TRANSACTION_FEE);
                    }
                }
            }

            if (tradeRecords.isEmpty()) {
                // System.out.println("无交易记录");
                return;
            }

            // System.out.println("交易记录:");
            for (TradeRecord tradeRecord : tradeRecords) {
                // System.out.println(tradeRecord);
            }

            pnl = pnlWithoutFee.subtract(tradeFee);
            accumulateFee = accumulateFee.add(tradeFee);
            accumulatePnl = accumulatePnl.add(pnl);

            System.out.println("当前持仓 longPosition: " + longPosition + ", shortPosition:" + shortPosition);
            System.out.println("当日交易记录数: " + tradeRecords.size() + ", 当日交易成本:" + tradeFee);
            System.out.println("当日收益不包含手续费 pnlWithoutFee:" + pnlWithoutFee);
            System.out.println("当日收益净值 pnl:" + pnl);
            System.out.println("累计交易记录数 tradeCount:" + (tradeCount = tradeCount + tradeRecords.size()));
            System.out.println("累计交易成本 accumulateFee:" + accumulateFee);
            System.out.println("累计收益净值 accumulatePnl:" + accumulatePnl);
        }
    }

}
