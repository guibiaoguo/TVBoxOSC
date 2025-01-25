package com.github.tvbox.osc.player.danmu;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.bean.Danmu;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.ZLibUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.util.JsonFormat;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Response;

public class DanmuRoute {

    public static final String TAG = DanmuRoute.class.getSimpleName();

    private static Map<String, String> paramMap;

    private DanmuRoute() {

    }

    public static DanmuRoute getInstance() {
        return DanmuRouteHandle.handle;
    }

    public static class DanmuRouteHandle {
        private static DanmuRoute handle = new DanmuRoute();
    }

    public static String route(String path) {
        try {
            Log.d(TAG,path);
            if (path.startsWith("file")) return path;
            if (path.contains("bilibili.com"))
                return DanmuRoute.getInstance().getBiliDanmu(path);
            if (path.contains("iqiyi.com"))
                return DanmuRoute.getInstance().getIQiYiDanmu(path);
            if (path.contains("bullet-ali.hitv.com") || path.contains("bullet-ws.hitv.com") || path.startsWith("https://www.mgtv.com"))
                return DanmuRoute.getInstance().getMGTVDanmu(path);
            if (path.contains("qq.com"))
                return DanmuRoute.getInstance().getQQDanmu(path);
            if (path.contains("youku.com"))
                return DanmuRoute.getInstance().getYouKuDanmu(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public List<String> extraJson(String xml, String tag) {
        Pattern p = Pattern.compile("\""+tag+"\":([^,]+)", Pattern.MULTILINE);
        List<String> data = new ArrayList<>();
        Matcher matcher = p.matcher(xml);
        while (matcher.find()) {
            data.add(matcher.group(1));
        }
        return data;
    }

    public String getBiliDanmu(String path) {
        try {
            if (path.contains("www.bilibili.com")) {
                String[] ids = path.split("/");
                String epid = ids[ids.length - 1].substring(2);
                String content = OkGo.<String>get("https://api.bilibili.com/pgc/view/web/ep/list?ep_id=" + epid).execute().body().string();
                String oid = "", pid = "";
                List<String> epIds = extraJson(content,"ep_id");
                List<String> cids = extraJson(content,"cid");
                List<String> aids = extraJson(content,"aid");
                for (int i = 0; i < epIds.size(); i++) {
                    if (epIds.get(i).equals(epid)) {
                        oid = cids.get(i);
                        pid = aids.get(i);
                        break;
                    }
                }
                path = "https://api.bilibili.com/x/v2/dm/wbi/web/seg.so?oid=" + oid + "&pe=120000&pid=" + pid;
            } else if (path.contains("api.bilibili.com")){
                String oid = getParam(path,"oid");
                String pid = getParam(path,"pid");
                path = "https://api.bilibili.com/x/v2/dm/wbi/web/seg.so?oid=" + oid + "&pe=120000&pid=" + pid;
            }
            File file = FileUtils.getLocal("file://TV/danmu/bilibili_from_" + MD5.string2MD5(path) + ".xml");
            long time = 0;
            if (file !=null && file.exists()) {
                time = System.currentTimeMillis() - file.lastModified();
            }
            if (file.exists() && time<3600*24*1000 && time > 0) {
                return "file://TV/danmu/bilibili_from_" + MD5.string2MD5(path) + ".xml";
            }
            List<String> contentList = new ArrayList<>();
            boolean flag = false;
            double j = 1;
            double i = 0;
            List<Request> failList = new ArrayList<>();
            while (true) {
                List<Request> requestList = new ArrayList<>();
                if (!flag && failList.size() < 5) {
                    for (i = j; i < 5 + j; i = i + 0.5) {
                        StringBuilder builder = new StringBuilder();
                        if (i < 2) {
                            builder.append("oid=").append(getParam(path, "oid")).append("&pe=").append(i == 1.0 ? 120000 : 360000).append("&pid=").append(getParam(path, "pid"));
                            builder.append("&ps=").append(i == 1.0 ? 0 : 120000).append("&pull_mode=1&segment_index=1");
                            builder.append("&type=1&web_location=1315873");
                        } else {
                            i = Math.ceil(i);
                            builder.append("oid=").append(getParam(path, "oid")).append("&pid=").append(getParam(path, "pid"));
                            builder.append("&segment_index=").append((int) i);
                            builder.append("&type=1&web_location=1315873");
                        }
                        builder.append("&wts=" + (System.currentTimeMillis() / 1000));
                        String md5 = MD5.encode(builder + "ea1db124af3c7062474693fa704f4ff8");
                        String url = path.split("\\?")[0] + "?" + builder + "&w_rid=" + md5;
                        requestList.add(new Request(url, new HashMap<>(), new HttpHeaders(), "GET"));
                    }
                }
                requestList.addAll(failList);
                failList = new ArrayList<>();
                Map<Request, byte[]> contentMap = batchFetch(requestList);
                for (Request request : requestList) {
                    if (contentMap.get(request) == null && request.getFailCount() < 4) {
                        request.setFailCount(request.getFailCount()+1);
                        failList.add(request);
                        continue;
                    }
                    String json;
                    try {
                        byte[] bytes = contentMap.get(request);
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        json = "{\n}";
                    }
                    if (json.equals("{\n}")) {
                        flag = true;
                    } else {
                        contentList.add(json);
                    }
                }
                if (flag && failList.isEmpty()) {
                    break;
                }
                j = i;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<i>\n");
            for (String content : contentList) {
                if (content.equals("{\n}"))
                    continue;
                BillDanmu bill = BillDanmu.objectFrom(content);
                for (BillDanmu billDanmu : bill.getDanmus()) {
                    builder.append("<d p=\"");
                    builder.append(billDanmu.getProgress() / 1000.0).append(",");
                    builder.append(billDanmu.getMode()).append(",");
                    builder.append(billDanmu.getFontsize()).append(",");
                    builder.append(billDanmu.getColor()).append(",").append("0");
                    builder.append("\">").append(billDanmu.getContent().replace(">", "&gt;").replace("<", "&lt;").replace("&", "&amp;")).append("</d>\n");
                }
            }
            builder.append("</i>");
            path = "bilibili_from_" + MD5.string2MD5(path) + ".xml";
            path = "file://" + FileUtils.writeDanmu(builder.toString().getBytes(), path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public class Request {
        private String url;
        private Map<String, String> params;
        private HttpHeaders headers;
        private String method;

        private int failCount;

        public Request(String url, Map<String, String> params, HttpHeaders headers, String method) {
            this.url = url;
            this.params = params;
            this.headers = headers;
            this.method = method;
            this.failCount = 0;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public String getMethod() {
            return method;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }

    }

    class Responses {
        private Request request;
        private byte[] content;

        public Responses(Request request, byte[] content) {
            this.request = request;
            this.content = content;
        }

        public Request getRequest() {
            return request;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public Map<Request, byte[]> batchFetch(List<Request> requestList) {
        long start = System.currentTimeMillis();
        int maxThread = Math.min(requestList.size(), 16);
        ExecutorService executorService = new ThreadPoolExecutor(maxThread, maxThread,
                1L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(4096));
        CompletionService<Responses> completionService = new ExecutorCompletionService(executorService);
        Map<Request, byte[]> contentMap = new ConcurrentHashMap<>();
        for (Request request : requestList) {
            completionService.submit(() -> {
                long start1 = System.currentTimeMillis();
                byte[] json = null;
                try {
                    if (request.getMethod().equalsIgnoreCase("GET")) {
                        json = OkGo.<String>get(request.getUrl())
                                .headers(request.getHeaders())
                                .params(request.getParams()).execute().body().bytes();
                    } else if (request.getMethod().equalsIgnoreCase("POST")) {
                        json = OkGo.<String>post(request.getUrl())
                                .headers(request.getHeaders())
                                .params(request.getParams()).execute().body().bytes();
                    }
                    if (request.getUrl().contains("api.bilibili.com")) {
                        BillDanmuEntity.Danmus bill = BillDanmuEntity.Danmus.parseFrom(json);
                        json = JsonFormat.printer().print(bill).getBytes();
                    } else if (request.getUrl().contains("cmts.iqiyi.com")) {
                        json = ZLibUtils.decompress(json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Responses(request, json);
                } finally {
                    long end = System.currentTimeMillis();
                    Log.d(TAG,Thread.currentThread().getName() + " 第" + request.getFailCount() + "次 " +request.getUrl() + " 耗时" + (end-start1) + "毫秒!");
                }
                return new Responses(request, json);
            });
        }
        for (Request request : requestList) {
            try {
                Future<Responses> future = completionService.take();
                Responses responses = future.get(10,TimeUnit.SECONDS);
                contentMap.put(responses.getRequest(),responses.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
        long end = System.currentTimeMillis();
        Log.d(TAG,requestList.size() + "条数据耗时"+(end-start)+"毫秒");
        return contentMap;
    }

    public String getIQiYiDanmu(String path) {
        try {
            if (path.contains("www.iqiyi.com")) {
                String content = OkGo.get("https://mesh.if.iqiyi.com/player/lw/lwplay/accelerator.js?apiVer=2").headers("referer", path).execute().body().string();
                Matcher matcher = Pattern.compile("tvid\":(\\d+),").matcher(content);
                String vid = "";
                if (matcher.find()) {
                    vid = matcher.group(1);
                    path = "https://cmts.iqiyi.com/bullet/" + vid.substring(vid.length() - 4, vid.length() - 2) + "/" + vid.substring(vid.length() - 2) + "/" + vid + "_300_1.z";
                }
            }
            File file = FileUtils.getLocal("file://TV/danmu/iqiyi_from_" + MD5.string2MD5(path) + ".xml");
            long time = 0;
            if (file !=null && file.exists()) {
                time = System.currentTimeMillis() - file.lastModified();
            }
            if (file.exists() && time<3600*24*1000 && time > 0) {
                return "file://TV/danmu/iqiyi_from_" + MD5.string2MD5(path) + ".xml";
            }
            List<String> contentList = new ArrayList<>();
            boolean flag = false;
            int j = 1;
            int i = 0;
            List<Request> failList = new ArrayList<>();
            while (true) {
                List<Request> requestList = new ArrayList<>();
                if (!flag && failList.size() < 5) {
                    for (i = j; i < 5 + j; i++) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(path.replace("_1.z", "")).append("_").append(i).append(".z");
                        requestList.add(new Request(builder.toString(), new HashMap<>(), new HttpHeaders(), "GET"));
                    }
                }
                requestList.addAll(failList);
                failList = new ArrayList<>();
                Map<Request, byte[]> contentMap = batchFetch(requestList);
                for (Request request : requestList) {
                    String json;
                    if (contentMap.get(request) == null && request.getFailCount() < 4) {
                        request.setFailCount(request.getFailCount()+1);
                        failList.add(request);
                        continue;
                    }
                    try {
                        byte[] bytes = contentMap.get(request);
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        json = "";
                        e.printStackTrace();
                    }
                    if (json.startsWith("{\"code\":\"NoSuchKey\"")) {
                        flag = true;
                    } else {
                        contentList.add(json);
                    }
                }
                if (flag && failList.isEmpty()) {
                    break;
                }
                j = i;
            }
            List<String> contents = new ArrayList<>();
            contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<i>\n");
            for (String content : contentList) {
                List<String> textes = extra(content, "content");
                List<String> showTimes = extra(content, "showTime");
                List<String> colors = extra(content, "color");
                List<String> fonts = extra(content, "font");
                for (int k = 0; k < textes.size(); k++) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<d p=\"");
                    builder.append(showTimes.get(k)).append(",");
                    int type = 1;
                    builder.append(type).append(",");
                    String font = "25";
                    if (!TextUtils.isEmpty(fonts.get(k))) {
                        font = fonts.get(k);
                    }
                    builder.append(font).append(",");
                    builder.append(Integer.parseInt(colors.get(k), 16)).append(",").append("0");
                    builder.append("\">").append(textes.get(k).replace(">", "&gt;").replace("<", "&lt;").replace("&", "&amp;")).append("</d>\n");
                    contents.add(builder.toString());
                }
            }
            contents.add("</i>");
            path = "iqiyi_from_" + MD5.string2MD5(path) + ".xml";
            path = "file://" + FileUtils.writeDanmu(TextUtils.join("", contents).getBytes(), path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public List<String> extra(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + ">" + "([^<]+)" + "</" + tag + ">", Pattern.MULTILINE);
        List<String> data = new ArrayList<>();
        Matcher matcher = p.matcher(xml);
        while (matcher.find()) {
            data.add(matcher.group(1));
        }
        return data;
    }

    public String getMGTVDanmu(String path) {
        try {
            if (path.startsWith("https://www.mgtv.com")) {
                String[] ids = path.split("/");
                String vid = ids[ids.length - 1].replace(".html", "");
                String cid = ids[ids.length - 2];
                String danmu = "https://galaxy.bz.mgtv.com/getctlbarrage?version=8.1.43&abroad=0&uuid=&os=10.0&platform=0&deviceid=5b74d471-b244-4ef8-83d5-39c063b93b5d&mac=&vid=" + vid + "&pid=&cid=" + cid + "&ticket=";
                String content = OkGo.get(danmu).execute().body().string();
                JsonObject dataObject = new Gson().fromJson(content, JsonObject.class).getAsJsonObject("data");
                path = "https://" + dataObject.get("cdn_list").getAsString().split(",")[0] + "/" + dataObject.get("cdn_version").getAsString() + "/1.json";
            }
            File file = FileUtils.getLocal("file://TV/danmu/mgtv_from_" + MD5.string2MD5(path) + ".xml");
            long time = 0;
            if (file !=null && file.exists()) {
                time = System.currentTimeMillis() - file.lastModified();
            }
            if (file.exists() && time<3600*24*1000 && time > 0) {
                return "file://TV/danmu/mgtv_from_" + MD5.string2MD5(path) + ".xml";
            }
            List<String> contentList = new ArrayList<>();
            boolean flag = false;
            int j = 1;
            int i = 0;
            List<Request> failList = new ArrayList<>();
            while (true) {
                List<Request> requestList = new ArrayList<>();
                if (!flag && failList.size() < 10) {
                    for (i = j; i < 10 + j; i++) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(path.replace("/1.json", "")).append("/").append(i).append(".json");
                        requestList.add(new Request(builder.toString(), new HashMap<>(), new HttpHeaders(), "GET"));
                    }
                }
                requestList.addAll(failList);
                failList = new ArrayList<>();
                Map<Request, byte[]> contentMap = batchFetch(requestList);
                for (Request request : requestList) {
                    String json;
                    if (contentMap.get(request) == null && request.getFailCount() < 4) {
                        request.setFailCount(request.getFailCount()+1);
                        failList.add(request);
                        continue;
                    }
                    try {
                        byte[] bytes = contentMap.get(request);
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        json = "";
                        e.printStackTrace();
                    }
                    if (json.contains("<Code>NoSuchKey</Code>")) {
                        flag = true;
                    } else {
                        contentList.add(json);
                    }
                }
                if (flag && failList.isEmpty()) {
                    break;
                }
                j = i;
            }
            List<String> contents = new ArrayList<>();
            contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<i>\n");
            for (String content : contentList) {
                MGTVDanmu mgtvDanmu = MGTVDanmu.objectFrom(content);
                for (MGTVDanmu.Danmu danmu : mgtvDanmu.getData().getDanmus()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<d p=\"");
                    builder.append(danmu.getTime() / 1000).append(",");
                    int type = 1;
                    int color = 16777215;
                    if (danmu.getV2Position() != null) {
                        type = 5;
                    }
                    if (danmu.getV2Color() != null) {
                        MGTVDanmu.MGTVDanmuV2ColorLeft colorLeft = danmu.getV2Color().getColorLeft();
                        color = (colorLeft.getR() << 16) + (colorLeft.getG() << 8) + (colorLeft.getB());
                    }
                    builder.append(type).append(",");
                    builder.append(25).append(",");
                    builder.append(color).append(",").append("0");
                    builder.append("\">").append(danmu.getContent().replace(">", "&gt;").replace("<", "&lt;").replace("&", "&amp;")).append("</d>\n");
                    contents.add(builder.toString());
                }
            }
            contents.add("</i>");
            path = "mgtv_from_" + MD5.string2MD5(path) + ".xml";
            path = "file://" + FileUtils.writeDanmu(TextUtils.join("", contents).toString().getBytes(), path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public String getQQDanmu(String path) {
        try {
            if (path.contains("v.qq.com")) {
                String[] ids = path.split("/");
                String vid = ids[ids.length - 1].split(".html")[0];
                path = "https://dm.video.qq.com/barrage/segment/" + vid + "/t/v1/0000/30000";
            }
            File file = FileUtils.getLocal("file://TV/danmu/qq_from_" + MD5.string2MD5(path) + ".xml");
            long time = 0;
            if (file !=null && file.exists()) {
                time = System.currentTimeMillis() - file.lastModified();
            }
            if (file.exists() && time<3600*24*1000 && time > 0) {
                return "file://TV/danmu/qq_from_" + MD5.string2MD5(path) + ".xml";
            }
            List<String> contentList = new ArrayList<>();
            boolean flag = false;
            int j = 1;
            int i = 0;
            List<Request> failList = new ArrayList<>();
            while (true) {
                List<Request> requestList = new ArrayList<>();
                if (!flag && failList.size() < 10) {
                    for (i = j; i < 10 + j; i++) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(path.replace("0000/30000", "")).append(30000 * i).append("/").append(30000 * (i + 1));
                        requestList.add(new Request(builder.toString(), new HashMap<>(), new HttpHeaders(), "GET"));
                    }
                }
                requestList.addAll(failList);
                failList = new ArrayList<>();
                Map<Request, byte[]> contentMap = batchFetch(requestList);
                for (Request request : requestList) {
                    String json;
                    if (contentMap.get(request) == null && request.getFailCount() < 4) {
                        request.setFailCount(request.getFailCount()+1);
                        failList.add(request);
                        continue;
                    }
                    try {
                        byte[] bytes = contentMap.get(request);
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        json = "";
                        e.printStackTrace();
                    }
                    if (json.startsWith("{\"barrage_list\":[]}")) {
                        flag = true;
                    } else {
                        contentList.add(json);
                    }
                }
                if (flag && failList.isEmpty()) {
                    break;
                }
                j = i;
            }
            List<String> contents = new ArrayList<>();
            contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<i>\n");
            for (String content : contentList) {
                QQDanmu qqDanmu = QQDanmu.objectFrom(content);
                for (QQDanmu danmu : qqDanmu.getDanmus()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<d p=\"");
                    builder.append(danmu.getTimeOffset() / 1000).append(",");
                    int type = 1;
                    int color = 16777215;
                    if (danmu.getContentStyle() != null) {
                        type = 5;
                    }
                    if (danmu.getContentStyle() != null) {
                        color = danmu.getContentStyle().getGradientColors() != null ? Integer.parseInt(danmu.getContentStyle().getGradientColors().get(0), 16) : danmu.getContentStyle().getColor() != null ? Integer.parseInt(danmu.getContentStyle().getColor(), 16) : color;
                    }
                    builder.append(type).append(",");
                    builder.append(25).append(",");
                    builder.append(color).append(",").append("0");
                    builder.append("\">").append(danmu.getContent().replace(">", "&gt;").replace("<", "&lt;").replace("&", "&amp;")).append("</d>\n");
                    contents.add(builder.toString());
                }
            }
            contents.add("</i>");
            path = "qq_from_" + MD5.string2MD5(path) + ".xml";
            path = "file://" + FileUtils.writeDanmu(TextUtils.join("", contents).getBytes(), path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    private Map<String, String> cookieManager = new HashMap<>();
    private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public void setCookie(List<String> cookies) {
        for (String cookie : cookies) {
            for (String c : cookie.split(";")) {
                String[] ckey = c.split("=");
                if (ckey.length > 1)
                    cookieManager.put(c.substring(0, c.indexOf("=")), c.substring(c.indexOf("=") + 1));
            }
        }
    }

    public String getYouKuDanmu(String path) {
        List<String> contentList = new ArrayList<>();
        try {
            File file = FileUtils.getLocal("file://TV/danmu/youku_from_" + MD5.string2MD5(path) + ".xml");
            long time = 0;
            if (file !=null && file.exists()) {
                time = System.currentTimeMillis() - file.lastModified();
            }
            if (file.exists() && time<3600*24*1000 && time > 0) {
                return "file://TV/danmu/youku_from_" + MD5.string2MD5(path) + ".xml";
            }
            String[] ids = path.split("/");
            String vid = ids[ids.length - 1].split(".html")[0].substring(3);
            Response response = OkGo.<String>get("https://log.mmstat.com/eg.js").tag("youkudanmu").execute();
            setCookie(response.headers().toMultimap().get("set-cookie"));
            response = OkGo.<String>get("https://acs.youku.com/h5/mtop.com.youku.aplatform.weakget/1.0/?jsv=2.5.1&appKey=24679788").tag("youkudanmu").execute();
            setCookie(response.headers().toMultimap().get("set-cookie"));
            String url = "https://openapi.youku.com/v2/videos/show.json?client_id=53e6cc67237fc59a&video_id=" + vid + "&package=com.huawei.hwvplayer.youku&ext=show";
            String content = OkGo.<String>get(url).tag("youkudanmu").execute().body().string();
            double duration = new Gson().fromJson(content, JsonObject.class).get("duration").getAsDouble();
            double max_mat = Math.floor(duration / 60) + 1;
            boolean flag = false;
            int j = 1;
            int i = 0;
            List<Request> failList = new ArrayList<>();
            while (true) {
                List<Request> requestList = new ArrayList<>();
                if (!flag && failList.size() < 10) {
                    for (i = j; i < 10 + j; i++) {
                        Map<String, Object> msg = new LinkedHashMap<>();
                        msg.put("ctime", System.currentTimeMillis());
                        msg.put("ctype", 10004);
                        msg.put("cver", "v1.0");
                        msg.put("guid", cookieManager.get("cna"));
                        msg.put("mat", i);
                        msg.put("mcount", 1);
                        msg.put("pid", 0);
                        msg.put("sver", "3.1.0");
                        msg.put("type", 1);
                        msg.put("vid", vid);
                        String msg_b64encode = Base64.encodeToString(gson.toJson(msg).getBytes(), 2);
                        msg.put("msg", msg_b64encode);
                        String sign = MD5.encode(msg_b64encode + "MkmC9SoIw6xCkSKHhJ7b5D2r51kBiREr");
                        msg.put("sign", sign);
                        long t = System.currentTimeMillis();
                        List<String> p = new ArrayList<>();
                        String data = gson.toJson(msg);
                        p.add(cookieManager.get("_m_h5_tk").substring(0, 32));
                        p.add(String.valueOf(t));
                        p.add("24679788");
                        p.add(data);
                        String signMd5 = MD5.encode(TextUtils.join("&", p));
                        url = "https://acs.youku.com/h5/mopen.youku.danmu.list/1.0/?t=" + t + "&v=1.0&dataType=jsonp&sign=" + signMd5 + "&appKey=24679788&api=mopen.youku.danmu.list&jsv=2.5.6&type=originaljson&jsonpIncPrefix=utility&timeout=20000";
                        Map<String, String> params = new HashMap<>();
                        params.put("data", data);
                        HttpHeaders headers = new HttpHeaders();
                        headers.put("Cookie", "_m_h5_tk=" + cookieManager.get("_m_h5_tk") + ";_m_h5_tk_enc=" + cookieManager.get("_m_h5_tk_enc") + ";");
                        headers.put("Referer", "https://v.youku.com");
                        headers.put("Content-Type", "application/x-www-form-urlencoded");
                        requestList.add(new Request(url, params, headers, "POST"));
                    }
                }
                requestList.addAll(failList);
                failList = new ArrayList<>();
                Map<Request, byte[]> contentMap = batchFetch(requestList);
                for (Request request : requestList) {
                    String json;
                    if (contentMap.get(request) == null && request.getFailCount() < 4) {
                        request.setFailCount(request.getFailCount()+1);
                        failList.add(request);
                        continue;
                    }
                    try {
                        byte[] bytes = contentMap.get(request);
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        json = "";
                        e.printStackTrace();
                    }
                    if (json.startsWith("{\"code\":\"NoSuchKey\"")) {
                        flag = true;
                    } else {
                        contentList.add(json);
                    }
                }
                if (flag && failList.isEmpty() || i >= max_mat) {
                    break;
                }
                j = i;
            }
            List<String> contents = new ArrayList<>();
            contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<i>\n");
            for (String content1 : contentList) {
                YoukuDanmu qqDanmu = YoukuDanmu.objectFrom(content1);
                for (YoukuDanmu.Danmu danmu : qqDanmu.getData().getResult()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<d p=\"");
                    builder.append(danmu.getPlayat() / 1000).append(",");
                    int type = 1;
                    int color = 16777215;
                    JsonObject propertis = gson.fromJson(danmu.getPropertis(), JsonObject.class);
                    if (propertis.has("color")) {
                        type = 5;
                        color = propertis.get("color").getAsInt();
                    }
                    builder.append(type).append(",");
                    builder.append(25).append(",");
                    builder.append(color).append(",").append("0");
                    builder.append("\">").append(danmu.getContent().replace(">", "&gt;").replace("<", "&lt;").replace("&", "&amp;")).append("</d>\n");
                    contents.add(builder.toString());
                }
            }
            contents.add("</i>");
            path = "youku_from_" + MD5.string2MD5(path) + ".xml";
            path = "file://" + FileUtils.writeDanmu(TextUtils.join("", contents).getBytes(), path);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String getParam(String path, String key) {
        String[] paths = path.split("\\?");
        Map<String, String> params = new HashMap<>();
        for (String p : paths[1].split("&")) {
            params.put(p.split("=")[0], p.split("=")[1]);
        }
        return params.get(key);
    }
}
