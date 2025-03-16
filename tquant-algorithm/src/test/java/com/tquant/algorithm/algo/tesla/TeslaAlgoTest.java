package com.tquant.algorithm.algo.tesla;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlineItem;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlinePoint;
import com.tigerbrokers.stock.openapi.client.struct.enums.KType;
import com.tquant.algorithm.algos.entity.TradeRecord;
import com.tquant.algorithm.algos.utils.KlineUtils;
import org.ta4j.core.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tquant.core.util.QuantConstants.ALGO_CONFIG_PATH_PROP;
import static com.tquant.core.util.QuantConstants.TIGER_CONFIG_PATH_PROP;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2023/05/20
 */
public class TeslaAlgoTest {

    private static final String algoConfigPath
            = "D:\\arhaiyun\\projects\\tiger_quant\\algo_setting.json";
    private static final String gatewayConfigPath
            = "D:\\arhaiyun\\projects\\tiger_quant\\gateway_setting.json";
    private static final String SYMBOL = "TSLA";
    private static final int SHARE_PER_TRADE = 400;
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.002);

    private static void initProperties(String algoConfigPath, String gatewayConfigPath) {
        System.setProperty(ALGO_CONFIG_PATH_PROP, algoConfigPath);
        System.setProperty(TIGER_CONFIG_PATH_PROP, gatewayConfigPath);
    }

    public static void main(String[] args) {
        /*initProperties(algoConfigPath, gatewayConfigPath);

        try {
            FileInputStream inputStream = new FileInputStream(gatewayConfigPath);
            TigerConfig tigerConfig = JSON.parseObject(inputStream, TigerConfig.class);
            // System.out.println(tigerConfig.toString());
        } catch (IOException e) {
            throw new TigerQuantException("parse config exception:" + e.getMessage());
        }*/

        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        // TSLA NVDA BABA BILI PDD
        symbols.add("TSLA");
        KType kType = KType.day;
        String beginTime = "2023-04-01";
        String endTime = "2023-06-10";


        List<KlineItem> kLineItems = kLineUtils.getAllStockKlineItems(symbols, kType, beginTime, endTime, 800);
        // System.out.println(Arrays.toString(kLineItems.toArray()));
        List<KlinePoint> klinePoints = kLineItems.get(0).getItems();

        BigDecimal currentPosition = BigDecimal.ZERO;
        BigDecimal initAsset = new BigDecimal(1000000);
        BigDecimal currentAsset = new BigDecimal(1000000);
        List<TradeRecord> tradeRecords = new ArrayList<>();

        KlinePoint lastklinePoint = null;
        for (KlinePoint klinePoint : klinePoints) {
            if (lastklinePoint == null) {
                lastklinePoint = klinePoint;
                lastklinePoint.setClose(klinePoint.getOpen());
            }
            ZoneOffset offset = ZoneOffset.ofHours(8); // 或者使用 ZoneOffset.UTC
            LocalDateTime tradeDateTime = LocalDateTime.ofEpochSecond(klinePoint.getTime() / 1000, 0, offset);
            LocalDate date = tradeDateTime.toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String tradeTime = tradeDateTime.format(formatter);

            BigDecimal closePrice = BigDecimal.valueOf(klinePoint.getClose());
            BigDecimal lastClosePrice = BigDecimal.valueOf(lastklinePoint.getClose());

//            if (lastClosePrice.compareTo(closePrice) > 0 && currentPosition.compareTo(BigDecimal.ZERO) == 0) {
            if (closePrice.compareTo(BigDecimal.valueOf(klinePoint.getOpen())) > 0 && currentPosition.compareTo(BigDecimal.ZERO) == 0) {
                // 当日收涨且为空仓，买入
                BigDecimal tradeAmount = closePrice.multiply(BigDecimal.valueOf(SHARE_PER_TRADE));
                // BigDecimal commission = tradeAmount.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal commission = new BigDecimal(10);
                // 交易成本叫手续费
                BigDecimal totalCost = tradeAmount.add(commission);
                if (totalCost.compareTo(currentAsset) > 0) {
                    // 余额不足无法交易
                    System.out.println("余额不足无法交易");
                    break;
                }
                currentPosition = currentPosition.add(BigDecimal.valueOf(SHARE_PER_TRADE));
                currentAsset = currentAsset.add(tradeAmount.negate()).add(commission.negate());
                TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.BUY, closePrice, SHARE_PER_TRADE, commission);
                tradeRecords.add(tradeRecord);
//                } else if (lastClosePrice.compareTo(closePrice) < 0 && currentPosition.compareTo(BigDecimal.ZERO) != 0) {
            } else if (closePrice.compareTo(BigDecimal.valueOf(klinePoint.getOpen())) < 0 && currentPosition.compareTo(BigDecimal.ZERO) != 0) {
                // 当日收跌且有持仓，卖出全部
                BigDecimal tradeAmount = closePrice.multiply(BigDecimal.valueOf(SHARE_PER_TRADE));
                // BigDecimal commission = tradeAmount.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal commission = new BigDecimal(10);
                // 卖出收益 - 手续费
                BigDecimal totalProceeds = tradeAmount.add(commission.negate());
                currentAsset = currentAsset.add(totalProceeds);
                currentPosition = currentPosition.subtract(BigDecimal.valueOf(SHARE_PER_TRADE));
                TradeRecord tradeRecord = new TradeRecord(tradeTime, Trade.TradeType.SELL, closePrice, SHARE_PER_TRADE, commission);
                tradeRecords.add(tradeRecord);
            }
            lastklinePoint = klinePoint;
        }

        // 输出买卖记录
        BigDecimal totalCommission = BigDecimal.ZERO;
        System.out.println("交易记录:");
        for (TradeRecord tradeRecord : tradeRecords) {
            System.out.println(tradeRecord);
            totalCommission = totalCommission.add(tradeRecord.getCommission());
        }

        // 计算当月收益率
        if (tradeRecords.isEmpty()) {
            System.out.println("无交易记录");
        } else {
            TradeRecord lastRecord = tradeRecords.get(tradeRecords.size() - 1);
            if (lastRecord.getTradeType().equals(Trade.TradeType.BUY)) {
                currentAsset = currentAsset.add(lastRecord.getTradePrice().multiply(BigDecimal.valueOf(lastRecord.getShare())));
            }
            System.out.println("累计交易记录数: " + tradeRecords.size() + ", 累计交易成本:" + totalCommission);
            System.out.println("initAsset: " + initAsset + ", currentAsset:" + currentAsset);
            BigDecimal rateOfReturn =
                    currentAsset.subtract(initAsset).divide(initAsset, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            System.out.println("当前收益净值 pnl:" + currentAsset.subtract(initAsset));
            System.out.println("当前收益率为 ratio:" + rateOfReturn + "%");
        }
    }

}

