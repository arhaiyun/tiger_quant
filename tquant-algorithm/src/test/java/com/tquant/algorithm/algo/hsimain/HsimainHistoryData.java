package com.tquant.algorithm.algo.hsimain;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tquant.algorithm.algo.KlineChartViewer;
import com.tquant.algorithm.algos.entity.TradeTimeRange;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.algorithm.algos.utils.TradeTimeUtils;
import com.tquant.algorithm.algos.utils.WeChatMessageUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.tquant.algorithm.algos.utils.TradeTimeUtils.toUnixTime;
import static com.tquant.algorithm.constants.HsimainAlgoConstants.*;

/**
 * Description:
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class HsimainHistoryData {

    public static void main(String[] args) throws InterruptedException {
        drawKineCanvas();
    }

    /**
     * 1.通过老虎证券的 Java open API 获取HSImain股指期货每分钟k线数据，获取时间段通过参数指定如 2025-03-01 09:30:00 - 2025-03-14 12:00:00
     */
    public static void drawKineCanvas() throws InterruptedException {
        List<String> symbols = Lists.newArrayList();
        symbols.add(HSIMAIN);
        FutureKType kType = FutureKType.day;
        // 或者使用 ZoneOffset.UTC
        ZoneOffset offset = ZoneOffset.ofHours(8);

        int counter = 0;
        List<TradeTimeRange> tradeTimeList = TradeTimeUtils.getTradeTimeList(YEAR + MONTH + "01", YEAR + MONTH + "10", "09:00", "16:01");
        for (TradeTimeRange tradeTimeRange : tradeTimeList) {
            counter++;
            String beginTime = tradeTimeRange.getBeginTime();
            String endTime = tradeTimeRange.getEndTime();
            System.out.println("\n\nIndex: " + counter + " 交易开始时间：beginTime:" + beginTime + " 结束时间：" + endTime);

            List<FutureKlineBatchItem> kLineItems = KlineUtils.getAllFutureKlineItems(symbols, kType, toUnixTime(beginTime), toUnixTime(endTime), 800);
            System.out.println("kLineItems.size() = " + kLineItems.size());
        }
    }


}
