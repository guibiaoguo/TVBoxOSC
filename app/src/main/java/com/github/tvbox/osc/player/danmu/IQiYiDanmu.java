package com.github.tvbox.osc.player.danmu;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

import java.util.List;

@Root(name = "danmu", strict = false)
public class IQiYiDanmu {

    @Element(data=false, required=false)
    private String code;
    @Element(data=false, required=false)
    private int sum;
    @Element(data=false, required=false)
    private int validSum;
    @Element(data=false, required=false)
    private int duration;
    @Element(data=false, required=false)
    private String ts;

    public void setCode(String code) {
        this.code = code;
    }

    @ElementList(name = "data",data = false , required = false)
    private List<Entry> data;

    public List<Entry> getData() {
        return data;
    }

    public void setData(List<Entry> data) {
        this.data = data;
    }

    public static IQiYiDanmu fromXml(String str) {
        try {
            return new Persister().read(IQiYiDanmu.class, str);
        } catch (Exception e) {
            e.printStackTrace();
            return new IQiYiDanmu();
        }
    }

    public static class Entry {
        @ElementList(name = "list",data = false,inline = false, empty = false)
        private List<BulletInfo> list;
        @Element(name = "int",required = false,data = false)
        private int id;
        public List<BulletInfo> getList() {
            return list;
        }

        public void setList(List<BulletInfo> list) {
            this.list = list;
        }
    }

    public static class UserInfo {
        @Element(data=false, required=false)
        public String senderAvatar;
        @Element(data=false, required=false)
        public String udid;
        @Element(data=false, required=false)
        public String uid;
        @Element(data=false, required=false)
        public String name;
        @Element(required = false)
        private int avatarId;
        @Element(required = false)
        private int avatarVipLevel;
        @Element(required = false)
        private String picL;
        @Element(required = false)
        private String desc;

    }

    public static class BulletInfo {
        @Element(data=false, required=false)
        private String color;
        @Element(data=false, required=false)
        private String src;
        @Element(data=false, required=false)
        private long showTime;
        @Element(data=false, required=false)
        private String scoreLevel;
        @Element(data=false, required=false)
        private String contentId;
        @Element(data=false, required=false)
        private String likeCount;
        @Element(data=false, required=false)
        private String content;
        @Element(data=false, required=false)
        private String parentId;
        @Element(data=false, required=false)
        private String dissCount;
        @Element(data=false, required=false)
        private String score;
        @Element(data=false, required=false)
        private String spoilerGuess;
        @Element(data=false, required=false)
        private String background;
        @Element(data=false, required=false)
        private String subType;
        @Element(data=false, required=false)
        private String spoiler;
        @Element(data=false, required=false)
        private String position;
        @Element(data=false, required=false)
        private String opacity;
        @Element(data=false, required=false)
        private String contentType;
        @Element(data=false, required=false)
        private String halfScreenShow;
        @Element(data=false, required=false)
        private int font;
        @Element(data=false, required=false)
        private String plusCount;
        @Element(data=false, required=false)
        private String isShowLike;
        @Element(name = "userInfo",data = false , required = false)
        private UserInfo userInfo;
        @Element(data = false,required = false)
        private int variableEffectId;
        @Element(data = false,required = false)
        private String isReply;
        @Element(data = false,required = false)
        private Boolean isShowLikeTest;
        @Element(data = false,required = false)
        private Boolean isShowReplyFlag;
        @Element(data = false,required = false)
        private Integer replyCnt;
        @Element(required = false,data = false)
        private Integer emotionType;
        @Element(required = false,data = false)
        private int specialEffectType;
        @Element(required = false)
        private String plantGrassInfo;
        public static class MinVersion {
            @Element(required = false)
            private String iPhone;
            @Element(required = false)
            private String GPhone;
            @Element(required = false)
            private String GPad;
            @Element(required = false)
            private String iPad;
        }
        @Element(required = false,data = true)
        private MinVersion minVersion;
        @Element(required = false)
        private String btnExtJson;
        @Element(required = false)
        private String btnType;
        @Element(required = false)
        private String mentionedTvid;
        @Element(required = false)
        private String mentionedTitle;
        public String getColor() {
            return color;
        }

        public String getSrc() {
            return src;
        }

        public long getShowTime() {
            return showTime;
        }

        public String getScoreLevel() {
            return scoreLevel;
        }

        public String getContentId() {
            return contentId;
        }

        public String getLikeCount() {
            return likeCount;
        }

        public String getContent() {
            return content;
        }

        public String getParentId() {
            return parentId;
        }

        public String getDissCount() {
            return dissCount;
        }

        public String getScore() {
            return score;
        }

        public String getSpoilerGuess() {
            return spoilerGuess;
        }

        public String getBackground() {
            return background;
        }

        public String getSubType() {
            return subType;
        }

        public String getSpoiler() {
            return spoiler;
        }

        public String getPosition() {
            return position;
        }

        public String getOpacity() {
            return opacity;
        }

        public String getContentType() {
            return contentType;
        }

        public String getHalfScreenShow() {
            return halfScreenShow;
        }

        public String getPlusCount() {
            return plusCount;
        }

        public String getIsShowLike() {
            return isShowLike;
        }

        public UserInfo getUserInfo() {
            return userInfo;
        }

        public int getFont() {
            return font;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public void setShowTime(long showTime) {
            this.showTime = showTime;
        }

        public void setScoreLevel(String scoreLevel) {
            this.scoreLevel = scoreLevel;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public void setLikeCount(String likeCount) {
            this.likeCount = likeCount;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public void setDissCount(String dissCount) {
            this.dissCount = dissCount;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public void setSpoilerGuess(String spoilerGuess) {
            this.spoilerGuess = spoilerGuess;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public void setSubType(String subType) {
            this.subType = subType;
        }

        public void setSpoiler(String spoiler) {
            this.spoiler = spoiler;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public void setOpacity(String opacity) {
            this.opacity = opacity;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setHalfScreenShow(String halfScreenShow) {
            this.halfScreenShow = halfScreenShow;
        }

        public void setFont(int font) {
            this.font = font;
        }

        public void setPlusCount(String plusCount) {
            this.plusCount = plusCount;
        }

        public void setIsShowLike(String isShowLike) {
            this.isShowLike = isShowLike;
        }

        public void setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }
}
