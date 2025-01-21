package com.github.tvbox.osc.player.danmu;

import java.io.Serializable;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * <p>
 * TODO
 * </p>
 *
 * @author ${USER}
 * @since 2025-01-12 01:21:31
 **/
public class MGTVDanmu implements Serializable {

    @SerializedName("status")
    private int status;
    @SerializedName("data")
    private Data data;

    public Data getData() {
        return data;
    }

    public static MGTVDanmu objectFrom(String content) {
        try {
            return new Gson().fromJson(content,MGTVDanmu.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new MGTVDanmu();
        }

    }

    public class Data {
        @SerializedName("next")
        private int next;
        @SerializedName("total")
        private int total;
        @SerializedName("interval")
        private int interval;
        @SerializedName("items")
        private List<Danmu> danmus;

        public List<Danmu> getDanmus() {
            return danmus;
        }
    }

    public class Danmu {

        @SerializedName("id")
        private String id;

        @SerializedName("ids")
        private String ids;

        @SerializedName("type")
        private Integer type;

        @SerializedName("uid")
        private String uid;

        @SerializedName("uuid")
        private String uuid;

        @SerializedName("content")
        private String content;

        @SerializedName("time")
        private Integer time;

        @SerializedName("v2_up_count")
        private Integer v2UpCount;

        @SerializedName("v2_position")
        private Integer v2Position;

        @SerializedName("v2_color")
        private MGTVDanmuV2Color v2Color;

        public String getContent() {
            return content;
        }

        public Integer getTime() {
            return time;
        }

        public Integer getV2UpCount() {
            return v2UpCount;
        }

        public Integer getV2Position() {
            return v2Position;
        }

        public MGTVDanmuV2Color getV2Color() {
            return v2Color;
        }
    }
    public class MGTVDanmuV2Color {
        @SerializedName("color_left")
        private MGTVDanmuV2ColorLeft colorLeft;
        @SerializedName("color_right")
        public MGTVDanmuV2ColorRight colorRight;

        public MGTVDanmuV2ColorLeft getColorLeft() {
            return colorLeft;
        }

        public MGTVDanmuV2ColorRight getColorRight() {
            return colorRight;
        }
    }
    public class MGTVDanmuV2ColorLeft {

        @SerializedName("r")
        private Integer r;

        @SerializedName("g")
        private Integer g;

        @SerializedName("b")
        private Integer b;

        public Integer getR() {
            return r;
        }

        public Integer getG() {
            return g;
        }

        public Integer getB() {
            return b;
        }
    }

    public class MGTVDanmuV2ColorRight {

        @SerializedName("r")
        private Integer r;

        @SerializedName("g")
        private Integer g;

        @SerializedName("b")
        private Integer b;
    }


}