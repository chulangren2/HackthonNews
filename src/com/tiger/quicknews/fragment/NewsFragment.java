
package com.tiger.quicknews.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.baifendian.mobile.BfdAgent;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.tiger.quicknews.R;
import com.tiger.quicknews.activity.*;
import com.tiger.quicknews.adapter.CardsAnimationAdapter;
import com.tiger.quicknews.adapter.NewAdapter;
import com.tiger.quicknews.bean.NewModle;
import com.tiger.quicknews.http.HttpUtil;
import com.tiger.quicknews.http.Url;
import com.tiger.quicknews.http.json.NewListJson;
import com.tiger.quicknews.initview.InitView;
import com.tiger.quicknews.utils.StringUtils;
import com.tiger.quicknews.wedget.slideingactivity.IntentUtils;
import com.tiger.quicknews.wedget.swiptlistview.SwipeListView;
import com.tiger.quicknews.wedget.viewimage.Animations.DescriptionAnimation;
import com.tiger.quicknews.wedget.viewimage.Animations.SliderLayout;
import com.tiger.quicknews.wedget.viewimage.SliderTypes.BaseSliderView;
import com.tiger.quicknews.wedget.viewimage.SliderTypes.BaseSliderView.OnSliderClickListener;
import com.tiger.quicknews.wedget.viewimage.SliderTypes.TextSliderView;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EFragment(R.layout.activity_main)
public class NewsFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        OnSliderClickListener {
    protected SliderLayout mDemoSlider;
    @ViewById(R.id.swipe_container)
    protected SwipeRefreshLayout swipeLayout;
    @ViewById(R.id.listview)
    protected SwipeListView mListView;
    @ViewById(R.id.progressBar)
    protected ProgressBar mProgressBar;
    protected HashMap<String, String> url_maps;

    protected HashMap<String, NewModle> newHashMap;
    private Activity activity;
    @Bean
    protected NewAdapter newAdapter;
    protected List<NewModle> listsModles;
    private int index = 0;
    private boolean isRefresh = false;
    private String channelName;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Bundle args = getArguments();
    	if(args != null ){
    		channelName = args.getString("channel");
    	}
    	super.onCreate(savedInstanceState);
    }
    @AfterInject
    protected void init() {

        listsModles = new ArrayList<NewModle>();
        url_maps = new HashMap<String, String>();

        newHashMap = new HashMap<String, NewModle>();
    }

    @AfterViews
    protected void initView() {
        swipeLayout.setOnRefreshListener(this);
        InitView.instance().initSwipeRefreshLayout(swipeLayout);
        InitView.instance().initListView(mListView, getActivity());
//        View headView = LayoutInflater.from(getActivity()).inflate(R.layout.head_item, null);
//        mDemoSlider = (SliderLayout) headView.findViewById(R.id.slider);
//        mListView.addHeaderView(headView);
        AnimationAdapter animationAdapter = new CardsAnimationAdapter(newAdapter);
        animationAdapter.setAbsListView(mListView);
        mListView.setAdapter(animationAdapter);
        loadData(getNewUrl(index+""));

        mListView.setOnBottomListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPagte++;
                index = index + 20;
                loadData(getNewUrl(index + ""));
            }
        });
    }

    private void loadData(String url) {
        if (getMyActivity().hasNetWork()) {
            loadNewList(url);
        } else {
            mListView.onBottomComplete();
            mProgressBar.setVisibility(View.GONE);
            getMyActivity().showShortToast(getString(R.string.not_network));
            String result = getMyActivity().getCacheStr("NewsFragment" + currentPagte);
            if (!StringUtils.isEmpty(result)) {
                getResult(result);
            }
        }
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentPagte = 1;
                isRefresh = true;
                loadData(getNewUrl("0"));
                url_maps.clear();
//                mDemoSlider.removeAllSliders();
            }
        }, 2000);
    }

    @ItemClick(R.id.listview)
    protected void onItemClick(int position) {
        NewModle newModle = listsModles.get(position - 1);
        enterDetailActivity(newModle);
    }

    public void enterDetailActivity(NewModle newModle) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("newModle", newModle);
        Class<?> class1;
        if (newModle.getImagesModle() != null && newModle.getImagesModle().getImgList().size() > 1) {
            class1 = ImageDetailActivity_.class;
        } else {
            class1 = DetailsActivity_.class;
        }
        ((BaseActivity) getActivity()).openActivity(class1,
                bundle, 0);
    }

    @Background
    void loadNewList(String url) {
        String result;
        try {
            //result = HttpUtil.getByHttpClient(getActivity(), url);
        	result = getRecData(url);
            if (!StringUtils.isEmpty(result)) {
                getResult(result);
            } else {
                swipeLayout.setRefreshing(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //获取推荐
    private boolean isComplete = false;
    private String result = null;
    public String getRecData(String url){
    	//String result = null;
    	Map<String, String> params = new HashMap<String, String>();
		params.put(
				"fmt",
				"{\"iid\":\"$iid\",\"url\":\"$url\",\"img\":\"$small_img\",\"ptime\":\"$ptime\",\"title\":\"$title\"}");
		
		//params.put("num", "20");
		params.put("tag",channelName);
		params.put("iid","100020000");
		BfdAgent.recommend(activity, "rec_C6613205_93B6_61A6_9FEC_180B70F91B94", params, new BfdAgent.Runnable() {

			@Override
			public void run(String arg0, JSONArray arg1) {
				System.out.println(arg1.toString());
				if(arg1 != null && arg1.length()>0){
					result = arg1.toString();
				}
				isComplete = true;
			}
		});
		int i = 0;
		while(!isComplete){
			try {
				
				Thread.sleep(200);
				i++;
				if( i == 5){
					isComplete = true;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isComplete = false;
		return result;
    }
    @UiThread
    public void getResult(String result) {
        getMyActivity().setCacheStr(channelName + currentPagte, result);
        if (isRefresh) {
            isRefresh = false;
            newAdapter.clear();
            listsModles.clear();
        }
        mProgressBar.setVisibility(View.GONE);
        swipeLayout.setRefreshing(false);
        JSONArray results = null;
        if(result != null){
        	try {
				results = new JSONArray(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        //添加 数据
        List<NewModle> list = new ArrayList<NewModle>();
        if(results != null && results.length()>0){
    		for(int i = 0;i < results.length();i++){
    			JSONObject js;
				try {
					js = results.getJSONObject(i);
					list.add(getNewModle(js));
				} catch (JSONException e) {
				}
    		}
    	}
      /*  List<NewModle> list =
                NewListJson.instance(getActivity()).readJsonNewModles(result,
                        Url.TopId);*/
     /*   if (index == 0 && list.size() >= 4) {
            initSliderLayout(list);
        } else {
            newAdapter.appendList(list);
        }*/
        newAdapter.appendList(list);
        listsModles.addAll(list);
        mListView.onBottomComplete();
    }
    private  NewModle getNewModle(JSONObject jsonObject){
//    	NewListJson nlj = NewListJson.instance(getActivity());
        String docid = "";
        String title = "";
        String digest = "";
        String imgsrc = "";
        String source = "";
        String ptime = "";
        String tag = "";
        try {
			docid = jsonObject.getString("iid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
        try {
			title = jsonObject.getString("title");
		} catch (JSONException e) {
		}
        //digest = ;
        try {
			imgsrc = jsonObject.getString("img");
		} catch (JSONException e) {
		}
        try {
			source = jsonObject.getString("source");
		} catch (JSONException e) {
		}
        try {
			ptime = jsonObject.getString("ptime");
		} catch (JSONException e) {
		}
        try {
			tag = jsonObject.getString("tag");
		} catch (JSONException e) {
		}
  /*      docid =nlj.getString("docid", jsonObject);
        title = getString("title", jsonObject);
        digest = getString("digest", jsonObject);
        imgsrc = getString("imgsrc", jsonObject);
        source = getString("source", jsonObject);
        ptime = getString("ptime", jsonObject);
        tag = getString("TAG", jsonObject);*/

        NewModle newModle = new NewModle();

        newModle.setDigest(digest);
        newModle.setDocid(docid);
        newModle.setImgsrc(imgsrc);
        newModle.setTitle(title);
        newModle.setPtime(ptime);
        newModle.setSource(source);
        newModle.setTag(tag);
        return newModle;
    }
    @Override
    public void onSliderClick(BaseSliderView slider) {
        NewModle newModle = newHashMap.get(slider.getUrl());
        enterDetailActivity(newModle);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
