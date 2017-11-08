package com.no1vietnam.demofragment;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.no1vietnam.demofragment.fragnav.FragNavController;

import java.util.Stack;

public class MainActivity extends AppCompatActivity implements FragNavController.TransactionListener, FragNavController.RootFragmentListener {
    public static FragNavController fragNavController;
    public final int FRAME_1 = 0;
    public final int FRAME_2 = 1;
    public final int FRAME_3 = 2;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private TextView tvTitle;
    private ImageView ivBtnLeft;
    private ImageView ivBtnRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

        fragNavController = FragNavController.newBuilder(savedInstanceState, getSupportFragmentManager())
                .transactionListener(this)
                .setmContaierId(R.id.frame)
                .build();
        fragNavController.setmRootFragmentListener(this, 3);

        ivBtnRight.setOnClickListener(view -> {
            if (fragNavController.getCurrentStack() != null) {
                FragmentMain fragmentMain = (FragmentMain) fragNavController
                        .getCurrentStack().firstElement();

                fragNavController.pushFragment(ChildFragment
                        .newInstance(++fragmentMain.numberChild));
            }
        });

        ivBtnLeft.setOnClickListener(view -> {
            if (fragNavController.isRootFragment()) {
                drawerLayout.openDrawer(navigationView);
            } else {
                onBackPressed();
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_frame1:
                    fragNavController.switchTab(FRAME_1);
                    break;
                case R.id.nav_frame2:
                    fragNavController.switchTab(FRAME_2);
                    break;
                case R.id.nav_frame3:
                    fragNavController.switchTab(FRAME_3);
                    break;
            }
            drawerLayout.closeDrawer(navigationView);
            return true;
        });

        fragNavController.switchTab(0);
    }

    private void findViews() {
        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigation_left);
        tvTitle = findViewById(R.id.tv_title);
        ivBtnLeft = findViewById(R.id.iv_btn_left);
        ivBtnRight = findViewById(R.id.iv_btn_right);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            Stack<Fragment> currentStack = fragNavController.getCurrentStack();
            if (currentStack != null && currentStack.size() > 1) {
                fragNavController.popFragment();
                FragmentMain fragmentMain = (FragmentMain) currentStack.firstElement();
                fragmentMain.numberChild--;
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onTabTransaction(Fragment fragment, int index) {
        showInstanceView(fragment);
    }

    private void showInstanceView(Fragment fragment) {
        if (fragNavController.isRootFragment()) {
            ivBtnLeft.setImageResource(R.drawable.ic_menu);
        } else {
            ivBtnLeft.setImageResource(R.drawable.ic_keyboard_arrow_left_white_24dp);
        }

        Bundle arguments = fragment.getArguments();
        if (arguments != null) {
            if (arguments.containsKey(BaseFragment.TITLE)) {
                tvTitle.setText(arguments.getString(BaseFragment.TITLE));
            }
            if (arguments.containsKey(BaseFragment.ICON_RIGHT)) {
                ivBtnRight.setImageResource(arguments.getInt(BaseFragment.ICON_RIGHT));
            }
        }
    }

    @Override
    public void onFragmentTransaction(Fragment fragment,
                                      FragNavController.TransactionType transactionType) {
        showInstanceView(fragment);
    }

    @Override
    public Fragment getRootFragment(int index) {
        switch (index) {
            case FRAME_1:
                return FragmentMain.newInstance("Main 1",
                        R.drawable.ic_star_half_black_24dp);
            case FRAME_2:
                return FragmentMain.newInstance("Main 2",
                        R.drawable.ic_assignment_ind_black_24dp);
            case FRAME_3:
                return FragmentMain.newInstance("Main 3",
                        R.drawable.ic_av_timer_black_24dp);
        }
        return null;
    }
}
