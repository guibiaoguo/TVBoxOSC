package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Sub;
import com.github.tvbox.osc.bean.SubtitleBean;
import com.github.tvbox.osc.bean.SubtitleData;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.SearchSubtitleAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.viewmodel.SubtitleViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class SearchDriveSubtitleDialog extends BaseDialog {

    private final Context mContext;
    private TvRecyclerView mGridView;
    private SearchSubtitleAdapter searchAdapter;

    private TextView subtitleSearchBtn;
    private EditText subtitleSearchEt;
    private SubtitleLoader mSubtitleLoader;
    private ProgressBar loadingBar;
    private SubtitleViewModel subtitleViewModel;
    private int page = 1;
    private final int maxPage = 5;
    private String searchWord = "";

    private List<SubtitleBean> zipSubtitles = new ArrayList<>();
    private boolean isSearchPag = true;

    private List<SubtitleBean> subtitles;

    public void setSubtitles(JSONArray subtitles) {
        this.subtitles = new ArrayList<>();
        for (int i = 0; i < subtitles.length(); i++) {
            SubtitleBean bean = new SubtitleBean();
            bean.setUrl(subtitles.optJSONObject(i).optString("url"));
            bean.setName(subtitles.optJSONObject(i).optString("name"));
            bean.setIsZip(false);
            this.subtitles.add(bean);
        }
    }

    public SearchDriveSubtitleDialog(@NonNull @NotNull Context context) {
        super(context);
        mContext = context;
        if (context instanceof Activity) {
            setOwnerActivity((Activity) context);
        }
        setContentView(R.layout.dialog_search_subtitle);
        initView(context);
        initViewModel();
    }

    protected void initView(Context context) {
        loadingBar = findViewById(R.id.loadingBar);
        mGridView = findViewById(R.id.mGridView);
        subtitleSearchEt = findViewById(R.id.input_sub);
        subtitleSearchBtn = findViewById(R.id.inputSubmit);
        subtitleSearchBtn.setText(HomeActivity.getRes().getString(R.string.vod_sub_search));

        searchAdapter = new SearchSubtitleAdapter();
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 1, false));
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                SubtitleBean subtitle = searchAdapter.getData().get(position);
                //加载字幕
                if (mSubtitleLoader != null) {
                    if (subtitle.getIsZip()) {
                        isSearchPag = false;
                        loadingBar.setVisibility(View.VISIBLE);
                        mGridView.setVisibility(View.GONE);
                        subtitleViewModel.getSearchResultSubtitleUrls(subtitle);
                    } else {
                        loadSubtitle(subtitle);
                        dismiss();
                    }
                }
            }
        });

        searchAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                if (searchAdapter.getData().get(0).getIsZip()) {
                    subtitleViewModel.searchResult(searchWord, page);
                }
            }
        }, mGridView);

        // takagen99 : Fix on Key Enter
        subtitleSearchEt.setOnKeyListener(onSoftKeyPress);

        subtitleSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                String wd = subtitleSearchEt.getText().toString().trim();
                search(wd);
            }
        });
        searchAdapter.setNewData(new ArrayList<>());
    }

    // takagen99 : Fix on Key Enter
    private final View.OnKeyListener onSoftKeyPress = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                // hide soft keyboard, set focus on next button
                subtitleSearchEt.clearFocus();
                subtitleSearchBtn.requestFocus();
            }
            return false;
        }
    };

    public void setSearchWord(String wd) {
        wd = wd.replaceAll("(?:（|\\(|\\[|【|\\.mp4|\\.mkv|\\.avi|\\.MP4|\\.MKV|\\.AVI)", "");
        wd = wd.replaceAll("(?:：|\\:|）|\\)|\\]|】|\\.)", " ");
        int len = wd.length();
        int finalLen = len >= 36 ? 36 : len;
        wd = wd.substring(0, finalLen).trim();
        subtitleSearchEt.setText(wd);
        subtitleSearchEt.setSelection(wd.length());
        subtitleSearchEt.requestFocus();
    }

    public void search(String wd) {
        isSearchPag = true;
        searchAdapter.setNewData(new ArrayList<>());
        SubtitleData subtitleData = new SubtitleData();
        if (!TextUtils.isEmpty(wd)) {
            List<SubtitleBean> subtitleList = new ArrayList<>();
            for (SubtitleBean bean:subtitles) {
                if (bean.getName().contains(wd)) {
                    subtitleList.add(bean);
                }
            }
            subtitleData.setSubtitleList(subtitleList);
        } else {
            subtitleData.setSubtitleList(subtitles);
        }
        subtitleData.setIsNew(true);
        subtitleData.setIsZip(false);
        subtitleViewModel.searchResult.postValue(subtitleData);
    }

    private void initViewModel() {
        subtitleViewModel = new ViewModelProvider((ViewModelStoreOwner) mContext).get(SubtitleViewModel.class);
        subtitleViewModel.searchResult.observe((LifecycleOwner) mContext, new Observer<SubtitleData>() {
            @Override
            public void onChanged(SubtitleData subtitleData) {
                List<SubtitleBean> data = subtitleData.getSubtitleList();
                loadingBar.setVisibility(View.GONE);
                mGridView.setVisibility(View.VISIBLE);
                if (data == null) {
                    mGridView.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "未查询到匹配字幕", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                if (data.size() > 0) {
                    mGridView.requestFocus();
                    if (subtitleData.getIsZip()) {
                        if (subtitleData.getIsNew()) {
                            searchAdapter.setNewData(data);
                            zipSubtitles = data;
                        } else {
                            searchAdapter.addData(data);
                            zipSubtitles.addAll(data);
                        }
                        page++;
                        if (page > maxPage) {
                            searchAdapter.loadMoreEnd();
                            searchAdapter.setEnableLoadMore(false);
                        } else {
                            searchAdapter.loadMoreComplete();
                            searchAdapter.setEnableLoadMore(true);
                        }
                    } else {
                        searchAdapter.loadMoreComplete();
                        searchAdapter.setNewData(data);
                        searchAdapter.setEnableLoadMore(false);
                    }
                } else {
                    if (page > maxPage) {
                        searchAdapter.loadMoreEnd();
                    } else {
                        searchAdapter.loadMoreComplete();
                    }
                    searchAdapter.setEnableLoadMore(false);
                }

            }
        });
    }

    private void loadSubtitle(SubtitleBean subtitle) {
//        subtitleViewModel.getSubtitleUrl(subtitle, mSubtitleLoader);
    }

    public void setSubtitleLoader(SubtitleLoader subtitleLoader) {
        mSubtitleLoader = subtitleLoader;
    }

    public interface SubtitleLoader {
        void loadSubtitle(SubtitleBean subtitle);
    }

    @Override
    public void onBackPressed() {
        if (!isSearchPag) {
            isSearchPag = true;
            loadingBar.setVisibility(View.GONE);
            mGridView.setVisibility(View.VISIBLE);
            searchAdapter.setNewData(zipSubtitles);
            searchAdapter.setEnableLoadMore(page < maxPage);
            return;
        }
        dismiss();
    }

}