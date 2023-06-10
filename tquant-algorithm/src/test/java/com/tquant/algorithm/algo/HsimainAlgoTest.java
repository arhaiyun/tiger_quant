package com.tquant.algorithm.algo;

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
public class HsimainAlgoTest {

    private static final String SYMBOL = "HSImain";
    private static final int SHARE_PER_TRADE = 1;
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.002);

    // 每点交易盈亏 50HKD
    private static final BigDecimal PROFIT_LOSS_FACTOR = new BigDecimal(50.0);
    // 1单位买卖手续费 25HKD
    private static final BigDecimal TRANSACTION_FEE = new BigDecimal(25.0);
    private static final int PRICE_CHANGE_FACTOR_3MIN = 20;
    private static BigDecimal lastTransactionPrice = BigDecimal.ZERO;
    private static BigDecimal transactionPrice = BigDecimal.ZERO;

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
     * 1.通过老虎证券的 Java open API 获取HSImain股指期货每分钟k线数据，获取时间段通过参数指定如
     *      String beginTime = "2023-05-30 22:48:00";
     *      String endTime = "2023-05-30 23:00:00";
     *
     * 2.如果当前x分钟收涨
     *   a.当前没有持仓则以收盘价买入1单位
     *   b.如果当前持有1单位多头，则保持持仓不动
     *   c.如果当前持有1单位空头，则以收盘价先买入1单位平仓，再以收盘价买入1单位多头持仓
     *
     * 3.如果当前x分钟收跌
     *   a.当前没有持仓则以收盘价卖出1单位做空
     *   b.如果当前持有1单位空头，则保持持仓不动
     *   c.如果当前持有1单位多头，则以收盘价先卖出1单位平仓，再以收盘价卖出1单位空头持仓
     *
     * 4.每单位买或者卖手续费是50港币，初始账户资金为200000港币，做多或者做空会分别占据相应的购买力50000
     *
     * 5.最终打印每笔交易记录，并计算盈亏金额，盈亏比率并打印结果
     *
     */
    public static void testMinDailyStrategy() {

        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        symbols.add("HSImain");
        FutureKType kType = FutureKType.min5;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);
        int counter = 0;

        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20230501", "20230601", "09:30", "11:00");
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
                transactionPrice = closePrice;
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

    /**
     * 多级别X-Min Kline 综合策略结果
     *
     * 基本设计思想：
     * 1. 多种时间维度 k线数据综合决策
     * 2. 截断亏损
     * 3. 让利润奔跑
     * 4. 市场具有共振关联性标的
     * 5. 优秀的仓位控制策略
     * 6. 基于历史统计数据做好充分的回测
     *
     */
    public static void mixedMinDailyStrategy() {

        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        symbols.add("HSImain");
        FutureKType kType = FutureKType.min3;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);
        int counter = 0;

        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList("20230501", "20230605", "09:30", "11:00");
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
                transactionPrice = closePrice;
                BigDecimal changePrice = closePrice.subtract(openPrice);
                BigDecimal highChangePrice = kLineItem.getHigh().subtract(closePrice);
                BigDecimal lowChangePrice = closePrice.subtract(kLineItem.getLow());

                if (closePrice.compareTo(openPrice) > 0 && lowChangePrice.compareTo(new BigDecimal(PRICE_CHANGE_FACTOR_3MIN)) >= 0) { // 当前分钟收涨
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
                } else if (closePrice.compareTo(openPrice) < 0 && highChangePrice.compareTo(new BigDecimal(PRICE_CHANGE_FACTOR_3MIN)) >= 0) { // 当前分钟收跌
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
                System.out.println(tradeRecord);
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

    public static void main(String[] args) {
        // testMinDailyStrategy();
        // System.out.println(TradeTimeUtils.getTradeTimeList("20230101", "20230601", "09:30", "11:30"));
        mixedMinDailyStrategy();
    }

}
