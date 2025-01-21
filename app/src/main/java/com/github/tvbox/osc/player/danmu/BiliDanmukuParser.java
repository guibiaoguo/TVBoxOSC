package com.github.tvbox.osc.player.danmu;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.github.tvbox.osc.util.ColorHelper;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkUtils;
import com.github.tvbox.osc.util.MD5;
import com.google.protobuf.util.JsonFormat;
import com.lzy.okgo.OkGo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.SpecialDanmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class BiliDanmukuParser extends BaseDanmakuParser {
    private BaseDanmakuParser.Listener listener;
    public BiliDanmukuParser(String path) {
        long start = new Date().getTime();
        Log.d("start",start+"");
        this.billDanmuList = new ArrayList<>();
        String[] paths = path.split("\\?");
        Map<String,String> params = new HashMap<>();
        for (String p:paths[1].split("&")){
            params.put(p.split("=")[0],p.split("=")[1]);
        }
        String content = getContent(path);
        double i = 1;
        setListener(listener);
        while (!content.equals("{\n}")) {
            BillDanmu bill = BillDanmu.objectFrom(content);
            billDanmuList.addAll(bill.getDanmus());
            StringBuilder builder = new StringBuilder();
            if (i<2){
                i=i+0.5;
                builder.append("oid=").append(params.get("oid")).append("&pe=").append(i==1?120000:360000).append("&pid=").append(params.get("pid"));
                builder.append("&ps=").append(i==1?0:120000).append("&pull_mode=1&segment_index=1");
                builder.append("&type=1&web_location=1315873");
            } else {
                i=Math.floor(i+1);
                builder.append("oid=").append(params.get("oid")).append("&pid=").append(params.get("pid"));
                builder.append("&segment_index=").append((int)i);
                builder.append("&type=1&web_location=1315873");
            }
            builder.append("&wts="+(System.currentTimeMillis()/1000));
            String md5 = MD5.encode(builder + "ea1db124af3c7062474693fa704f4ff8");
            String url = paths[0] + "?" + builder + "&w_rid=" + md5;
            content = getContent(url);
        }
        long end = new Date().getTime();
        Log.d("end",end+" 耗时 "+(end-start)/1000.0);
    }

    private final List<BillDanmu> billDanmuList;
    private BaseDanmaku item;
    private float scaleX;
    private float scaleY;
    private int index;

    private String getContent(String path) {
        if (path.startsWith("file")) return FileUtils.read(path);
        if (path.startsWith("http")) {
            try {
                InputStream inputStream =  OkGo.<String>get(path).execute().body().byteStream();
                BillDanmuEntity.Danmus bill = BillDanmuEntity.Danmus.parseFrom(inputStream);
                String json = JsonFormat.printer().print(bill);
                return json;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "{\n}";
    }

    @Override
    protected Danmakus parse() {
        Danmakus result = new Danmakus(IDanmakus.ST_BY_TIME);
        for (BillDanmu billDanmu1:billDanmuList) {
            setParam(billDanmu1);
            setText(billDanmu1.getContent());
            synchronized (result.obtainSynchronizer()) {
                result.addItem(item);
            }
        }
        return result;
    }

    @Override
    public BaseDanmakuParser setDisplayer(IDisplayer display) {
        super.setDisplayer(display);
        scaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH;
        scaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT;
        return this;
    }

    private void setParam(BillDanmu billDanmu) {
        int type = billDanmu.getMode();
        long time = billDanmu.getProgress();
        float size = billDanmu.getFontsize() * (mDispDensity - 0.6f);
        int color = HawkUtils.getDanmuColor()? ColorHelper.getCN():(int) ((0x00000000ff000000L | billDanmu.getColor()) & 0x00000000ffffffffL);
        item = mContext.mDanmakuFactory.createDanmaku(type, mContext);
        item.setTime(time);
        item.setTimer(mTimer);
        item.textSize = size;
        item.textColor = color;
        item.textShadowColor = color <= Color.BLACK ? Color.WHITE : Color.BLACK;
        item.flags = mContext.mGlobalFlagValues;
    }

    private void setText(String text) {
        item.index = index++;
        DanmakuUtils.fillText(item, decodeXmlString(text));
        if (item.getType() == BaseDanmaku.TYPE_SPECIAL && text.startsWith("[") && text.endsWith("]")) setSpecial();
    }

    private void setSpecial() {
        String[] textArr = null;
        try {
            JSONArray jsonArray = new JSONArray(item.text.toString());
            textArr = new String[jsonArray.length()];
            for (int i = 0; i < textArr.length; i++) {
                textArr[i] = jsonArray.getString(i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (textArr == null || textArr.length < 5 || TextUtils.isEmpty(textArr[4])) {
            item = null;
            return;
        }
        DanmakuUtils.fillText(item, textArr[4]);
        float beginX = Float.parseFloat(textArr[0]);
        float beginY = Float.parseFloat(textArr[1]);
        float endX = beginX;
        float endY = beginY;
        String[] alphaArr = textArr[2].split("-");
        int beginAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[0]));
        int endAlpha = beginAlpha;
        if (alphaArr.length > 1) {
            endAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[1]));
        }
        long alphaDuraion = (long) (Float.parseFloat(textArr[3]) * 1000);
        long translationDuration = alphaDuraion;
        long translationStartDelay = 0;
        float rotateY = 0, rotateZ = 0;
        if (textArr.length >= 7) {
            rotateZ = Float.parseFloat(textArr[5]);
            rotateY = Float.parseFloat(textArr[6]);
        }
        if (textArr.length >= 11) {
            endX = Float.parseFloat(textArr[7]);
            endY = Float.parseFloat(textArr[8]);
            if (!"".equals(textArr[9])) {
                translationDuration = Integer.parseInt(textArr[9]);
            }
            if (!"".equals(textArr[10])) {
                translationStartDelay = (long) (Float.parseFloat(textArr[10]));
            }
        }
        if (isPercentageNumber(textArr[0])) {
            beginX *= DanmakuFactory.BILI_PLAYER_WIDTH;
        }
        if (isPercentageNumber(textArr[1])) {
            beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
        }
        if (textArr.length >= 8 && isPercentageNumber(textArr[7])) {
            endX *= DanmakuFactory.BILI_PLAYER_WIDTH;
        }
        if (textArr.length >= 9 && isPercentageNumber(textArr[8])) {
            endY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
        }
        item.duration = new Duration(alphaDuraion);
        item.rotationZ = rotateZ;
        item.rotationY = rotateY;
        mContext.mDanmakuFactory.fillTranslationData(item, beginX, beginY, endX, endY, translationDuration, translationStartDelay, scaleX, scaleY);
        mContext.mDanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuraion);
        if (textArr.length >= 12) {
            if (!TextUtils.isEmpty(textArr[11]) && "true".equalsIgnoreCase(textArr[11])) {
                item.textShadowColor = Color.TRANSPARENT;
            }
        }
        if (textArr.length >= 14) {
            ((SpecialDanmaku) item).isQuadraticEaseOut = ("0".equals(textArr[13]));
        }
        if (textArr.length >= 15) {
            if (!"".equals(textArr[14])) {
                String motionPathString = textArr[14].substring(1);
                if (!TextUtils.isEmpty(motionPathString)) {
                    String[] pointStrArray = motionPathString.split("L");
                    if (pointStrArray.length > 0) {
                        float[][] points = new float[pointStrArray.length][2];
                        for (int i = 0; i < pointStrArray.length; i++) {
                            String[] pointArray = pointStrArray[i].split(",");
                            if (pointArray.length >= 2) {
                                points[i][0] = Float.parseFloat(pointArray[0]);
                                points[i][1] = Float.parseFloat(pointArray[1]);
                            }
                        }
                        DanmakuFactory.fillLinePathData(item, points, scaleX, scaleY);
                    }
                }
            }
        }
    }

    private boolean isPercentageNumber(String number) {
        return number != null && number.contains(".");
    }

    private String decodeXmlString(String title) {
        if (title.contains("&amp;")) title = title.replace("&amp;", "&");
        if (title.contains("&quot;")) title = title.replace("&quot;", "\"");
        if (title.contains("&gt;")) title = title.replace("&gt;", ">");
        if (title.contains("&lt;")) title = title.replace("&lt;", "<");
        return title;
    }

}