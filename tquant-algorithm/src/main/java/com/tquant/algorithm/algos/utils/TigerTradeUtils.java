package com.tquant.algorithm.algos.utils;

import com.alibaba.fastjson.JSONObject;
import com.tigerbrokers.stock.openapi.client.https.client.TigerHttpClient;
import com.tigerbrokers.stock.openapi.client.https.domain.trade.item.PrimeAssetItem;
import com.tigerbrokers.stock.openapi.client.https.request.trade.EstimateTradableQuantityRequest;
import com.tigerbrokers.stock.openapi.client.https.request.trade.PrimeAssetRequest;
import com.tigerbrokers.stock.openapi.client.https.response.trade.EstimateTradableQuantityResponse;
import com.tigerbrokers.stock.openapi.client.https.response.trade.PrimeAssetResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.*;
import com.tquant.gateway.tiger.TigerClient;

/**
 * Tiger交易账户信息获取工具类
 * <p>
 * 交易类接口列表
 * https://quant.itigerup.com/openapi/zh/java/operation/trade/tradeList.html
 *
 * @author yunmu.zhy
 * @version 1.0
 * @since 2023年05月30日
 */
public class TigerTradeUtils {

    /**
     * 综合/模拟账号获取资产
     * <p>
     * 证券账户：
     * 1.现金额 cashBalance
     * 2.证券总价值 grossPositionValue
     * 3.未实现盈亏 unrealizedPL
     * 4.最大购买力 buyingPower
     * 5.隔夜剩余流动性 excessLiquidation
     * 6.杠杆 leverage
     * <p>
     * 期货账户
     * 1.账户总金额 cashBalance
     * 2.可用资金 cashAvailableForTrade
     * 2.未实现盈亏 unrealizedPL
     * 3.隔夜剩余流动性 excessLiquidation
     *
     * @param category Category.S - 证券类， Category.C - 期货类
     * @param currency Currency.USD - 美元, Currency.HKD 港币
     */
    public static PrimeAssetResponse getPrimeAsset(Category category, Currency currency) {
        // 模拟账号 20221226135452241 真实账号 7244563
        PrimeAssetRequest assetRequest = PrimeAssetRequest.buildPrimeAssetRequest("20221226135452241");

        TigerHttpClient client = TigerClient.getInstance();
        PrimeAssetResponse primeAssetResponse = client.execute(assetRequest);

        // 查询证券相关资产信息
        PrimeAssetItem.Segment segment = primeAssetResponse.getSegment(Category.S);
        System.out.println("Security account: " + JSONObject.toJSONString(segment));

        // 查询期货账号相关信息
        segment = primeAssetResponse.getSegment(Category.C);
        System.out.println("Commodity account: " + JSONObject.toJSONString(segment));

        // 查询账号中美元相关资产信息
        if (segment != null) {
            PrimeAssetItem.CurrencyAssets assetByCurrency = segment.getAssetByCurrency(Currency.USD);
            System.out.println("assetByCurrency: " + JSONObject.toJSONString(assetByCurrency));
        }

        return primeAssetResponse;
    }

    /**
     * 获取当前综合账号 证券可用现金
     *
     * @param currency
     * @return
     */
    public static Double getSecurityAccountCash(Currency currency) {
        // 模拟账号 20221226135452241 真实账号 7244563
        PrimeAssetRequest assetRequest = PrimeAssetRequest.buildPrimeAssetRequest("20221226135452241");

        TigerHttpClient client = TigerClient.getInstance();
        PrimeAssetResponse primeAssetResponse = client.execute(assetRequest);

        // 查询证券相关资产信息
        PrimeAssetItem.Segment segment = primeAssetResponse.getSegment(Category.S);

        // 查询账号中相关资产信息
        PrimeAssetItem.CurrencyAssets assetByCurrency = new PrimeAssetItem.CurrencyAssets();
        if (segment != null) {
            assetByCurrency = segment.getAssetByCurrency(currency);
            System.out.println("assetByCurrency: " + JSONObject.toJSONString(assetByCurrency));
        }

        return assetByCurrency.getCashAvailableForTrade();
    }

    /**
     * 获取当前综合账号 期货可用现金
     *
     * @param currency
     * @return
     */
    public static Double getCommodityAccountCash(Currency currency) {
        // 模拟账号 20221226135452241 真实账号 7244563
        PrimeAssetRequest assetRequest = PrimeAssetRequest.buildPrimeAssetRequest("20221226135452241");

        TigerHttpClient client = TigerClient.getInstance();
        PrimeAssetResponse primeAssetResponse = client.execute(assetRequest);

        // 查询证券相关资产信息
        PrimeAssetItem.Segment segment = primeAssetResponse.getSegment(Category.C);

        // 查询账号中相关资产信息
        PrimeAssetItem.CurrencyAssets assetByCurrency = new PrimeAssetItem.CurrencyAssets();
        if (segment != null) {
            assetByCurrency = segment.getAssetByCurrency(currency);
            System.out.println("assetByCurrency: " + JSONObject.toJSONString(assetByCurrency));
        }

        return assetByCurrency.getCashAvailableForTrade();
    }

    /**
     * 获取最大可交易数量
     * <p>
     * ☆ tradablePositionQuantity 持仓可交易数量
     * positionQuantity Double 持仓数量
     * <p>
     * ☆ tradableQuantity Double 现金可买卖数量(如action为BUY，返回为可买数量，反之为可卖数量)
     * financingQuantity Double 融资融券可买卖数量(现金账号没有)
     */
    public static EstimateTradableQuantityResponse getEstimateTradableQuantity(SecType secType, String symbol,
                                                                               ActionType action, OrderType orderType,
                                                                               Double limitPrice, Double stopPrice) {
        EstimateTradableQuantityRequest request = EstimateTradableQuantityRequest
                .buildRequest("20221226135452241", secType, symbol, action, orderType, limitPrice, stopPrice);

        TigerHttpClient client = TigerClient.getInstance();
        EstimateTradableQuantityResponse response = client.execute(request);
        if (response.isSuccess()) {
            System.out.println(JSONObject.toJSONString(response));
            // System.out.println(JSONObject.toJSONString(response.getTradableQuantityItem().getFinancingQuantity()));
        } else {
            System.out.println("fail." + JSONObject.toJSONString(response));
        }

        return response;
    }

    public static void main(String[] args) {
//         getPrimeAsset(Category.S, Currency.USD);
        System.out.printf("Security account cash: %f\n", getSecurityAccountCash(Currency.USD));
//         System.out.printf("Commodity account cash: %f\n", getCommodityAccountCash(Currency.USD));
//         System.out.println(getEstimateTradableQuantity(SecType.STK, "BABA", ActionType.BUY, OrderType.MKT, null, null));
    }

}
