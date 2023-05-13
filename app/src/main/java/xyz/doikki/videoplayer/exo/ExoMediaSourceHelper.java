package xyz.doikki.videoplayer.exo;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

public final class ExoMediaSourceHelper {

    private static volatile ExoMediaSourceHelper sInstance;

    private final String mUserAgent;
    private final Context mAppContext;
    private HttpDataSource.Factory mHttpDataSourceFactory;
    private Cache mCache;

    private ExoMediaSourceHelper(Context context) {
        mAppContext = context.getApplicationContext();
        mUserAgent = Util.getUserAgent(mAppContext, mAppContext.getApplicationInfo().name);
    }

    public static ExoMediaSourceHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ExoMediaSourceHelper.class) {
                if (sInstance == null) {
                    sInstance = new ExoMediaSourceHelper(context);
                }
            }
        }
        return sInstance;
    }

    public MediaSource getMediaSource(String uri) {
        return getMediaSource(uri, null, false);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers) {
        return getMediaSource(uri, headers, false);
    }

    public MediaSource getMediaSource(String uri, boolean isCache) {
        return getMediaSource(uri, null, isCache);
    }

    public MediaSource getMediaSource(String uri, Map<String, String> headers, boolean isCache) {
        Uri contentUri = Uri.parse(uri);
        if ("rtmp".equals(contentUri.getScheme())) {
            return new ProgressiveMediaSource.Factory(new RtmpDataSource.Factory())
                    .createMediaSource(MediaItem.fromUri(contentUri));
        } else if ("rtsp".equals(contentUri.getScheme())) {
            return new RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(contentUri));
        }
        int contentType = inferContentType(uri);
        DataSource.Factory factory;
        if (isCache) {
            factory = getCacheDataSourceFactory();
        } else {
            factory = getDataSourceFactory();
        }
        if (mHttpDataSourceFactory != null) {
            setHeaders(headers);
        }
        MediaSource videoSource = null;
        switch (contentType) {
            case C.CONTENT_TYPE_DASH:
                videoSource =  new DashMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
                break;
            case C.CONTENT_TYPE_HLS:
                videoSource = new HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
                break;
            default:
            case C.CONTENT_TYPE_OTHER:
                // Build the video MediaSource.
//                MediaItem.SubtitleConfiguration subtitle =
//                        new MediaItem.SubtitleConfiguration.Builder(Uri.parse("https://file0.assrt.net/onthefly/678863/-/1/Agents%20of%20S.H.I.E.L.D%20S07E13%20What%20We're%20Fighting%20For.%E7%AE%80%E8%8B%B1.ass?_=1663175883&-=841669064ebfca68bbad9a53bfb3b205"))
//                                .setMimeType(MimeTypes.TEXT_VTT) // The correct MIME type (required).
//                                .setLanguage("zh") // The subtitle language (optional).
//                                .setSelectionFlags(0) // Selection flags for the track (optional).
//                                .build();
//                MediaItem mediaItem =
//                        new MediaItem.Builder()
//                                .setUri(contentUri)
//                                .setSubtitleConfigurations(ImmutableList.of(subtitle))
//                                .build();
//                MediaSource videoSource =
//                        new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
                videoSource =  new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(contentUri));
                break;
//                MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(Uri.parse("https://dash.akamaized.net/akamai/test/caption_test/ElephantsDream/ElephantsDream_en.vtt")).build();
//                MediaSource subtitleSource =
//                        new SingleSampleMediaSource.Factory(factory)
//        .createMediaSource(subtitleConfiguration, C.TIME_UNSET);
// Plays the video with the sideloaded subtitle.
        }
        return videoSource;
    }

    public MediaSource getTextSource(String url, String name, Map<String, String> headers) {
        return getTextSource(url, name, headers,false);
    }

    public MediaSource getTextSource(String url, String name, Map<String, String> headers,boolean isCache) {

        mHttpDataSourceFactory = null;

        String mimeType = MimeTypes.APPLICATION_SUBRIP;
        if (name.toLowerCase().endsWith(".ssa")) {
            mimeType = MimeTypes.TEXT_SSA;
        }
        else if (name.toLowerCase().endsWith(".vtt")) {
            mimeType = MimeTypes.TEXT_VTT;
        }
        else if (name.toLowerCase().endsWith(".ass")) {
            mimeType = MimeTypes.TEXT_SSA;
        }
        Format textFormat = new Format.Builder()
                /// 其他的比如 text/x-ssa ，text/vtt，application/ttml+xml 等等
                .setSampleMimeType(mimeType)
                .setSelectionFlags(C.SELECTION_FLAG_FORCED)
                /// 如果出现字幕不显示，可以通过修改这个语音去对应，
                //  这个问题在内部的 selectTextTrack 时，TextTrackScore 通过 getFormatLanguageScore 方法判断语言获取匹配不上
                //  就会不出现字幕
                .setLanguage("zh-cn")
                .build();

        MediaItem.SubtitleConfiguration  subtitle = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                .setMimeType(checkNotNull(textFormat.sampleMimeType))
                .setLanguage( textFormat.language)
                .setSelectionFlags(textFormat.selectionFlags).build();

        DataSource.Factory factory;
        if (isCache) {
            factory = getCacheDataSourceFactory();
        } else {
            factory = getDataSourceFactory();
        }
        if (mHttpDataSourceFactory != null) {
//            headers.put("Accept-Encoding","gzip, deflate, b");
//            headers.put("ContentType","UTF-8");
            setHeaders(headers);
        }

//        DefaultHttpDataSource.Factory  factory = new DefaultHttpDataSource.Factory()
//                .setAllowCrossProtocolRedirects(true)
//                .setConnectTimeoutMs(50000)
//                .setReadTimeoutMs(50000)
//                .setTransferListener( new DefaultBandwidthMeter.Builder(mAppContext).build());

        MediaSource textMediaSource = new MySingleSampleMediaSource.Factory(new DefaultDataSource.Factory(mAppContext,
                factory))
                .createMediaSource(subtitle, C.TIME_UNSET);
        return textMediaSource;

    }

    private int inferContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.contains(".mpd")) {
            return C.CONTENT_TYPE_DASH;
        } else if (fileName.contains(".m3u8")) {
            return C.CONTENT_TYPE_HLS;
        } else {
            return C.CONTENT_TYPE_OTHER;
        }
    }

    private DataSource.Factory getCacheDataSourceFactory() {
        if (mCache == null) {
            mCache = newCache();
        }
        return new CacheDataSource.Factory()
                .setCache(mCache)
                .setUpstreamDataSourceFactory(getDataSourceFactory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private Cache newCache() {
        return new SimpleCache(
                new File(mAppContext.getExternalCacheDir(), "exo-video-cache"),//缓存目录
                new LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024),//缓存大小，默认512M，使用LRU算法实现
                new StandaloneDatabaseProvider(mAppContext));
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private DataSource.Factory getDataSourceFactory() {
        return new DefaultDataSource.Factory(mAppContext, getHttpDataSourceFactory());
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @return A new HttpDataSource factory.
     */
    private DataSource.Factory getHttpDataSourceFactory() {
        if (mHttpDataSourceFactory == null) {
            mHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent(mUserAgent)
                    .setAllowCrossProtocolRedirects(true);
        }
        return mHttpDataSourceFactory;
    }

    private void setHeaders(Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            //如果发现用户通过header传递了UA，则强行将HttpDataSourceFactory里面的userAgent字段替换成用户的
            if (headers.containsKey("User-Agent")) {
                String value = headers.remove("User-Agent");
                if (!TextUtils.isEmpty(value)) {
                    try {
                        Field userAgentField = mHttpDataSourceFactory.getClass().getDeclaredField("userAgent");
                        userAgentField.setAccessible(true);
                        userAgentField.set(mHttpDataSourceFactory, value);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
            mHttpDataSourceFactory.setDefaultRequestProperties(headers);
        }
    }

    public void setCache(Cache cache) {
        this.mCache = cache;
    }
}
