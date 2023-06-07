package com.tquant.algorithm.algos.entity;

import lombok.Data;
import org.ta4j.core.Trade;

import java.math.BigDecimal;

/**
 * 交易计费工具类
 *
 * @author yunmu.zhy
 * @version 1.0
 * @since 2023年05月30日
 */
@Data
public class TradeRecord {

    private final String tradeTime;
    private final Trade.TradeType tradeType;
    private final BigDecimal tradePrice;
    private final int share;
    private final BigDecimal commission;

    public TradeRecord(String tradeTime, Trade.TradeType tradeType,
                       BigDecimal tradePrice,
                       int share,
                       BigDecimal commission) {
        this.tradeTime = tradeTime;
        this.tradeType = tradeType;
        this.tradePrice = tradePrice;
        this.share = share;
        this.commission = commission;
    }

    public String getTradeDate() {
        return tradeTime;
    }

    public Trade.TradeType getTradeType() {
        return tradeType;
    }

    public BigDecimal getTradePrice() {
        return tradePrice;
    }

    public int getShare() {
        return share;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    @Override
    public String toString() {
        return tradeTime + "    " + tradeType + "   " + tradePrice + "  " + share + "   " + commission;
    }

}