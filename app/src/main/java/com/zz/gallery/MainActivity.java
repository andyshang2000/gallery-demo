package com.zz.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.etiennelawlor.imagegallery.library.activities.FullScreenImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.adapters.FullScreenImageGalleryAdapter;
import com.etiennelawlor.imagegallery.library.enums.PaletteColorType;
import com.holenzhou.pullrecyclerview.BaseRecyclerAdapter;
import com.holenzhou.pullrecyclerview.BaseViewHolder;
import com.holenzhou.pullrecyclerview.PullRecyclerView;
import com.holenzhou.pullrecyclerview.layoutmanager.XLinearLayoutManager;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FullScreenImageGalleryAdapter.FullScreenImageLoader {

    private static final String TAG = "MainActivity";
    private PullRecyclerView recyclerView;
    private ArrayList<Joke> mDataList = new ArrayList<>();
    private int pageIndex = 1;
    private int pageSize = 5;
    private GridListAdapter mAdapter;
    private String cat = "";
    private int currentCat;
    private AsyncHttpClient client = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new AsyncHttpClient();
        currentCat = R.id.cat_all;

        setupToolBar();
        setupFloatActionButton();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FullScreenImageGalleryActivity.setFullScreenImageLoader(this);

        createGridOn(R.id.rv);
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

    private void refresh() {
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
    public void loadFullScreenImage(final ImageView iv, String imageUrl, int width, final LinearLayout bgLinearLayout) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(iv.getContext())
                    .load(imageUrl)
                    .listener(mRequestListener)
                    .into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }

    RequestListener mRequestListener = new RequestListener() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            Log.d(TAG, "onException: " + e.toString() + "  model:" + model + " isFirstResource: " + isFirstResource);
            return false;
        }

        @Override
        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
            Log.e(TAG, "model:" + model + " isFirstResource: " + isFirstResource);
            return false;
        }
    };

    class GridListAdapter<T> extends BaseRecyclerAdapter<T> {

        public GridListAdapter(Context context, int layoutResId, List<T> data) {
            super(context, layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, final T item, final int pos) {
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
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            addOne.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            });
            Joke joke = (Joke) item;
            text.setText(joke.text);
            if (!TextUtils.isEmpty(joke.image)) {
                Picasso.with(mContext)
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

        private ArrayList<String> extractImages() {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < mDataList.size(); i++) {
                list.add(mDataList.get(i).image);
            }
            return list;
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
