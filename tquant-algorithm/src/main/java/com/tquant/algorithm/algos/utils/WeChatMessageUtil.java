package com.tquant.algorithm.algos.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信消息发送工具类
 *
 * @author arhaiyun
 */
public class WeChatMessageUtil {
    private static final Logger logger = LoggerFactory.getLogger(WeChatMessageUtil.class);
    private static final String WEBHOOK_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=f788bfe5-04b9-40ca-b8af-0f2a65d2e6bc";
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");


    /**
     * 发送交易信号消息到企业微信机器人
     *
     * @param signal 交易信号类型（多/空）
     * @param time   触发时间
     * @param price  触发价格
     * @return 是否发送成功
     */
    public static boolean sendTradeSignal(String symbol, String signal, String time, String price) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("content", String.format("交易信号提醒\n交易标的: %s\n信号类型: %s\n触发时间: %s\n触发价格: %s", symbol, signal, time, price));

            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "text");
            message.put("text", content);

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, JSON.toJSONString(message));
            Request request = new Request.Builder()
                    .url(WEBHOOK_URL)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("微信消息发送成功: {}", signal);
                    return true;
                } else {
                    logger.error("微信消息发送失败: {}, 状态码: {}", signal, response.code());
                }
            }
        } catch (IOException e) {
            logger.error("微信消息发送异常", e);
        }
        return false;
    }
}