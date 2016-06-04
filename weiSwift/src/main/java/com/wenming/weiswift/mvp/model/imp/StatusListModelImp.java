package com.wenming.weiswift.mvp.model.imp;

import android.content.Context;
import android.text.TextUtils;

import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.wenming.weiswift.api.GroupAPI;
import com.wenming.weiswift.api.StatusesAPI;
import com.wenming.weiswift.entity.Status;
import com.wenming.weiswift.entity.list.StatusList;
import com.wenming.weiswift.mvp.model.StatusListModel;
import com.wenming.weiswift.ui.common.NewFeature;
import com.wenming.weiswift.ui.common.login.AccessTokenKeeper;
import com.wenming.weiswift.ui.common.login.Constants;
import com.wenming.weiswift.utils.SDCardUtil;
import com.wenming.weiswift.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wenmingvs on 16/5/14.
 */
public class StatusListModelImp implements StatusListModel {
    /**
     * 全局刷新的间隔时间
     */
    private static int REFRESH_FRIENDS_TIMELINE_TASK = 15 * 60 * 1000;
    private ArrayList<Status> mStatusList = new ArrayList<>();
    private Context mContext;
    private OnDataFinishedListener mOnDataFinishedListener;
    private Timer mTimer;
    private TimerTask mTimerTask;
    /**
     * 当前的分组位置
     */
    private long mCurrentGroup = Constants.GROUP_TYPE_ALL;
    /**
     * 是否全局刷新
     */
    private boolean mRefrshAll = true;


    @Override
    public void friendsTimeline(Context context, OnDataFinishedListener onDataFinishedListener) {
        StatusesAPI mStatusesAPI = new StatusesAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        setRefrshFriendsTimelineTask();
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        long sinceId = checkout(Constants.GROUP_TYPE_ALL);
        mStatusesAPI.friendsTimeline(sinceId, 0, NewFeature.GET_WEIBO_NUMS, 1, false, 0, false, pullToRefreshListener);
    }


    @Override
    public void bilateralTimeline(Context context, OnDataFinishedListener onDataFinishedListener) {
        StatusesAPI mStatusesAPI = new StatusesAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        setRefrshFriendsTimelineTask();
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        long sinceId = checkout(Constants.GROUP_TYPE_FRIENDS_CIRCLE);
        mStatusesAPI.bilateralTimeline(sinceId, 0, NewFeature.GET_WEIBO_NUMS, 1, false, StatusesAPI.FEATURE_ORIGINAL, false, pullToRefreshListener);
    }

    @Override
    public void timeline(long newGroupId, Context context, OnDataFinishedListener onDataFinishedListener) {
        GroupAPI groupAPI = new GroupAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        setRefrshFriendsTimelineTask();
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        long sinceId = checkout(newGroupId);
        groupAPI.timeline(newGroupId, sinceId, 0, NewFeature.GET_WEIBO_NUMS, 1, false, GroupAPI.FEATURE_ALL, pullToRefreshListener);
    }

    /**
     * 获取指定分组的下一页微博
     *
     * @param groundId
     * @param context
     * @param onDataFinishedListener
     */
    @Override
    public void timelineNextPage(long groundId, Context context, OnDataFinishedListener onDataFinishedListener) {
        GroupAPI groupAPI = new GroupAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        setRefrshFriendsTimelineTask();
        String maxId = mStatusList.get(mStatusList.size() - 1).id;
        groupAPI.timeline(groundId, 0, Long.valueOf(maxId), NewFeature.GET_WEIBO_NUMS, 1, false, GroupAPI.FEATURE_ALL, nextPageListener);
    }

    /**
     * 获取我关注的人的下一页微博
     *
     * @param context
     * @param onDataFinishedListener
     */
    @Override
    public void friendsTimelineNextPage(Context context, OnDataFinishedListener onDataFinishedListener) {
        setRefrshFriendsTimelineTask();
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        StatusesAPI mStatusesAPI = new StatusesAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        String maxId = mStatusList.get(mStatusList.size() - 1).id;
        mStatusesAPI.friendsTimeline(0, Long.valueOf(maxId), NewFeature.LOADMORE_WEIBO_ITEM, 1, false, 0, false, nextPageListener);
    }


    /**
     * 获取相互关注的人的下一页微博
     *
     * @param context
     * @param onDataFinishedListener
     */
    @Override
    public void bilateralTimelineNextPage(Context context, OnDataFinishedListener onDataFinishedListener) {
        setRefrshFriendsTimelineTask();
        mContext = context;
        mOnDataFinishedListener = onDataFinishedListener;
        StatusesAPI mStatusesAPI = new StatusesAPI(context, Constants.APP_KEY, AccessTokenKeeper.readAccessToken(context));
        String maxId = mStatusList.get(mStatusList.size() - 1).id;
        mStatusesAPI.bilateralTimeline(0, Long.valueOf(maxId), NewFeature.LOADMORE_WEIBO_ITEM, 1, false, StatusesAPI.FEATURE_ORIGINAL, false, nextPageListener);
    }

    @Override
    public void cacheLoad(long groupType, Context context, OnDataFinishedListener onDataFinishedListener) {
        String response = null;
        mCurrentGroup = groupType;
        if (groupType == Constants.GROUP_TYPE_ALL) {
            response = SDCardUtil.get(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", "全部微博" + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt");
        } else if (groupType == Constants.GROUP_TYPE_FRIENDS_CIRCLE) {
            response = SDCardUtil.get(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", "好友圈" + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt");
        } else {
            response = SDCardUtil.get(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", groupType + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt");
        }
        if (response != null) {
            mStatusList = StatusList.parse(response).statusList;
            onDataFinishedListener.onDataFinish(mStatusList);
        } else {
            mOnDataFinishedListener.noDataInFirstLoad("还没有缓存的内容");
        }
    }


    @Override
    public void cacheSave(long groupType, Context context, String response) {
        if (groupType == Constants.GROUP_TYPE_ALL) {
            SDCardUtil.put(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", "全部微博" + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt", response);
        } else if (groupType == Constants.GROUP_TYPE_FRIENDS_CIRCLE) {
            SDCardUtil.put(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", "好友圈" + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt", response);
        } else {
            SDCardUtil.put(context, SDCardUtil.getSDCardPath() + "/weiSwift/home", groupType + AccessTokenKeeper.readAccessToken(context).getUid() + ".txt", response);
        }
    }

    @Override
    public void setRefrshFriendsTimelineTask() {
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mRefrshAll = true;
                }
            };
            mTimer = new Timer();
            mTimer.schedule(mTimerTask, 0, REFRESH_FRIENDS_TIMELINE_TASK);
        }
    }

    @Override
    public void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    /**
     * 用于更新sinceId和maxId的值
     *
     * @param newGroupId
     * @return
     */
    private long checkout(long newGroupId) {
        long sinceId = 0;
        if (mCurrentGroup != newGroupId) {
            mRefrshAll = true;
        }
        if (mStatusList.size() > 0 && mCurrentGroup == newGroupId && mRefrshAll == false) {
            sinceId = Long.valueOf(mStatusList.get(0).id);
        }
        if (mRefrshAll) {
            sinceId = 0;
        }
        mCurrentGroup = newGroupId;
        return sinceId;
    }


    private RequestListener pullToRefreshListener = new RequestListener() {
        @Override
        public void onComplete(String response) {
            ArrayList<Status> temp = StatusList.parse(response).statusList;
            if (temp != null && temp.size() > 0) {
                if (mStatusList != null) {
                    mStatusList.clear();
                }
                cacheSave(mCurrentGroup, mContext, response);
                mStatusList = temp;
                mOnDataFinishedListener.onDataFinish(mStatusList);
                mRefrshAll = false;
            } else {
                if (mRefrshAll == false) {//局部刷新，get不到数据
                    ToastUtil.showShort(mContext, "没有更新的内容了");
                    mOnDataFinishedListener.noMoreData();
                } else {//全局刷新，get不到数据
                    mOnDataFinishedListener.noDataInFirstLoad("你还没有为此组增加成员");
                }
            }
            mRefrshAll = false;
        }

        @Override
        public void onWeiboException(WeiboException e) {
            ToastUtil.showShort(mContext, e.getMessage());
            mOnDataFinishedListener.onError(e.getMessage());
            cacheLoad(mCurrentGroup, mContext, mOnDataFinishedListener);
        }
    };


    private RequestListener nextPageListener = new RequestListener() {
        @Override
        public void onComplete(String response) {
            if (!TextUtils.isEmpty(response)) {
                ArrayList<Status> temp = StatusList.parse(response).statusList;
                if (temp.size() == 0 || (temp != null && temp.size() == 1 && temp.get(0).id.equals(mStatusList.get(mStatusList.size() - 1).id))) {
                    mOnDataFinishedListener.noMoreData();
                } else if (temp.size() > 1) {
                    temp.remove(0);
                    mStatusList.addAll(temp);
                    mOnDataFinishedListener.onDataFinish(mStatusList);
                }
            } else {
                mOnDataFinishedListener.noMoreData();
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            ToastUtil.showShort(mContext, e.getMessage());
            mOnDataFinishedListener.onError(e.getMessage());
        }
    };
}
