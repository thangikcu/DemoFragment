package com.no1vietnam.demofragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

/**
 * Created by thanggun99 on 08/11/2017.
 */

public class ChildFragment extends BaseFragment {

    public static ChildFragment newInstance(int numberChild) {
        ChildFragment childFragment = new ChildFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FragmentMain.NUMBER_CHILD, numberChild);
        childFragment.setArguments(arguments);
        return childFragment;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int numberChild = getArguments().getInt(FragmentMain.NUMBER_CHILD);

        ((TextView) view.findViewById(R.id.tv_title)).setText(numberChild + " ");
    }
}
