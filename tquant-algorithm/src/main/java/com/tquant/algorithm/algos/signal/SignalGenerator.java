package com.tquant.algorithm.algos.signal;

import com.tquant.core.model.data.Bar;
import com.tquant.core.indicators.Indicators;
import java.util.List;

/**
 * Description: Trading signal generator to handle signal generation logic
 *
 * @author arhaiyun
 * @date 2025/03/15
 */
public class SignalGenerator {
    private final Indicators indicators;
    private List<Bar> shortSma;
    private List<Bar> longSma;

    public SignalGenerator() {
        this.indicators = new Indicators();
    }

    /**
     * Generate trading signals based on SMA crossover
     *
     * @param bars Historical price bars
     * @param shortPeriod Short period for SMA calculation
     * @param longPeriod Long period for SMA calculation
     * @return true if buy signal, false if sell signal
     */
    public boolean generateSmaCrossSignal(List<Bar> bars, int shortPeriod, int longPeriod) {
        if (bars == null || bars.isEmpty() || bars.size() < longPeriod) {
            return false;
        }

        shortSma = (List<Bar>) indicators.sma(bars, shortPeriod);
        longSma = (List<Bar>) indicators.sma(bars, longPeriod);

        int lastIndex = bars.size() - 1;
        return shortSma.get(lastIndex).attr("close_ma" + shortPeriod) > 
               longSma.get(lastIndex).attr("close_ma" + longPeriod);
    }

    /**
     * Get the latest short period SMA value
     *
     * @return Latest short period SMA value
     */
    public double getLatestShortSma() {
        if (shortSma == null || shortSma.isEmpty()) {
            return 0.0;
        }
        return shortSma.get(shortSma.size() - 1).getClose();
    }

    /**
     * Get the latest long period SMA value
     *
     * @return Latest long period SMA value
     */
    public double getLatestLongSma() {
        if (longSma == null || longSma.isEmpty()) {
            return 0.0;
        }
        return longSma.get(longSma.size() - 1).getClose();
    }
}