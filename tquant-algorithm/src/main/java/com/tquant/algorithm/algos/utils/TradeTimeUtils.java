package com.tquant.algorithm.algos.utils;

import com.tquant.algorithm.algos.entity.TradeTimeRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * unittime 转换工具类
 *
 * @author yunmu.zhy
 * @version 1.0
 * @since 2023年05月30日
 */
public class TradeTimeUtils {

    /**
     * 根据相应格式的时间数据生成unix毫秒
     *
     * @param timeString
     * @return
     */
    public static Long toUnixTime(String timeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(timeString, formatter);
        return dateTime.toEpochSecond(java.time.ZoneOffset.ofHours(8)) * 1000;
    }

    /**
     * 生成一段时间范围内的秒数据, 用于交易回测的模拟
     * eg:
     * String beginTime = "2023-05-30 00:00:00";
     * String endTime = "2023-05-30 00:00:04";
     * <p>
     * 2023-05-30 00:00:00
     * 2023-05-30 00:00:01
     * 2023-05-30 00:00:02
     * 2023-05-30 00:00:03
     * 2023-05-30 00:00:04
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<String> generateSecondTime(String beginTime, String endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDateTime = LocalDateTime.parse(beginTime, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endTime, formatter);
        List<String> timeList = new ArrayList<>();
        while (startDateTime.isBefore(endDateTime)) {
            timeList.add(formatter.format(startDateTime));
            startDateTime = startDateTime.plusSeconds(1);
        }
        timeList.add(formatter.format(endDateTime));
        return timeList;
    }

    /**
     * 获取交易数据回测区间
     *
     * @param beginDate 20230101
     * @param endDate   20230601
     * @param beginTime 9:30
     * @param endTime   11:00
     * @return
     */
    public static List<TradeTimeRange> getTradeTimeList(String beginDate, String endDate,
                                                        String beginTime, String endTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDate = LocalDate.parse(beginDate, dateFormatter);
        LocalDate endLocalDate = LocalDate.parse(endDate, dateFormatter);

        String[] beginTimeParts = beginTime.split(":");
        int beginHour = Integer.parseInt(beginTimeParts[0]);
        int beginMinute = Integer.parseInt(beginTimeParts[1]);

        String[] endTimeParts = endTime.split(":");
        int endHour = Integer.parseInt(endTimeParts[0]);
        int endMinute = Integer.parseInt(endTimeParts[1]);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<TradeTimeRange> tradeTimeRanges = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endLocalDate); date = date.plusDays(1)) {
            LocalDateTime beginDateTime = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), beginHour, beginMinute);
            LocalDateTime endDateTime = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), endHour, endMinute);
            tradeTimeRanges.add(new TradeTimeRange(beginDateTime.format(formatter), endDateTime.format(formatter)));
        }
        return tradeTimeRanges;
    }


    public static void main(String[] args) {
        System.out.println(getTradeTimeList("20230101", "20230601", "09:30", "11:30"));
    }
}
