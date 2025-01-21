package com.github.tvbox.osc.player.danmu;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class BillDanmu {

    public BillDanmu() {
        this.danmus = new ArrayList<>();
    }

    /**
     * 弹幕列表
     */
    @SerializedName("danmus")
    private List<BillDanmu> danmus;
    /**
     * 弹幕dmID
     */
    @SerializedName("id")
    private Long id;
    /**
     * 视频内弹幕出现时间
     */
    @SerializedName("progress")
    private int progress;
    /**
     * 弹幕类型
     * 1 2 3：普通弹幕
     * 4：底部弹幕
     * 5：顶部弹幕
     * 6：逆向弹幕
     * 7：高级弹幕
     * 8：代码弹幕
     * 9：BAS弹幕
     */
    @SerializedName("mode")
    private int mode;
    /**
     * 弹幕字号
     * 18：小
     * 25：标准
     * 36：大
     */
    @SerializedName("fontsize")
    private int fontsize;
    /**
     * 弹幕颜色
     */
    @SerializedName("color")
    private int color;
    /**
     * 发送者UID的HASH
     * 用于屏蔽用户和查看用户发送的所有弹幕 也可反查用户ID
     */
    @SerializedName("midHash")
    private String midHash;
    /**
     * 弹幕内容
     */
    @SerializedName("content")
    private String content;
    /**
     * 弹幕发送时间
     * 时间戳
     */
    @SerializedName("ctime")
    private long ctime;
    /**
     * 弹幕池
     * 0：普通池
     * 1：字幕池
     * 2：特殊池(代码/BAS弹幕)
     */
    @SerializedName("pool")
    private int pool;
    /**
     * 弹幕dmID的字符串类型
     * 唯一 可用于操作参数
     */
    @SerializedName("weight")
    private int weight;
    @SerializedName("idStr")
    private String idStr;
    @SerializedName("attr")
    private String attr;

    public static BillDanmu objectFrom(String content) {
        try {
            return new Gson().fromJson(content,BillDanmu.class);
        } catch (Exception e) {
            return new BillDanmu();
        }
    }

    public Long getId() {
        return id;
    }

    public int getProgress() {
        return progress;
    }

    public int getMode() {
        return mode;
    }

    public int getFontsize() {
        return fontsize;
    }

    public int getColor() {
        return color;
    }

    public String getMidHash() {
        return midHash;
    }

    public String getContent() {
        return content;
    }

    public long getCtime() {
        return ctime;
    }

    public int getWeight() {
        return weight;
    }

    public String getIdStr() {
        return idStr;
    }

    public String getAttr() {
        return attr;
    }

    public List<BillDanmu> getDanmus() {
        return danmus;
    }

}
