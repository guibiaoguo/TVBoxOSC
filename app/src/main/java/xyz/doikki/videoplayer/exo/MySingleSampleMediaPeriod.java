package xyz.doikki.videoplayer.exo;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceUtil;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.StatsDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class MySingleSampleMediaPeriod implements MediaPeriod, Loader.Callback<MySingleSampleMediaPeriod.SourceLoadable>{


    private static final String TAG = "SingleSampleMediaPeriod";

    /** The initial size of the allocation used to hold the sample data. */
    private static final int INITIAL_SAMPLE_SIZE = 1024;

    private final DataSpec dataSpec;
    private final DataSource.Factory dataSourceFactory;
    @Nullable
    private final TransferListener transferListener;
    private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private final MediaSourceEventListener.EventDispatcher eventDispatcher;
    private final TrackGroupArray tracks;
    private final ArrayList<MySingleSampleMediaPeriod.SampleStreamImpl> sampleStreams;
    private final long durationUs;

    // Package private to avoid thunk methods.
    /* package */ final Loader loader;
    /* package */ final Format format;
    /* package */ final boolean treatLoadErrorsAsEndOfStream;

    /* package */ boolean loadingFinished;
    /* package */ byte[] sampleData;
    /* package */ int sampleSize;

    public MySingleSampleMediaPeriod(
            DataSpec dataSpec,
            DataSource.Factory dataSourceFactory,
            @Nullable TransferListener transferListener,
            Format format,
            long durationUs,
            LoadErrorHandlingPolicy loadErrorHandlingPolicy,
            MediaSourceEventListener.EventDispatcher eventDispatcher,
            boolean treatLoadErrorsAsEndOfStream) {
        this.dataSpec = dataSpec;
        this.dataSourceFactory = dataSourceFactory;
        this.transferListener = transferListener;
        this.format = format;
        this.durationUs = durationUs;
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
        this.eventDispatcher = eventDispatcher;
        this.treatLoadErrorsAsEndOfStream = treatLoadErrorsAsEndOfStream;
        tracks = new TrackGroupArray(new TrackGroup(format));
        sampleStreams = new ArrayList<>();
        loader = new Loader("SingleSampleMediaPeriod");
    }

    public void release() {
        loader.release();
    }

    @Override
    public void prepare(Callback callback, long positionUs) {
        callback.onPrepared(this);
    }

    @Override
    public void maybeThrowPrepareError() {
        // Do nothing.
    }

    @Override
    public TrackGroupArray getTrackGroups() {
        return tracks;
    }

    @Override
    public long selectTracks(
            ExoTrackSelection[] selections,
            boolean[] mayRetainStreamFlags,
            SampleStream[] streams,
            boolean[] streamResetFlags,
            long positionUs) {
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                sampleStreams.remove(streams[i]);
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                MySingleSampleMediaPeriod.SampleStreamImpl stream = new MySingleSampleMediaPeriod.SampleStreamImpl();
                sampleStreams.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    @Override
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        // Do nothing.
    }

    @Override
    public void reevaluateBuffer(long positionUs) {
        // Do nothing.
    }

    @Override
    public boolean continueLoading(long positionUs) {
        if (loadingFinished || loader.isLoading() || loader.hasFatalError()) {
            return false;
        }
        DataSource dataSource = dataSourceFactory.createDataSource();
        if (transferListener != null) {
            dataSource.addTransferListener(transferListener);
        }
        MySingleSampleMediaPeriod.SourceLoadable loadable = new MySingleSampleMediaPeriod.SourceLoadable(dataSpec, dataSource);
        long elapsedRealtimeMs =
                loader.startLoading(
                        loadable,
                        /* callback= */ this,
                        loadErrorHandlingPolicy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA));
        eventDispatcher.loadStarted(
                new LoadEventInfo(loadable.loadTaskId, dataSpec, elapsedRealtimeMs),
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs);
        return true;
    }

    @Override
    public boolean isLoading() {
        return loader.isLoading();
    }

    @Override
    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    @Override
    public long getNextLoadPositionUs() {
        return loadingFinished || loader.isLoading() ? C.TIME_END_OF_SOURCE : 0;
    }

    @Override
    public long getBufferedPositionUs() {
        return loadingFinished ? C.TIME_END_OF_SOURCE : 0;
    }

    @Override
    public long seekToUs(long positionUs) {
        for (int i = 0; i < sampleStreams.size(); i++) {
            sampleStreams.get(i).reset();
        }
        return positionUs;
    }

    @Override
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return positionUs;
    }

    // Loader.Callback implementation.

    @Override
    public void onLoadCompleted(
            MySingleSampleMediaPeriod.SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        sampleSize = (int) loadable.dataSource.getBytesRead();
        sampleData = Assertions.checkNotNull(loadable.sampleData);
        if (sampleData != null)
            sampleSize = sampleData.length;
        loadingFinished = true;
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo =
                new LoadEventInfo(
                        loadable.loadTaskId,
                        loadable.dataSpec,
                        dataSource.getLastOpenedUri(),
                        dataSource.getLastResponseHeaders(),
                        elapsedRealtimeMs,
                        loadDurationMs,
                        sampleSize);
        loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        eventDispatcher.loadCompleted(
                loadEventInfo,
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs);
    }

    @Override
    public void onLoadCanceled(
            MySingleSampleMediaPeriod.SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo =
                new LoadEventInfo(
                        loadable.loadTaskId,
                        loadable.dataSpec,
                        dataSource.getLastOpenedUri(),
                        dataSource.getLastResponseHeaders(),
                        elapsedRealtimeMs,
                        loadDurationMs,
                        dataSource.getBytesRead());
        loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        eventDispatcher.loadCanceled(
                loadEventInfo,
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                /* trackFormat= */ null,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs);
    }

    @Override
    public Loader.LoadErrorAction onLoadError(
            MySingleSampleMediaPeriod.SourceLoadable loadable,
            long elapsedRealtimeMs,
            long loadDurationMs,
            IOException error,
            int errorCount) {
        StatsDataSource dataSource = loadable.dataSource;
        LoadEventInfo loadEventInfo =
                new LoadEventInfo(
                        loadable.loadTaskId,
                        loadable.dataSpec,
                        dataSource.getLastOpenedUri(),
                        dataSource.getLastResponseHeaders(),
                        elapsedRealtimeMs,
                        loadDurationMs,
                        dataSource.getBytesRead());
        MediaLoadData mediaLoadData =
                new MediaLoadData(
                        C.DATA_TYPE_MEDIA,
                        C.TRACK_TYPE_UNKNOWN,
                        format,
                        C.SELECTION_REASON_UNKNOWN,
                        /* trackSelectionData= */ null,
                        /* mediaStartTimeMs= */ 0,
                        Util.usToMs(durationUs));
        long retryDelay =
                loadErrorHandlingPolicy.getRetryDelayMsFor(
                        new LoadErrorHandlingPolicy.LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
        boolean errorCanBePropagated =
                retryDelay == C.TIME_UNSET
                        || errorCount
                        >= loadErrorHandlingPolicy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA);

        Loader.LoadErrorAction action;
        if (treatLoadErrorsAsEndOfStream && errorCanBePropagated) {
            Log.w(TAG, "Loading failed, treating as end-of-stream.", error);
            loadingFinished = true;
            action = Loader.DONT_RETRY;
        } else {
            action =
                    retryDelay != C.TIME_UNSET
                            ? Loader.createRetryAction(/* resetErrorCount= */ false, retryDelay)
                            : Loader.DONT_RETRY_FATAL;
        }
        boolean wasCanceled = !action.isRetry();
        eventDispatcher.loadError(
                loadEventInfo,
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                error,
                wasCanceled);
        if (wasCanceled) {
            loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
        }
        return action;
    }

    private final class SampleStreamImpl implements SampleStream {

        private static final int STREAM_STATE_SEND_FORMAT = 0;
        private static final int STREAM_STATE_SEND_SAMPLE = 1;
        private static final int STREAM_STATE_END_OF_STREAM = 2;

        private int streamState;
        private boolean notifiedDownstreamFormat;

        public void reset() {
            if (streamState == STREAM_STATE_END_OF_STREAM) {
                streamState = STREAM_STATE_SEND_SAMPLE;
            }
        }

        @Override
        public boolean isReady() {
            return loadingFinished;
        }

        @Override
        public void maybeThrowError() throws IOException {
            if (!treatLoadErrorsAsEndOfStream) {
                loader.maybeThrowError();
            }
        }

        @Override
        public int readData(
                FormatHolder formatHolder, DecoderInputBuffer buffer, @ReadFlags int readFlags) {
            maybeNotifyDownstreamFormat();
            if (loadingFinished && sampleData == null) {
                streamState = STREAM_STATE_END_OF_STREAM;
            }

            if (streamState == STREAM_STATE_END_OF_STREAM) {
                buffer.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
                return C.RESULT_BUFFER_READ;
            }

            if ((readFlags & FLAG_REQUIRE_FORMAT) != 0 || streamState == STREAM_STATE_SEND_FORMAT) {
                formatHolder.format = format;
                streamState = STREAM_STATE_SEND_SAMPLE;
                return C.RESULT_FORMAT_READ;
            }

            if (!loadingFinished) {
                return C.RESULT_NOTHING_READ;
            }
            Assertions.checkNotNull(sampleData);

            buffer.addFlag(C.BUFFER_FLAG_KEY_FRAME);
            buffer.timeUs = 0;
            if ((readFlags & FLAG_OMIT_SAMPLE_DATA) == 0) {
                buffer.ensureSpaceForWrite(sampleSize);
                buffer.data.put(sampleData, 0, sampleSize);
            }
            if ((readFlags & FLAG_PEEK) == 0) {
                streamState = STREAM_STATE_END_OF_STREAM;
            }
            return C.RESULT_BUFFER_READ;
        }

        @Override
        public int skipData(long positionUs) {
            maybeNotifyDownstreamFormat();
            if (positionUs > 0 && streamState != STREAM_STATE_END_OF_STREAM) {
                streamState = STREAM_STATE_END_OF_STREAM;
                return 1;
            }
            return 0;
        }

        private void maybeNotifyDownstreamFormat() {
            if (!notifiedDownstreamFormat) {
                eventDispatcher.downstreamFormatChanged(
                        MimeTypes.getTrackType(format.sampleMimeType),
                        format,
                        C.SELECTION_REASON_UNKNOWN,
                        /* trackSelectionData= */ null,
                        /* mediaTimeUs= */ 0);
                notifiedDownstreamFormat = true;
            }
        }
    }

    /* package */ static final class SourceLoadable implements Loader.Loadable {

        public final long loadTaskId;
        public final DataSpec dataSpec;

        private final StatsDataSource dataSource;

        @Nullable private byte[] sampleData;

        public SourceLoadable(DataSpec dataSpec, DataSource dataSource) {
            this.loadTaskId = LoadEventInfo.getNewId();
            this.dataSpec = dataSpec;
            this.dataSource = new StatsDataSource(dataSource);
        }

        @Override
        public void cancelLoad() {
            // Never happens.
        }

        @Override
        public void load() throws IOException {
            // We always load from the beginning, so reset bytesRead to 0.
            dataSource.resetBytesRead();
            try {
                // Create and open the input.
                dataSource.open(dataSpec);
                // Load the sample data.
                int result = 0;
                while (result != C.RESULT_END_OF_INPUT) {
                    int sampleSize = (int) dataSource.getBytesRead();
                    if (sampleData == null) {
                        sampleData = new byte[INITIAL_SAMPLE_SIZE];
                    } else if (sampleSize == sampleData.length) {
                        sampleData = Arrays.copyOf(sampleData, sampleData.length * 2);
                    }
                    result = dataSource.read(sampleData, sampleSize, sampleData.length - sampleSize);
                }
            } finally {
                DataSourceUtil.closeQuietly(dataSource);
                if (sampleData != null) {
                    UniversalDetector detector = new UniversalDetector(null);
                    detector.handleData(sampleData, 0, sampleData.length);
                    detector.dataEnd();
                    String encoding = detector.getDetectedCharset();
                    sampleData = new String(sampleData,encoding).getBytes(StandardCharsets.UTF_8);
                }
//                    sampleData = new String(sampleData,getTxtEncode(Arrays.copyOfRange(sampleData,0,3))).getBytes("UTF-8");
            }
        }

        public String getTxtEncode(byte[] head) {

            String code = "GBK";
            if (head[0] == -1 && head[1] == -2 )
                code = "UTF-16";
            if (head[0] == -2 && head[1] == -1 )
                code = "Unicode";
            //带BOM
            if(head[0]==-17 && head[1]==-69 && head[2] ==-65)
                code = "UTF-8";
            if("Unicode".equals(code)){
                code = "UTF-16";
            }
            return code;
        }

    }
}
