package com.tquant.algorithm.algos.entity;

import lombok.Data;

/**
 * 交易时间范围
 *
 * @author yunmu.zhy
 * @version 1.0
 * @since 2023年05月30日
 */
@Data
public class TradeTimeRange {

    private final String beginTime;
    private final String endTime;

    public TradeTimeRange(String beginTime, String endTime) {
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

}