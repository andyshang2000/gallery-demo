package com.zz.gallery;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.jpeng.jptabbar.JPTabBar;
import com.jpeng.jptabbar.OnTabSelectListener;
import com.jpeng.jptabbar.anno.NorIcons;
import com.jpeng.jptabbar.anno.SeleIcons;
import com.jpeng.jptabbar.anno.Titles;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.qq.e.ads.banner.ADSize;
import com.qq.e.ads.banner.AbstractBannerADListener;
import com.qq.e.ads.banner.BannerView;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.comm.util.AdError;
import com.youth.banner.Banner;
import com.youth.banner.listener.OnBannerListener;
import com.youth.banner.loader.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends BaseActivity
        implements FullScreenImageGalleryActivity.FullScreenImageLoader, NativeExpressAD.NativeExpressADListener {

    private final String static_server = "http://192.168.0.103:8081/images/";
    private String server = "192.168.0.103";
    private String port = "50001";

    private static final String TAG = "MainActivity";;
    private XRecyclerView recyclerView;
    private ArrayList<Object> mDataList = new ArrayList<>();
    private int pageIndex = 1;
    private int pageSize = 5;
    private GridListAdapter mAdapter;
    private String cat = "";
    private int currentCat;
    private AsyncHttpClient client = null;
    private Handler handler;
    ImageView fab;
    //广告
    private NativeExpressAD mADManager;
    private List<NativeExpressADView> mAdViewList;
    private HashMap<NativeExpressADView, Integer> mAdViewPositionMap = new HashMap<NativeExpressADView, Integer>();
    public static final int AD_COUNT = 6;    // 加载广告的条数，取值范围为[1, 10]
    public static int FIRST_AD_POSITION = 3; // 第一条广告的位置
    public static int ITEMS_PER_AD = 4;     // 每间隔10个条目插入一条广告

    public int firstAD = FIRST_AD_POSITION;
    private BannerView bv;
    private ViewGroup bannerContainer;
    //*/广告
    @Titles
    private static final String[] mTitles = {"新鲜事", "搞笑GIF", "幽默笑话", "奇闻怪事"};
    @SeleIcons
    private static final int[] mSeleIcons = {R.drawable.ic_menu_share, R.drawable.ic_menu_manage, R.drawable.ic_menu_gallery, R.drawable.ic_menu_camera};
    @NorIcons
    private static final int[] mNormalIcons = {R.drawable.ic_menu_share, R.drawable.ic_menu_manage, R.drawable.ic_menu_gallery, R.drawable.ic_menu_camera};
    private boolean bannerShown;
    private boolean bannerReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setupHeader();
        setupTabbar();

        client = new AsyncHttpClient();
        handler = new Handler();
        currentCat = R.id.cat_all;
        setupFloatActionButton();

        FullScreenImageGalleryActivity.setFullScreenImageLoader(this);

        createGridOn(R.id.rv);
        BaseActivity.requestRuntimePermission(
                new String[]{Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new PermissionListener() {

                    @Override
                    public void onGranted() {
                        Log.d(TAG, "init GDT");
                        initAD();
                        doCloseBanner();
                        refresh();
                    }

                    @Override
                    public void onDenied(List<String> deniedPermission) {
                        Log.d(TAG, String.valueOf(deniedPermission));
                        refresh();
                    }
                });
    }

    private void setupTabbar() {
        final JPTabBar mTabbar = (JPTabBar) findViewById(R.id.tabbar);
        mTabbar.setTabListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int index) {
                if (index != currentCat) {
                    currentCat = index;
                    cat = mTitles[index];
                    refresh();
                }

            }
        });
    }

    private void initBanner() {
        this.bv = new BannerView(this, ADSize.BANNER, Constants.APPID, Constants.BannerPosID);
        bannerContainer = (ViewGroup) findViewById(R.id.bannerContainer);
        bannerContainer.addView(bv);
        // 注意：如果开发者的banner不是始终展示在屏幕中的话，请关闭自动刷新，否则将导致曝光率过低。
        // 并且应该自行处理：当banner广告区域出现在屏幕后，再手动loadAD。
        bv.setADListener(new AbstractBannerADListener() {

            @Override
            public void onNoAD(AdError error) {
                Log.i("AD_DEMO", String.format("Banner onNoAD，eCode = %d, eMsg = %s", error.getErrorCode(),
                        error.getErrorMsg()));
            }

            @Override
            public void onADExposure() {
                findViewById(R.id.tabbar).setVisibility(View.GONE);
                fab.setVisibility(View.VISIBLE);
                Log.d(TAG, "banner onNOAD");
                bannerShown = true;
            }

            @Override
            public void onADReceiv() {
                bannerReceived = true;
                Log.i("AD_DEMO", "ONBannerReceive");
            }
        });
    }

    private void doRefreshBanner() {
        if (bv == null) {
            initBanner();
            bv.loadAD();
        } else if (bannerReceived && !bannerShown) {
            doCloseBanner();
            initBanner();
            bv.loadAD();
            bannerContainer.addView(bv);
            bannerReceived = false;
            bannerShown = false;
        }
    }

    private void doCloseBanner() {
        if (bannerContainer != null) {
            bannerContainer.removeAllViews();
        }
        if (bv != null) {
            bv.destroy();
            bv = null;
            findViewById(R.id.tabbar).setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        }
    }

    private void setupFloatActionButton() {
        fab = (ImageView) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCloseBanner();
            }
        });
    }

    private void createGridOn(int app_content) {
        recyclerView = (XRecyclerView) findViewById(app_content);
        // 初始化PullRecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new GridListAdapter(this, R.layout.image_thumbnail, mDataList);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                // 模拟下拉刷新网络请求
                refresh();
            }

            @Override
            public void onLoadMore() {

                if (mADManager != null) {
                    mADManager.loadAD(AD_COUNT); //加载广告条数
                    firstAD = pageIndex * 30 + FIRST_AD_POSITION;
                }
                pageIndex++;
                client.get("http://" + server + ":" + port + "/api/p/" + pageIndex + "?c=" + cat, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        try {
                            pageSize = response.getInt("pages");
                            JSONArray items = response.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject o = items.getJSONObject(i);
                                mDataList.add(new Joke(o));
                            }
                            mAdapter.notifyItemInserted(mAdapter.getItemCount());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            recyclerView.loadMoreComplete();
                            recyclerView.setNoMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        super.onFailure(statusCode, headers, responseString, throwable);
                        recyclerView.loadMoreComplete();
                        recyclerView.setNoMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
                    }
                });
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    doRefreshBanner();
                }
            }
        });
        recyclerView.refreshComplete();
    }

    private void initAD() {
        final float density = getResources().getDisplayMetrics().density;
        com.qq.e.ads.nativ.ADSize adSize = new com.qq.e.ads.nativ.ADSize((int) (getResources().getDisplayMetrics().widthPixels / density) - 15, 320); // 宽、高的单位是dp。ADSize不支持MATCH_PARENT or WRAP_CONTENT，必须传入实际的宽高
        mADManager = new NativeExpressAD(this, adSize, Constants.APPID, Constants.NativeExpressPosID, this);
    }

    private void refresh() {
        if (mADManager != null) {
            mADManager.loadAD(AD_COUNT); //加载广告条数
            firstAD = FIRST_AD_POSITION;
        }

        client.get("http://" + server + ":" + port + "/api/p/1?c=" + cat, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "onSuccess: " + statusCode);
                super.onSuccess(statusCode, headers, response);
                try {
                    pageSize = response.getInt("pages");
                    JSONArray items = response.getJSONArray("items");
                    mDataList.clear();
                    for (int i = 0; i < items.length(); i++) {
                        mDataList.add(new Joke(items.getJSONObject(i)));
                    }
                    mAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    recyclerView.refreshComplete();
                    recyclerView.setNoMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.smoothScrollToPosition(0);
                        }
                    }, 100);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                recyclerView.refreshComplete();
                recyclerView.setNoMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
            }
        });
    }

    @Override
    public void loadFullScreenImage(final ImageView iv, String imageUrl) {
        if (!TextUtils.isEmpty(imageUrl)) {
            GlideApp.with(iv.getContext())
                    .asDrawable()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }


    @Override
    public void onNoAD(AdError adError) {
        Log.i(TAG, String.format("onNoAD, error code: %d, error msg: %s", adError.getErrorCode(),
                adError.getErrorMsg()));
    }

    @Override
    public void onADLoaded(List<NativeExpressADView> adList) {
        Log.i(TAG, "onADLoaded: " + adList.size());
        mAdViewList = adList;
        for (int i = 0; i < mAdViewList.size(); i++) {
            int position = firstAD + ITEMS_PER_AD * i;
            if (position < mDataList.size()) {
                mAdViewPositionMap.put(mAdViewList.get(i), position); // 把每个广告在列表中位置记录下来
                mAdapter.addADViewToPosition(position, mAdViewList.get(i));
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRenderFail(NativeExpressADView adView) {
        Log.i(TAG, "onRenderFail: " + adView.toString());
    }

    @Override
    public void onRenderSuccess(NativeExpressADView adView) {
        Log.i(TAG, "onRenderSuccess: " + adView.toString());
    }

    @Override
    public void onADExposure(NativeExpressADView adView) {
        Log.i(TAG, "onADExposure: " + adView.toString());
    }

    @Override
    public void onADClicked(NativeExpressADView adView) {
        Log.i(TAG, "onADClicked: " + adView.toString());
    }

    @Override
    public void onADClosed(NativeExpressADView adView) {
        Log.i(TAG, "onADClosed: " + adView.toString());
        if (mAdapter != null) {
            int removedPosition = mAdViewPositionMap.get(adView);
            mAdapter.removeADView(removedPosition, adView);
        }
    }

    @Override
    public void onADLeftApplication(NativeExpressADView adView) {
        Log.i(TAG, "onADLeftApplication: " + adView.toString());
    }

    @Override
    public void onADOpenOverlay(NativeExpressADView adView) {
        Log.i(TAG, "onADOpenOverlay: " + adView.toString());
    }

    class GridListAdapter<T> extends BaseRecyclerAdapter<T> {

        public static final int TYPE_AD = 10086;

        public GridListAdapter(Context context, int layoutResId, List<T> data) {
            super(context, layoutResId, data);
        }

        @Override
        public int getItemViewType(int position) {
            if (position < mDataList.size()) {
                if (mDataList.get(position) instanceof NativeExpressADView) {
                    return TYPE_AD;
                }
            }
            return super.getItemViewType(position);
        }

        @Override
        protected void convert(BaseViewHolder holder, final T item, final int pos) {
            if (!(item instanceof Joke)) {
                convertToAd(holder, item, pos);
                return;
            }
            ImageView avatarView = holder.getView(R.id.iv);
            TextView text = holder.getView(R.id.text_content);
            final Animation animation = AnimationUtils.loadAnimation(getBaseContext(), R.anim.add_score);
            Button zan_btn = holder.getView(R.id.zan_btn);
            final TextView addOne = holder.getView(R.id.addOne_tv);
            //  按钮点击 触发动画
            zan_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addOne.setVisibility(View.VISIBLE);
                    addOne.startAnimation(animation);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            addOne.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            });
            Joke joke = (Joke) item;
            text.setText(joke.text);

            if (!TextUtils.isEmpty(joke.image) && !joke.image.toLowerCase().endsWith("null")) {
                GlideApp.with(mContext)
                        .asBitmap()
                        .load(joke.image)
                        .into(avatarView);
            } else {
                avatarView.setImageDrawable(null);
            }

            holder.getView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, FullScreenImageGalleryActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(FullScreenImageGalleryActivity.KEY_IMAGES, extractImages());
                    bundle.putInt(FullScreenImageGalleryActivity.KEY_POSITION, pos);
                    intent.putExtras(bundle);

                    startActivity(intent);
                }
            });
        }

        private void convertToAd(BaseViewHolder holder, T item, int pos) {

            final NativeExpressADView adView = (NativeExpressADView) mDataList.get(pos);
            mAdViewPositionMap.put(adView, pos); // 广告在列表中的位置是可以被更新的
            ViewGroup container = (ViewGroup) holder.convertView;
            if (container.getChildCount() > 0
                    && container.getChildAt(0) == adView) {
                return;
            }
            if (container.getChildCount() > 0) {
                container.removeAllViews();
            }

            if (adView.getParent() != null) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
            container.addView(adView);
            adView.render(); // 调用render方法后sdk才会开始展示广告
        }

        private ArrayList<String> extractImages() {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < mDataList.size(); i++) {
                Object item = mDataList.get(i);
                if (item instanceof Joke) {
                    Joke joke = (Joke) item;
                    list.add(joke.image);
                }
            }
            return list;
        }

        public void addADViewToPosition(int position, NativeExpressADView adView) {
            if (position >= 0 && position < mDataList.size() && adView != null) {
                mDataList.add(position, adView);
            }
        }

        public void removeADView(int position, NativeExpressADView adView) {
            mDataList.remove(position);
            mAdapter.notifyItemRemoved(position); // position为adView在当前列表中的位置
            mAdapter.notifyItemRangeChanged(0, mDataList.size() - 1);
        }
    }

    private class Joke {
        public String image;
        public String text;
        public List<Comment> comments;

        public Joke(JSONObject o) {
            try {
                image = static_server + o.getString("image");
                text = o.getString("text");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class Comment {
        public String text;
        public String type;
    }
}
