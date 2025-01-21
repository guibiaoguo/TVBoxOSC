package com.github.tvbox.osc.player.danmu;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import com.github.tvbox.osc.util.ColorHelper;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkUtils;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.ZLibUtils;
import com.google.protobuf.util.JsonFormat;
import com.lzy.okgo.OkGo;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;
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

public class IQiYiDanmuParser extends BaseDanmakuParser {
    private BaseDanmakuParser.Listener listener;
    public IQiYiDanmuParser(String path) {
        this.iQiYiDanmuList = new ArrayList<>();
        String content = getContent(path);
        int i = 1;
        setListener(listener);
        while (!TextUtils.isEmpty(content)) {
            i++;
            IQiYiDanmu iQiYiDanmu = IQiYiDanmu.fromXml(content);
            iQiYiDanmuList.addAll(iQiYiDanmu.getData());
            StringBuilder builder = new StringBuilder();
            builder.append(path.replace("_1.z","")).append("_").append(i).append(".z");
            String url = builder.toString();
            content = getContent(url);
        }
    }

    private final List<IQiYiDanmu.Entry> iQiYiDanmuList;
    private BaseDanmaku item;
    private float scaleX;
    private float scaleY;
    private int index;

    private String getContent(String path) {
        if (path.startsWith("file")) return FileUtils.read(path);
        if (path.startsWith("http")) {
            try {
                InputStream inputStream =  OkGo.<String>get(path).execute().body().byteStream();
                byte[] bytes = ZLibUtils.decompress(inputStream);
                String json = new String(bytes,"utf-8");
                return json;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    protected Danmakus parse() {
        Danmakus result = new Danmakus(IDanmakus.ST_BY_TIME);
        for (IQiYiDanmu.Entry entry: iQiYiDanmuList) {
            for (IQiYiDanmu.BulletInfo bulletInfo:entry.getList()) {
                setParam(bulletInfo);
                setText(bulletInfo.getContent());
                synchronized (result.obtainSynchronizer()) {
                    result.addItem(item);
                }
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

    private void setParam(IQiYiDanmu.BulletInfo bulletInfo) {
        int type = 1;
        long time = bulletInfo.getShowTime()*1000;
        float size = bulletInfo.getFont() * (mDispDensity - 0.6f);
        int color = ColorHelper.getCN();
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