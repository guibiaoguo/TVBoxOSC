// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

package com.github.tvbox.osc.player.danmu;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class QQDanmu {

    @SerializedName("id")
    private String id;


    @SerializedName("is_op")
    private Integer isOp;


    @SerializedName("head_url")
    private String headUrl;


    @SerializedName("time_offset")
    private Integer timeOffset;


    @SerializedName("up_count")
    private Integer upCount;


    @SerializedName("bubble_head")
    private String bubbleHead;


    @SerializedName("bubble_level")
    private String bubbleLevel;


    @SerializedName("bubble_id")
    private String bubbleId;


    @SerializedName("rick_type")
    private Integer rickType;


    @SerializedName("content_style")
    private String contentStyle;


    @SerializedName("user_vip_degree")
    private Integer userVipDegree;


    @SerializedName("create_time")
    private String createTime;


    @SerializedName("content")
    private String content;


    @SerializedName("hot_type")
    private Integer hotType;


    @SerializedName("vuid")
    private String vuid;


    @SerializedName("nick")
    private String nick;


    @SerializedName("data_key")
    private String dataKey;


    @SerializedName("content_score")
    private String contentScore;


    @SerializedName("show_weight")
    private Integer showWeight;


    @SerializedName("track_type")
    private Integer trackType;


    @SerializedName("show_like_type")
    private Integer showLikeType;

    @SerializedName("report_like_score")
    private Integer reportLikeScore;
    private String headurl;
    private String bubbleid;
    private List<Object> relateSkuInfo;
    @SerializedName("barrage_list")
    private List<QQDanmu> danmus;

    public ContentStyle getContentStyle() {
        if (TextUtils.isEmpty(contentStyle))
            return null;
        return new Gson().fromJson(contentStyle, ContentStyle.class);
    }

    public class ContentStyle {
        @SerializedName("color")
        private String color;
        @SerializedName("gradient_colors")
        private List<String> gradientColors;
        @SerializedName("position")
        private int position;

        public List<String> getGradientColors() {
            return gradientColors;
        }

        public String getColor() {
            return color;
        }
    }
    public QQDanmu() {
        danmus = new ArrayList<>();
    }

    public static QQDanmu objectFrom(String content) {
        try {
            return new Gson().fromJson(content,QQDanmu.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new QQDanmu();
        }
    }

    public String getContent() { return content; }
    public void setContent(String value) { this.content = value; }

    public String getNick() { return nick; }
    public void setNick(String value) { this.nick = value; }

    public String getHeadurl() { return headurl; }
    public void setHeadurl(String value) { this.headurl = value; }

    public String getid() { return id; }
    public void setid(String value) { this.id = value; }

    public String getBubbleHead() { return bubbleHead; }
    public void setBubbleHead(String value) { this.bubbleHead = value; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String value) { this.createTime = value; }

    public String getVuid() { return vuid; }
    public void setVuid(String value) { this.vuid = value; }

    public String getDataKey() { return dataKey; }
    public void setDataKey(String value) { this.dataKey = value; }

    public String getBubbleid() { return bubbleid; }
    public void setBubbleid(String value) { this.bubbleid = value; }

    public List<Object> getRelateSkuInfo() { return relateSkuInfo; }
    public void setRelateSkuInfo(List<Object> value) { this.relateSkuInfo = value; }

    public String getBubbleLevel() { return bubbleLevel; }
    public void setBubbleLevel(String value) { this.bubbleLevel = value; }

    public int getRickType() {
        return rickType;
    }

    public List<QQDanmu> getDanmus() {
        return danmus;
    }

    public long getTimeOffset() {
        return timeOffset;
    }
}
