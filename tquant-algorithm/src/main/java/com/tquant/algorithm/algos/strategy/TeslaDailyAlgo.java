package com.tquant.algorithm.algos.strategy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tquant.core.core.AlgoTemplate;
import com.tquant.core.model.data.Bar;
import com.tquant.core.model.data.Order;
import com.tquant.core.model.data.Tick;
import com.tquant.core.model.data.Trade;
import com.tquant.core.model.enums.OrderType;
import com.tquant.core.util.BarGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Description: Tesla daily trade strategy
 * <p>
 * 1.闄愬畾浜ゆ槗鏃舵涓哄紑鐩?.5灏忔椂
 * <p>
 * <p>
 * 2.nasdaq鎸囨暟1-3-5min bar绛栫暐涓哄紩瀵硷紝Tesla1-3-5min bar 鍏辨尟鍚庤繘琛屼拱鍏ユ垨鑰呭崠鍑? *
 * <p>
 * 3.nasdaq鎸囨暟1-3-5min bar绛栫暐璇存槑锛? *  3.1 寮€鐩?5min鍋氭暟鎹Н绱瀵?- 瑙勯伩鍦ㄩ潪纭畾鎬ф椂娈典氦鏄? *
 * 3.2 涓婁竴浜ゆ槗鏃ユ定璺屽澶ц秼鍔跨殑鍒ゆ柇褰卞搷
 * a.鏄ㄦ棩鏀舵定锛屼粖鏃ラ珮寮€
 * b.鏄ㄦ棩鏀舵定锛屼粖鏃ヤ綆寮€
 * c.鏄ㄦ棩鏀惰穼锛屼粖鏃ラ珮寮€
 * d.鏄ㄦ棩鏀惰穼锛屼粖鏃ヤ綆寮€
 * <p>
 * <p>
 * 3.3 鏈€鏂?min bar骞呭害澶у皬瀵瑰喅绛栫殑褰卞搷锛屽畾涔変俊鍙风殑寮轰腑寮? *      a.nasdaq鎸囨暟
 * 1min-bar: 寮?>=8, 涓?>=4 && <8, 寮? <4
 * 3min-bar: 寮?>=12, 涓?>=6 && <12, 寮? <6
 * <p>
 * b.tesla鑲′环
 * 1min-bar: 寮?>=0.08%, 涓?>=0.04% && <0.08%, 寮? <0.04%
 * 3min-bar: 寮?>=0.24%, 涓?>=0.12% && <0.24%, 寮? <0.12%
 * <p>
 * 鎴栬€呮崲绉嶇畻娉曪紝濡傦細鑲′环鍦ㄤ竴瀹氳寖鍥村唴鐨勭粷瀵瑰€? *          Tesla鑲′环鍦╗150,200]
 * 1min-bar: 寮?>=0.16, 涓?>=0.08 && <0.16, 寮? <0.08
 * 3min-bar: 寮?>=0.48, 涓?>=0.24 && <0.48, 寮? <0.24
 * <p>
 * <p>
 * 3.4 3min bar 鎸囨暟璺熶釜鑲″嚭鐜版定璺屼笉涓€鑷存椂鍊欑殑绛栫暐
 * a. nasdaq娑紝tesla璺屾垨鑰呮粸娑紝鍒欑瓑寰卬asdaq璺屼俊鍙凤紝1-min-bar鍋氱┖Tesla
 * b. nasdaq璺岋紝tesla娑ㄦ垨鑰呮璺岋紝鍒欑瓑寰卬asdaq娑ㄤ俊鍙凤紝1-min-bar鍋氬Tesla
 * <p>
 * <p>
 * 3.5 甯傚満骞叉壈淇″彿鐨勫簲瀵圭瓥鐣? *
 * <p>
 * 3.6 鏄惁鍋氭寔浠撹繃澶滐紵鍒跺畾绛栫暐鍒ゆ柇鏍囧噯
 * <p>
 * <p>
 * <p>
 * 4.浠撲綅澶у皬璇存槑
 * 4.1 鍋氭暟鎹暱鏈熺粺璁★紝鍖呮嫭鑳滅巼銆佺泩浜忔瘮锛岀粨鍚堝嚡鍒╁叕寮忓仛绋嬪簭鍖栦粨浣嶈瀹? *      f = (bp -1) / (b-1)    b:涓虹泩浜忔瘮 p:鑳滅巼锛屽鑳滅巼p=60%锛?.6锛?鐩堜簭姣攂=2 鍒欐渶浣充粨浣?(2*0.6-1)/(2-1)=0.2 浠撲綅涓嶈秴杩?0%
 * <p>
 * 4.2 浠撲綅鎸夌収1-2-3-4娉曞垯鍋氳瀹氾紵
 * 100 - 200 - 300 - 400 鑲¤繖鏍风殑涔板叆鏂瑰紡
 * 浠esla涓轰緥1min-bar鍋?00浠撲綅涔板叆锛?min-bar纭鍚庝拱鍏?00锛岃秼鍔垮欢缁泩鍒╁姞浠撳埌300/400
 * <p>
 * <p>
 * 5.姝㈡崯&姝㈢泩璇存槑
 * 5.1 缁濆鐐规暟(鐧惧垎姣?鍥炴挙姝㈡崯(姝㈢泩)
 * a.鏃ュ唴浜ゆ槗浜忔崯閲戦瓒呰繃璐︽埛 2% 鍋滄褰撴棩鎵€鏈変氦鏄擄紝娓呯┖浜ゆ槗浠撲綅
 * b.缁濆閲戦鏁版鎹燂紝鏃ュ唴浜忔崯瓒呰繃4000娓竵锛屽仠姝㈠綋鏃ユ墍鏈夐噺鍖栦氦鏄? *
 * <p>
 * 5.2 涓婁竴涓?min-bar open/low price鍋氭鎹?姝㈢泩)锛屾鎹熺殑璇濆仛娓呬粨锛屾诞鍔ㄦ鐩堢殑璇濋噰鐢ㄥ垎鎵圭瓥鐣? *
 * <p>
 * 5.3 鐩堝埄鍔犱粨绛栫暐璇存槑
 * <p>
 * <p>
 * <p>
 * <p>
 * 6.绛栫暐鏍稿績搴曞眰閫昏緫
 * a.甯傚満鍔ㄨ兘瓒嬪娍 - 1-3-5min bar鍘诲畾涔夎繖绉嶈秼鍔? *  b.nasdaq浣滀负鏁翠綋甯傚満鎯呯华鐨勫弬鑰冿紝Tesla褰㈡垚琛屾儏鍏辨尟鍘诲仛涔板崠鍐崇瓥
 * c.璁惧畾鍏锋湁缁熻鏁版嵁鏀拺鐨勬鎹?姝㈢泩
 * d.鍏呭垎鐨勬暟鎹洖娴嬶紝骞惰皟鏁村ソ浜ゆ槗鍙傛暟锛氫粨浣嶏紝姝㈡崯锛屾鐩? *
 * <p>
 * <p>
 * 7.鍥炴祴鏁版嵁缁熻楠岃瘉
 * a.鏃ュ唴娉㈠姩骞呭害
 * b.浠ヤ笂绛栫暐瀹為檯妯℃嫙鏁堟灉
 * <p>
 * <p>
 * <p>
 * 8.瀹炵洏灏忎粨浣嶇嚎涓婇獙璇佷竴娈垫椂闂达紝鏍规嵁瀹為檯鏁堟灉纭畾鏄惁鍔犲ぇ浠撲綅銆? *
 *
 * @author arhaiyun
 * @date 2023/05/20
 */
public class TeslaDailyAlgo extends AlgoTemplate {

    private String todayDate;
    private Tick lastTick;
    private String direction;
    private int volume;
    private String orderId;
    private double orderPrice;
    private int traded = 0;
    private String symbol;
    private List<String> symbols = Lists.newArrayList();
    private BarGenerator barGenerator;
    private BarGenerator min1BarGenerator;
    private BarGenerator min3BarGenerator;
    private BarGenerator min5BarGenerator;

    /**
     * 淇濆瓨涓嶅悓symbol褰撴棩涓嶅悓绫诲瀷bar鏁版嵁淇℃伅
     * 濡? <tsla,{<1min,bars>,<3min,3-bars>,<5min,5-bars>}>
     */
    Map<String, Map<String, List<Bar>>> symbolBarData = Maps.newConcurrentMap();

    // 姣忔棩寮€鐩樺墠鍚姩褰撳ぉ鏃ユ湡淇℃伅
    // 璺濈寮€鐩樻椂闂磋绠楋紝瓒呰繃90min鍋滄浜ゆ槗
    // 鎸囨爣鏁版嵁淇℃伅锛?min/sec绾у埆鐪嬫定/璺屾暟鎹?min1CallIndi/min1PutIndi  > threshold:3 then call or put. 閽堝 tick 绾у埆鐨勫搷搴?

    public TeslaDailyAlgo() {
    }

    public TeslaDailyAlgo(Map<String, Object> settings) {
        super(settings);
    }

    @Override
    public void init() {
        this.direction = (String) settings.get("direction");
        this.volume = (Integer) settings.get("volume");
        this.symbol = (String) settings.get("symbol");
        this.symbols.add(symbol);

        this.todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log("TeslaDailyAlgo init at:" + todayDate);
    }

    @Override
    public void onStart() {

        symbols.add("TSLA");
        symbols.add("Nasdaq");

        for (String symbol : symbols) {
            subscribe(symbol);
        }

        barGenerator = new BarGenerator(bar -> onBar(bar));

        min1BarGenerator = new BarGenerator(symbols, 1, bar -> on1minBar(bar));
        min3BarGenerator = new BarGenerator(symbols, 3, bar -> on3minBar(bar));
        min5BarGenerator = new BarGenerator(symbols, 5, bar -> on5minBar(bar));
    }

    /**
     * 鑾峰彇 tick 绾у埆鏁版嵁锛屽苟鏇存柊 bar 鏁版嵁
     */
    @Override
    public void onTick(Tick tick) {
        this.lastTick = tick;

        barGenerator.updateTick(tick);
        min1BarGenerator.updateTick(tick);
        min3BarGenerator.updateTick(tick);
        min5BarGenerator.updateTick(tick);

    }

    /**
     * 鍩轰簬1-min鐨刡ar鐢熸垚鍏跺畠鍛ㄦ湡鐨刡ar鏁版嵁
     */
    @Override
    public void onBar(Bar bar) {
        log("{} onBar {}", getAlgoName(), bar);
        min1BarGenerator.updateBar(bar);
        min3BarGenerator.updateBar(bar);
        min5BarGenerator.updateBar(bar);
    }

    /**
     * 鏍规嵁鏈€鏂癰ar鏁版嵁淇℃伅锛屽仛浜ゆ槗鐩稿叧鐨勫喅绛?
     */
    public void on1minBar(Bar bar) {
        log("{} on1minBar {}", bar);
    }

    public void on3minBar(Bar bar) {
        log("{} on3minBar {}", bar);
    }

    public void on5minBar(Bar bar) {
        log("{} on5minBar {}", bar);
    }

    /**
     * 姝㈡崯浠锋牸璁惧畾 stopLosePrice
     * 1. 缁濆鐩堜簭姝㈡崯锛氭寔浠撲簭鎹熻秴杩?2000
     * 2. 閫氳繃k绾垮墠浣庝繚鎶ゆ鎹?     *  2.1 ...
     * <p>
     * 濡傛灉鏄volume浠撲綅锛屽彲浠ラ噰鐢ㄥ垎鎵瑰噺浠撴鎹熺殑绛栫暐
     * <p>
     * riskControlModule
     */
    public void stopLosePrice() {

    }

    /**
     * 鐩堝埄鍔犱粨绛栫暐
     * Tesla 閲囩敤鐨?"2111-姣斾緥绛栫暐"
     * 鎸佷粨娴泩锛屽嚭鐜颁簩娆″悓鍚戜氦鏄撲俊鍙峰垯鐩堝埄鍔犱粨
     */
    public void profitPlusPosition() {

    }

    /**
     * 褰撳墠璐︽埛淇℃伅鑾峰彇
     * 璐︽埛璧勪骇淇℃伅锛屽綋鏃ョ泩浜忕姸鍐?
     */
    public void getCurAccountInfo() {

    }

    /**
     * 鍙戦€佹秷鎭彁閱掓ā鍧?     * 1.浜ゆ槗淇″彿
     * 2.瀹為檯鎴愪氦
     * 3.姝㈡崯/姝㈢泩
     * <p>
     * 鐭俊銆佺數璇濄€侀偖浠躲€佸井淇?.
     */
    public void sentAlarmMsg() {

    }

    /**
     * 鎸佷箙鍖栧瓨鍌ㄦā鍧?     * 1. 璁板綍褰撴棩浜ゆ槗淇″彿
     * 2. 璁板綍褰撴棩瀹為檯鎴愪氦
     * <p>
     * 璁捐琛ㄦ満鏋勶紝瀹氭湡鍋氭暟鎹粺璁★紝鍖呮嫭PNL,鐩堜簭姣?鑳滅巼..绛変俊鎭紝鍙嶅摵浜ゆ槗鍐崇瓥
     */
    public void tradeLogPersistence() {

    }

    /**
     * 鏍稿績浜ゆ槗绠楁硶閫昏緫
     */
    public void coreTradeAlgo() {

    }


    private void buyBestLimit() {
        int orderVolume = volume - traded;
        orderPrice = lastTick.getBidPrice();
        if (orderPrice > 0) {
            buy(symbol, orderPrice, orderVolume, OrderType.LMT);
        }
    }

    private void sellBestLimit() {
        int orderVolume = volume - traded;
        orderPrice = lastTick.getAskPrice();
        if (orderPrice > 0) {
            sell(symbol, orderPrice, orderVolume, OrderType.LMT);
        }
    }

    @Override
    public void onOrder(Order order) {
        if (!order.isActive()) {
            orderId = "";
            orderPrice = 0;
        }
    }

    @Override
    public void onTrade(Trade trade) {
        this.traded += trade.getVolume();
        if (traded >= trade.getVolume()) {
            log("{} onTrade traded:{},volume:{}", getAlgoName(), traded, trade.getVolume());
            stop();
        }
    }


}

