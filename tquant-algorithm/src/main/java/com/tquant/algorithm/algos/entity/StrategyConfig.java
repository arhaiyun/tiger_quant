package com.tquant.algorithm.algos.entity;

import java.util.Map;

/**
 * Description: Strategy configuration class to manage strategy parameters
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class StrategyConfig {
    private String symbol;
    private String direction;
    private double price;
    private int volume;
    private int shortSma;
    private int longSma;
    private double stopLoss;
    private double takeProfit;

    public StrategyConfig() {}

    public StrategyConfig(Map<String, Object> settings) {
        this.symbol = (String) settings.get("symbol");
        this.direction = (String) settings.get("direction");
        this.price = settings.get("price") != null ? (Double) settings.get("price") : 0.0;
        this.volume = settings.get("volume") != null ? (Integer) settings.get("volume") : 0;
        this.shortSma = settings.get("shortSma") != null ? (Integer) settings.get("shortSma") : 5;
        this.longSma = settings.get("longSma") != null ? (Integer) settings.get("longSma") : 10;
        this.stopLoss = settings.get("stopLoss") != null ? (Double) settings.get("stopLoss") : 0.0;
        this.takeProfit = settings.get("takeProfit") != null ? (Double) settings.get("takeProfit") : 0.0;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getShortSma() {
        return shortSma;
    }

    public void setShortSma(int shortSma) {
        this.shortSma = shortSma;
    }

    public int getLongSma() {
        return longSma;
    }

    public void setLongSma(int longSma) {
        this.longSma = longSma;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getTakeProfit() {
        return takeProfit;
    }

    public void setTakeProfit(double takeProfit) {
        this.takeProfit = takeProfit;
    }
}