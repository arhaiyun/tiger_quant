package com.tquant.algorithm.algo;

import com.tquant.algorithm.algos.utils.TradeTimeUtils;

import java.util.List;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2023/05/20
 */
public class TimeUtilsTest {

    public static void main(String[] args) {
        TradeTimeUtils timeUtils = new TradeTimeUtils();
        String beginTime = "2023-05-30 09:30:00";
        String endTime = "2023-05-30 10:45:00";
        List<String> seconds = timeUtils.generateSecondTime(beginTime, endTime);
        for (String second : seconds) {
            System.out.println(second);
        }
    }
}
