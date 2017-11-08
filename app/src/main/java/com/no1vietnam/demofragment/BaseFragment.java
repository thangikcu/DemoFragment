package com.no1vietnam.demofragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by thanggun99 on 07/11/2017.
 */

public abstract class BaseFragment extends Fragment {
    public static final String TITLE = "title";
    public static final String ICON_RIGHT = "icon_right";

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment, container, false);
    }

}
