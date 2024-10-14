package com.tquant.gateway.tiger;

import com.alibaba.fastjson.JSON;
import com.tquant.core.TigerQuantException;
import com.tquant.core.config.ConfigLoader;

import java.io.FileInputStream;
import java.io.IOException;

import static com.tquant.core.util.QuantConstants.ALGO_CONFIG_PATH_PROP;
import static com.tquant.core.util.QuantConstants.TIGER_CONFIG_PATH_PROP;

/**
 * Description:
 *
 * @author kevin
 * @date 2022/08/05
 */
public class TigerConfigLoader {

//    private static final String algoConfigPath
//            = "D:\\arhaiyun\\projects\\tiger_quant\\algo_setting.json";
//    private static final String gatewayConfigPath
//            = "D:\\arhaiyun\\projects\\tiger_quant\\gateway_setting.json";

    private static final String algoConfigPath
            = "/Users/arhaiyun/github/tiger_quant/algo_setting.json";
    private static final String gatewayConfigPath
            = "/Users/arhaiyun/github/tiger_quant/gateway_setting.json";

    private static void initProperties(String algoConfigPath, String gatewayConfigPath) {
        System.setProperty(ALGO_CONFIG_PATH_PROP, algoConfigPath);
        System.setProperty(TIGER_CONFIG_PATH_PROP, gatewayConfigPath);
    }
    public static TigerConfig loadTigerConfig() {
        initProperties(algoConfigPath, gatewayConfigPath);

        TigerConfig config = null;

        try {
            FileInputStream inputStream = new FileInputStream(gatewayConfigPath);
            config = JSON.parseObject(inputStream, TigerConfig.class);
            System.out.println(config.toString());
        } catch (IOException e) {
            throw new TigerQuantException("parse config exception:" + e.getMessage());
        }

        if (config.getTigerId() == null) {
            throw new TigerQuantException("tigerId is null");
        }
        if (config.getAccount() == null) {
            throw new TigerQuantException("account is null");
        }
        if (config.getPrivateKey() == null) {
            throw new TigerQuantException("privateKey is null");
        }
        return config;
    }
}
