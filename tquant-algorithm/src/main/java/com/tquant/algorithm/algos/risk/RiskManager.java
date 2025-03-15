package com.tquant.algorithm.algos.risk;

import com.tquant.core.model.data.Position;
import com.tquant.core.model.data.Trade;
import com.tquant.algorithm.algos.entity.StrategyConfig;

/**
 * Description: Risk management class to handle stop loss and take profit logic
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class RiskManager {
    private final StrategyConfig config;
    private double entryPrice;

    public RiskManager(StrategyConfig config) {
        this.config = config;
    }

    /**
     * Update entry price when new trade occurs
     *
     * @param trade Latest trade information
     */
    public void updateEntryPrice(Trade trade) {
        if (trade != null) {
            this.entryPrice = trade.getPrice();
        }
    }

    /**
     * Check if position should be closed based on stop loss or take profit levels
     *
     * @param currentPrice Current market price
     * @param position Current position
     * @return true if position should be closed, false otherwise
     */
    public boolean shouldClosePosition(double currentPrice, Position position) {
        if (position == null || position.getPosition() == 0) {
            return false;
        }

        double pnlPercent = (currentPrice - entryPrice) / entryPrice * 100;

        // Check stop loss
        if (config.getStopLoss() > 0 && pnlPercent <= -config.getStopLoss()) {
            return true;
        }

        // Check take profit
        if (config.getTakeProfit() > 0 && pnlPercent >= config.getTakeProfit()) {
            return true;
        }

        return false;
    }

    /**
     * Get current profit/loss percentage
     *
     * @param currentPrice Current market price
     * @return Current P/L percentage
     */
    public double getCurrentPnlPercent(double currentPrice) {
        if (entryPrice == 0) {
            return 0.0;
        }
        return (currentPrice - entryPrice) / entryPrice * 100;
    }
}