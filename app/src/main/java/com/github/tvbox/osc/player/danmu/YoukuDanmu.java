package com.github.tvbox.osc.player.danmu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * TODO 
 * </p>
 * @author bill
 * @since  
 **/
public class YoukuDanmu implements Serializable {

    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static YoukuDanmu objectFrom(String content) {
        try {
            JsonObject data = gson.fromJson(content, JsonObject.class).getAsJsonObject("data");
            content = data.get("result").getAsString();
            return gson.fromJson(content,YoukuDanmu.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new YoukuDanmu();
        }
    }

    @SerializedName("code")
    private int code;
    @SerializedName("cost")
    private int cost;
    @SerializedName("data")
    private Data data;
    @SerializedName("message")
    private String message;

    public class ExtFields {
        @SerializedName("aigc")
        private Integer aigc;

        @SerializedName("grade")
        private Integer grade;

        @SerializedName("voteUp")
        private Integer voteUp;
    }
    public class Danmu {
        @SerializedName("mat")
        private Integer mat;


        @SerializedName("createtime")
        private String createtime;


        @SerializedName("ver")
        private Integer ver;


        @SerializedName("propertis")
        private String propertis;


        @SerializedName("iid")
        private String iid;


        @SerializedName("level")
        private Integer level;


        @SerializedName("lid")
        private Integer lid;


        @SerializedName("type")
        private Integer type;


        @SerializedName("content")
        private String content;


        @SerializedName("extFields")
        private ExtFields extFields;


        @SerializedName("ct")
        private Integer ct;


        @SerializedName("uid")
        private String uid;


        @SerializedName("uid2")
        private String uid2;


        @SerializedName("ouid")
        private String ouid;


        @SerializedName("playat")
        private Integer playat;


        @SerializedName("id")
        private String id;


        @SerializedName("aid")
        private Integer aid;


        @SerializedName("status")
        private Integer status;

        public Integer getMat() {
            return mat;
        }

        public String getCreatetime() {
            return createtime;
        }

        public Integer getVer() {
            return ver;
        }

        public String getPropertis() {
            return propertis;
        }

        public String getIid() {
            return iid;
        }

        public Integer getLevel() {
            return level;
        }

        public Integer getLid() {
            return lid;
        }

        public Integer getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public ExtFields getExtFields() {
            return extFields;
        }

        public Integer getCt() {
            return ct;
        }

        public String getUid() {
            return uid;
        }

        public String getUid2() {
            return uid2;
        }

        public String getOuid() {
            return ouid;
        }

        public Integer getPlayat() {
            return playat;
        }

        public String getId() {
            return id;
        }

        public Integer getAid() {
            return aid;
        }

        public Integer getStatus() {
            return status;
        }
    }

    public class Data {
        @SerializedName("result")
        private List<Danmu> result;
        @SerializedName("count")
        private int count;
        @SerializedName("filtered")
        private int filtered;
        @SerializedName("scm")
        private double scm;

        public List<Danmu> getResult() {
            return result;
        }
    }

    public Data getData() {
        return data;
    }
}