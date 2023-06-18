package com.tquant.algorithm.algos.utils;

import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineBatchItem;
import com.tigerbrokers.stock.openapi.client.https.domain.future.item.FutureKlineItem;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlineItem;
import com.tigerbrokers.stock.openapi.client.https.request.future.FutureKlineRequest;
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteKlineRequest;
import com.tigerbrokers.stock.openapi.client.https.response.future.FutureKlineResponse;
import com.tigerbrokers.stock.openapi.client.https.response.quote.QuoteKlineResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.FutureKType;
import com.tigerbrokers.stock.openapi.client.struct.enums.KType;
import com.tigerbrokers.stock.openapi.client.struct.enums.RightOption;
import com.tigerbrokers.stock.openapi.client.struct.enums.TimeZoneId;
import com.tquant.gateway.tiger.TigerClient;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * K线数据获取工具类
 * https://quant.itigerup.com/openapi/zh/java/operation/quotation/stock
 * .html#%E8%8E%B7%E5%8F%96k%E7%BA%BF%E6%95%B0%E6%8D%AE
 *
 * @author yunmu.zhy
 * @version 1.0
 * @since 2023年05月28日
 */
public class KlineUtils {

    /**
     * 获取股票K线数据
     * <p>
     * newRequest(symbols, kType, "2023-05-20", "2023-05-28")
     *
     * @param symbols
     * @param kType
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<KlineItem> getStockKlineItems(List<String> symbols,
                                                     KType kType,
                                                     String beginTime, String endTime,
                                                     Integer limit) {
        List<KlineItem> klineItems = Lists.newArrayList();

        TigerHttpClient client = TigerClient.getInstance();
        QuoteKlineResponse response = client.execute(QuoteKlineRequest
                .newRequest(symbols, kType, beginTime, endTime)
                .withLimit(limit)
                .withRight(RightOption.br));

        if (response.isSuccess()) {
            klineItems = response.getKlineItems();
            return klineItems;
        } else {
            System.out.println("response error:" + response.getMessage());
        }

        return klineItems;
    }

    /**
     * 获取股票K线数据, 针对翻页场景 - 通过pageToken获取
     *
     * @param symbols
     * @param kType
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<KlineItem> getAllStockKlineItems(List<String> symbols,
                                                        KType kType,
                                                        String beginTime, String endTime,
                                                        Integer limit) {
        TigerHttpClient client = TigerClient.getInstance();
        QuoteKlineRequest request = QuoteKlineRequest.newRequest(symbols, kType, beginTime, endTime, TimeZoneId.Shanghai);
        request.withLimit(limit);
        request.withRight(RightOption.br);

        List<KlineItem> klineItems = Lists.newArrayList();

        int count = 1;
        while (true) {
            QuoteKlineResponse response = client.execute(request);
            // System.out.println("search time:" + count + ", success:" + response.isSuccess() + ", msg:" + response
            // .getMessage());
            if (!response.isSuccess()) {
                break;
            }
            if (response.getKlineItems().size() == 0) {
                break;
            }
            // 保存所有的kLineItem信息并返回
            klineItems.addAll(response.getKlineItems());

            KlineItem klineItem = response.getKlineItems().get(0);
            if (klineItem.getNextPageToken() == null) {
                break;
            }
            count++;
            // 10 times per minute
            try {
                TimeUnit.SECONDS.sleep(6);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // set pagination token then query the next page
            request.withPageToken(klineItem.getNextPageToken());
        }

        return klineItems;
    }

    /**
     * 获取期货K线数据
     * contractCodes: HSImain, NQmain, CLmain, FDAXmain
     * <p>
     * https://quant.itigerup.com/openapi/zh/python/operation/quotation/future.html#get-future-bars-%E8%8E%B7%E5%8F%96%E6%9C%9F%E8%B4%A7k%E7%BA%BF
     *
     * @param contractCodes
     * @param kType
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<FutureKlineBatchItem> getFutureKlineItems(List<String> contractCodes,
                                                                 FutureKType kType,
                                                                 Long beginTime, Long endTime,
                                                                 Integer limit) {
        List<FutureKlineBatchItem> klineItems = Lists.newArrayList();
        TigerHttpClient client = TigerClient.getInstance();

        FutureKlineResponse response = client.execute(FutureKlineRequest.newRequest(contractCodes, kType, beginTime, endTime, limit));
        if (response.isSuccess()) {
            klineItems = response.getFutureKlineItems();
            return klineItems;
        } else {
            System.out.println("response error:" + response.getMessage());
        }

        return klineItems;
    }

    /**
     * 获取期货K线数据, 针对翻页场景 - 通过pageToken获取
     * contractCodes: HSImain, NQmain, CLmain, FDAXmain
     * <p>
     * https://quant.itigerup.com/openapi/zh/python/operation/quotation/future.html#get-future-bars-%E8%8E%B7%E5%8F%96%E6%9C%9F%E8%B4%A7k%E7%BA%BF
     *
     * @param contractCodes
     * @param kType
     * @param beginTime
     * @param endTime
     * @return
     */
    public static List<FutureKlineBatchItem> getAllFutureKlineItems(List<String> contractCodes,
                                                                    FutureKType kType,
                                                                    Long beginTime, Long endTime,
                                                                    Integer limit) {
        List<FutureKlineBatchItem> klineItems = Lists.newArrayList();

        TigerHttpClient client = TigerClient.getInstance();
        // pagetoken only for single symbol and specified endTime
        FutureKlineRequest request = FutureKlineRequest.newRequest(contractCodes, kType, beginTime, endTime, limit);

        int count = 1;
        while (true) {
            FutureKlineResponse response = client.execute(request);
            // System.out.println("search time:" + count + ", success:" + response.isSuccess() + ", msg:" + response
            // .getMessage());
            if (!response.isSuccess()) {
                break;
            }
            if (response.getFutureKlineItems().size() == 0) {
                break;
            }
            // TODO: 加一个最大返回数量保护，防止传参时间
            klineItems.addAll(response.getFutureKlineItems());

            String nextPageToken = response.getFutureKlineItems().get(0).getNextPageToken();
            if (nextPageToken == null) {
                break;
            }
            count++;
            // 10 times per minute
            try {
                TimeUnit.SECONDS.sleep(6);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // set nextPageToken and search next page data
            request.withPageToken(nextPageToken);
        }

        return klineItems;
    }

    public static List<FutureKlineItem> getSortedFutureKlineItems(List<String> contractCodes,
                                                                  FutureKType kType,
                                                                  Long beginTime, Long endTime,
                                                                  Integer limit) {
        List<FutureKlineBatchItem> klineItems = getAllFutureKlineItems(contractCodes, kType, beginTime, endTime, limit);
        if (klineItems.size() == 0) {
            return Lists.newArrayList();
        }

        List<FutureKlineItem> klinePoints = klineItems.get(0).getItems();
        List<FutureKlineItem> sortedKlineList = klinePoints.stream()
                .sorted(Comparator.comparingLong(FutureKlineItem::getTime))
                .collect(Collectors.toList());

        return sortedKlineList;
    }
}
