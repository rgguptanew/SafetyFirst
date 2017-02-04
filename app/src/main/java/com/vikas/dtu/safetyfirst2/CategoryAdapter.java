package com.vikas.dtu.safetyfirst2;

/**
 * Created by Mukul on 1/23/2017.
 */

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.vikas.dtu.safetyfirst2.mDiscussion.fragment.BookmarkedFragment;
import com.vikas.dtu.safetyfirst2.mDiscussion.fragment.MyPostsFragment;
import com.vikas.dtu.safetyfirst2.mDiscussion.fragment.NewPostFragment;
import com.vikas.dtu.safetyfirst2.mDiscussion.fragment.RecentPostsFragment;


public class CategoryAdapter extends FragmentPagerAdapter {

    private Context mContext;


    public CategoryAdapter(Context context, FragmentManager fm)

    {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0)
            return new RecentPostsFragment();
        else if (position == 1)
            return new MyPostsFragment();
        else if (position == 2)
            return new BookmarkedFragment();
        else
            return new NewPostFragment();
    }

    @Override
    public int getCount() {
        return 4;
    }



}
