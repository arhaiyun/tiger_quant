package com.tquant.algorithm.constants;

import java.math.BigDecimal;

/**
 * 恒生指数期货策略相关常量
 */
public class HsimainAlgoConstants {
    /** 交易品种 */
    public static final String HSIMAIN = "HSImain";

    /** 每点交易盈亏(HKD) */
    public static final BigDecimal PROFIT_LOSS_FACTOR = new BigDecimal(50.0);
    /**
     * 单位买卖手续费(HKD) */
    public static final BigDecimal TRANSACTION_FEE = new BigDecimal(25.0);

    /** 每次交易份数 */
    public static final int SHARE_PER_TRADE = 1;


    
    /** 累积计数 */
    public static final int ACCUMULATE_CNT = 2;

    public static final BigDecimal SHARE_PER_TRADE_VOL = new BigDecimal(SHARE_PER_TRADE);

    
    /** 休眠时间(毫秒) */
    public static final Long SLEEP_MILL_SEC = 1500L;
    
    /** 交易年份 */
    public static final String YEAR = "2025";
    
    /** 交易月份 */
    public static final String MONTH = "03";
    
    /** 日内交易开始时间 */
    public static final String DAY_BEGIN_TIME = "09:45";
    
    /** 日内交易结束时间 */
    public static final String DAY_END_TIME = "11:00";

    /** k线实体变化点数 */
    public static final BigDecimal PRICE_CHANGE_FACTOR = new BigDecimal(20);
    
    /** 最高/低点到k线close价格变化点数 */
    public static final BigDecimal PRICE_CHANGE_FACTOR_HL = new BigDecimal(50);
    
    /** 连续变化点数 */
    public static final BigDecimal PRICE_CHANGE_FACTOR_CONSECUTIVE = new BigDecimal(60);

    /** 1分钟K线止损点数 */
    public static final BigDecimal STOP_LOSE_POINT_1MIN = new BigDecimal(30);
    
    /** 3分钟K线止损点数 */
    public static final BigDecimal STOP_LOSE_POINT_3MIN = new BigDecimal(45);
    
    /** 5分钟K线止损点数 */
    public static final BigDecimal STOP_LOSE_POINT_5MIN = new BigDecimal(60);
}