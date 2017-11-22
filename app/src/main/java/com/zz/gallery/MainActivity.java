package com.zz.gallery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.holenzhou.pullrecyclerview.BaseRecyclerAdapter;
import com.holenzhou.pullrecyclerview.BaseViewHolder;
import com.holenzhou.pullrecyclerview.PullRecyclerView;
import com.holenzhou.pullrecyclerview.layoutmanager.XLinearLayoutManager;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.comm.util.AdError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FullScreenImageGalleryActivity.FullScreenImageLoader, NativeExpressAD.NativeExpressADListener {

    private static final String TAG = "MainActivity";
    private PullRecyclerView recyclerView;
    private ArrayList<Object> mDataList = new ArrayList<>();
    private int pageIndex = 1;
    private int pageSize = 5;
    private GridListAdapter mAdapter;
    private String cat = "";
    private int currentCat;
    private AsyncHttpClient client = null;
    private Handler handler;
    //广告
    private NativeExpressAD mADManager;
    private List<NativeExpressADView> mAdViewList;
    private HashMap<NativeExpressADView, Integer> mAdViewPositionMap = new HashMap<NativeExpressADView, Integer>();
    public static final int AD_COUNT = 3;    // 加载广告的条数，取值范围为[1, 10]
    public static int FIRST_AD_POSITION = 3; // 第一条广告的位置
    public static int ITEMS_PER_AD = 10;     // 每间隔10个条目插入一条广告
    //*/广告

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new AsyncHttpClient();
        handler = new Handler();
        currentCat = R.id.cat_all;

        setupToolBar();
        setupFloatActionButton();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FullScreenImageGalleryActivity.setFullScreenImageLoader(this);

        createGridOn(R.id.rv);
        initAD();
    }

    private void setupToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupFloatActionButton() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void createGridOn(int app_content) {
        recyclerView = (PullRecyclerView) findViewById(app_content);
        // 初始化PullRecyclerView
        recyclerView.setLayoutManager(new XLinearLayoutManager(this));
//        recyclerView.setLayoutManager(new XGridLayoutManager(this, 3));
        mAdapter = new GridListAdapter(this, R.layout.image_thumbnail, mDataList);
        recyclerView.setAdapter(mAdapter);

        recyclerView.setColorSchemeResources(R.color.colorAccent); // 设置下拉刷新的旋转圆圈的颜色
        recyclerView.enablePullRefresh(true); // 开启下拉刷新，默认即为true，可不用设置
        recyclerView.enableLoadDoneTip(true, R.string.load_done_tip); // 开启数据全部加载完成时的底部提示，默认为false
        recyclerView.setOnRecyclerRefreshListener(new PullRecyclerView.OnRecyclerRefreshListener() {
            @Override
            public void onPullRefresh() {
                // 模拟下拉刷新网络请求
                refresh();
            }

            @Override
            public void onLoadMore() {

                pageIndex++;
                client.get("http://192.168.0.103:50001/api/p/" + pageIndex + "?c=" + cat, new JsonHttpResponseHandler() {
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
                            recyclerView.stopRefresh();
                            recyclerView.enableLoadMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        super.onFailure(statusCode, headers, responseString, throwable);
                        recyclerView.stopRefresh();
                        recyclerView.enableLoadMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
                    }
                });
            }
        });
        recyclerView.postRefreshing();
    }

    private void initAD() {
        final float density = getResources().getDisplayMetrics().density;
        ADSize adSize = new ADSize((int) (getResources().getDisplayMetrics().widthPixels / density), 340); // 宽、高的单位是dp。ADSize不支持MATCH_PARENT or WRAP_CONTENT，必须传入实际的宽高
        mADManager = new NativeExpressAD(this, adSize, Constants.APPID, Constants.NativeExpressPosID, this);
    }

    private void refresh() {
        mADManager.loadAD(6); //加载广告条数
        client.get("http://192.168.0.103:50001/api/p/1?c=" + cat, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
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
                    recyclerView.stopRefresh();
                    recyclerView.enableLoadMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
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
                recyclerView.stopRefresh();
                recyclerView.enableLoadMore(pageIndex < pageSize); // 当剩余还有大于0页的数据时，开启上拉加载更多
                recyclerView.setSelection(0);
            }
        });
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id != currentCat) {
            if (id == R.id.cat_all) {
                cat = "";
            } else if (id == R.id.cat_gif) {
                cat = "搞笑GIF";
            } else if (id == R.id.cat_text) {
                cat = "幽默笑话";
            } else if (id == R.id.cat_images) {
                cat = "奇闻怪事";
            }
            refresh();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            int position = FIRST_AD_POSITION + ITEMS_PER_AD * i;
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
            if (!TextUtils.isEmpty(joke.image)) {
                Glide.with(mContext)
                        .asBitmap()
                        .load(joke.image)
//                        .resize(100, 200)
//                        .centerCrop()
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
                image = "http://192.168.0.103:8081/images/" + o.getString("image");
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
