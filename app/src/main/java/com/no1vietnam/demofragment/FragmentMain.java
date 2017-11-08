package com.no1vietnam.demofragment;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

/**
 * Created by thanggun99 on 07/11/2017.
 */

public class FragmentMain extends BaseFragment {
    public static final String NUMBER_CHILD = "number_child";

    public int numberChild = 0;

    public static FragmentMain newInstance(String title, @DrawableRes int resIcon) {
        Bundle arguments = new Bundle();
        arguments.putInt(ICON_RIGHT, resIcon);
        arguments.putString(TITLE, title);
        FragmentMain fragmentMain = new FragmentMain();
        fragmentMain.setArguments(arguments);
        return fragmentMain;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();

        ((TextView) view.findViewById(R.id.tv_title)).setText(arguments.getString(TITLE));
    }
}
