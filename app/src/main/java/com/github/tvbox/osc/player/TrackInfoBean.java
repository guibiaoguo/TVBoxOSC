package com.github.tvbox.osc.player;

import java.util.HashMap;

public class TrackInfoBean {
    public int trackId;
    //渲染器ID（exo）
    public int renderId;
    //分组ID（exo）
    public int trackGroupId;

    public String name;
    public String url;
    public HashMap<String, String> header;
    public String language;
    public boolean selected;
}
