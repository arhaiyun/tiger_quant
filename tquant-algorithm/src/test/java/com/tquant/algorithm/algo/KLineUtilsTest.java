package com.tquant.algorithm.algo;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlineItem;
import com.tigerbrokers.stock.openapi.client.struct.enums.KType;
import com.tquant.algorithm.algos.utils.KlineUtils;
import com.tquant.core.TigerQuantException;
import com.tquant.gateway.tiger.TigerConfig;

import java.io.FileInputStream;
import java.io.IOException;
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
public class KLineUtilsTest {

    private static final String algoConfigPath
            = "D:\\arhaiyun\\projects\\tiger_quant\\algo_setting.json";
    private static final String gatewayConfigPath
            = "D:\\arhaiyun\\projects\\tiger_quant\\gateway_setting.json";

    private static void initProperties(String algoConfigPath, String gatewayConfigPath) {
        System.setProperty(ALGO_CONFIG_PATH_PROP, algoConfigPath);
        System.setProperty(TIGER_CONFIG_PATH_PROP, gatewayConfigPath);
    }

    public static void main(String[] args) {
        initProperties(algoConfigPath, gatewayConfigPath);
        try {
            FileInputStream inputStream = new FileInputStream(gatewayConfigPath);
            TigerConfig tigerConfig = JSON.parseObject(inputStream, TigerConfig.class);
//            System.out.println(tigerConfig.toString());
        } catch (IOException e) {
            throw new TigerQuantException("parse config exception:" + e.getMessage());
        }

        KlineUtils kLineUtils = new KlineUtils();
        List<String> symbols = Lists.newArrayList();
        symbols.add("TSLA");
        // symbols.add("BABA");
        KType kType = KType.day;
        String beginTime = "2023-05-01";
        String endTime = "2023-06-01";

        List<KlineItem> kLineItems = kLineUtils.getStockKlineItems(symbols, kType, beginTime, endTime, 120);
        System.out.println(Arrays.toString(kLineItems.toArray()));

        kType = KType.day;
        kLineItems = kLineUtils.getStockKlineItems(symbols, kType, beginTime, endTime, 120);
        System.out.println(Arrays.toString(kLineItems.toArray()));

        kLineItems = kLineUtils.getAllStockKlineItems(symbols, kType, beginTime, endTime, 120);
        System.out.println(Arrays.toString(kLineItems.toArray()));
    }
}
