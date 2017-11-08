package com.no1vietnam.demofragment.fragnav;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.view.View;

import org.json.JSONArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@SuppressWarnings("ALL")
public class FragNavController {
    public static final int NO_TAB = -1;

    private static final String EXTRA_SELECTED_TAB_INDEX = FragNavController.class.getName() + ":EXTRA_SELECTED_TAB_INDEX";
    private static final String EXTRA_CURRENT_FRAGMENT = FragNavController.class.getName() + ":EXTRA_CURRENT_FRAGMENT";
    private static final String EXTRA_FRAGMENT_STACK = FragNavController.class.getName() + ":EXTRA_FRAGMENT_STACK";
    @NonNull
    private final FragmentManager mFragmentManager;
    private final FragNavTransactionOptions mDefaultTransactionOptions;
    @Nullable
    private final TransactionListener mTransactionListener;
    @IdRes
    private int mContainerId;
    @NonNull
    private List<Stack<Fragment>> mFragmentStacks;
    private int numberTab;
    private int mSelectedTabIndex;
    @Nullable
    private Fragment mCurrentFrag;
    @Nullable
    private RootFragmentListener mRootFragmentListener;
    private boolean mExecutingTransaction;
    private Fragment containerFragment;
    private Fragment currentRootFragment;
    private DialogFragment mCurrentDialogFrag;
    private int tagCount;

    private FragNavController(Builder builder, @Nullable Bundle savedInstanceState) {
        mFragmentManager = builder.mFragmentManager;
        mTransactionListener = builder.mTransactionListener;
        mDefaultTransactionOptions = builder.mDefaultTransactionOptions;

        mFragmentStacks = new ArrayList<>(1);
        Stack<Fragment> stack = new Stack<>();
        stack.add(null);
        mFragmentStacks.add(stack);

        mContainerId = builder.mContaierId;
        mSelectedTabIndex = NO_TAB;

        if (!restoreFromBundle(savedInstanceState)) {
            clearFragmentManager();
            clearDialogFragment();
        }
    }

    public static Builder newBuilder(@Nullable Bundle savedInstanceState, FragmentManager fragmentManager) {
        return new Builder(savedInstanceState, fragmentManager);
    }

    public void switchTab(int index) throws IndexOutOfBoundsException {
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Index : " + index + ", current stack size : " + mFragmentStacks.size());
        }
        if (mSelectedTabIndex != index) {
            mSelectedTabIndex = index;

            FragmentTransaction ft = createTransactionWithOptions(null);

            detachCurrentFragment(ft);

            Fragment fragment = null;
            if (index == NO_TAB) {
                ft.commitAllowingStateLoss();
            } else {
                fragment = reattachPreviousFragment(ft);
                if (fragment != null) {
                    ft.commitAllowingStateLoss();
                } else {
                    fragment = getRootFragment(mSelectedTabIndex);
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    ft.commitAllowingStateLoss();
                }
            }

            executePendingTransactions();

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onTabTransaction(mCurrentFrag, mSelectedTabIndex);
            }
        } else {
            clearStack();
        }
    }

    private String generateTag(Fragment fragment) {
        return fragment.getClass().getName() + ++tagCount;
    }

    public void showDialogFragment(@Nullable DialogFragment dialogFragment, boolean clearAllBeforeShow) {
        if (dialogFragment != null) {
            FragmentManager fragmentManager;
            if (mCurrentFrag != null) {
                fragmentManager = mCurrentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }

            if (clearAllBeforeShow) {
                if (fragmentManager.getFragments() != null) {
                    for (Fragment fragment : fragmentManager.getFragments()) {
                        if (fragment instanceof DialogFragment) {
                            ((DialogFragment) fragment).dismiss();
                            mCurrentDialogFrag = null;
                        }
                    }
                }
            }

            mCurrentDialogFrag = dialogFragment;
            try {
                dialogFragment.show(fragmentManager, generateTag(dialogFragment));
            } catch (IllegalStateException ignored) {
            }
        }
    }

    @Nullable
    @CheckResult
    public DialogFragment getCurrentDialogFrag() {
        if (mCurrentDialogFrag != null) {
            return mCurrentDialogFrag;
        } else {
            FragmentManager fragmentManager;
            if (mCurrentFrag != null) {
                fragmentManager = mCurrentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }
            if (fragmentManager.getFragments() != null) {
                for (Fragment fragment : fragmentManager.getFragments()) {
                    if (fragment instanceof DialogFragment) {
                        mCurrentDialogFrag = (DialogFragment) fragment;
                        break;
                    }
                }
            }
        }
        return mCurrentDialogFrag;
    }

    public void clearDialogFragment() {
        FragmentManager fragmentManager;
        if (mCurrentFrag != null) {
            fragmentManager = mCurrentFrag.getChildFragmentManager();
        } else {
            fragmentManager = mFragmentManager;
        }

        if (fragmentManager.getFragments() != null) {
            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof DialogFragment) {
                    ((DialogFragment) fragment).dismiss();
                }
            }
        }
        mCurrentDialogFrag = null;
    }

    public void pushFragment(@Nullable Fragment fragment) {
        if (fragment != null && mSelectedTabIndex != NO_TAB) {
            FragmentTransaction ft = createTransactionWithOptions(null);
            detachCurrentFragment(ft);
            ft.add(mContainerId, fragment, generateTag(fragment));
            ft.commitAllowingStateLoss();

            executePendingTransactions();

            mFragmentStacks.get(mSelectedTabIndex).push(fragment);

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag, TransactionType.PUSH);
            }
        }
    }

    public void popFragments(int popDepth) throws UnsupportedOperationException {
        popFragments(popDepth, null);
    }

    public void popFragment(@Nullable FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
        popFragments(1, transactionOptions);
    }

    public void popFragment() throws UnsupportedOperationException {
        popFragment(null);
    }

    public void popFragments(int popDepth, @Nullable FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
        if (isRootFragment()) {
            return;
            /*throw new UnsupportedOperationException(
                    "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)");*/
        } else if (popDepth < 1) {
            throw new UnsupportedOperationException("popFragments parameter needs to be greater than 0");
        } else if (mSelectedTabIndex == NO_TAB) {
            throw new UnsupportedOperationException("You can not pop fragments when no tab is selected");
        }

        if (popDepth >= mFragmentStacks.get(mSelectedTabIndex).size() - 1) {
            clearStack(transactionOptions);
            return;
        }

        Fragment fragment;
        FragmentTransaction ft = createTransactionWithOptions(transactionOptions);

        for (int i = 0; i < popDepth; i++) {
            fragment = mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).pop().getTag());
            if (fragment != null) {
                ft.remove(fragment);
            }
        }

        fragment = reattachPreviousFragment(ft);

        boolean bShouldPush = false;
        if (fragment != null) {
            ft.commitAllowingStateLoss();
        } else {
            if (!mFragmentStacks.get(mSelectedTabIndex).isEmpty()) {
                fragment = mFragmentStacks.get(mSelectedTabIndex).peek();
                ft.add(mContainerId, fragment, generateTag(fragment));
                ft.commitAllowingStateLoss();
            } else {
                fragment = getRootFragment(mSelectedTabIndex);
                ft.add(mContainerId, fragment, generateTag(fragment));
                ft.commitAllowingStateLoss();

                bShouldPush = true;
            }
        }

        executePendingTransactions();

        if (bShouldPush) {
            mFragmentStacks.get(mSelectedTabIndex).push(fragment);
        }

        mCurrentFrag = fragment;
        if (mTransactionListener != null) {
            mTransactionListener.onFragmentTransaction(mCurrentFrag, TransactionType.POP);
        }
    }

    public void clearStack(@Nullable FragNavTransactionOptions transactionOptions) {
        if (mSelectedTabIndex == NO_TAB) {
            return;
        }

        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);

        if (fragmentStack.size() > 1) {
            Fragment fragment;
            FragmentTransaction ft = createTransactionWithOptions(transactionOptions);

            while (fragmentStack.size() > 1) {
                fragment = mFragmentManager.findFragmentByTag(fragmentStack.pop().getTag());
                if (fragment != null) {
                    ft.remove(fragment);
                }
            }

            fragment = reattachPreviousFragment(ft);

            boolean bShouldPush = false;
            if (fragment != null) {
                ft.commitAllowingStateLoss();
            } else {
                if (!fragmentStack.isEmpty()) {
                    fragment = fragmentStack.peek();
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    ft.commitAllowingStateLoss();
                } else {
                    fragment = getRootFragment(mSelectedTabIndex);
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    ft.commitAllowingStateLoss();

                    bShouldPush = true;
                }
            }

            executePendingTransactions();

            if (bShouldPush) {
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }

            mFragmentStacks.set(mSelectedTabIndex, fragmentStack);

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag, TransactionType.POP);
            }
        }
    }

    public void clearStack() {
        clearStack(null);
    }

    public void replaceFragment(@NonNull Fragment fragment) {
        Fragment poppingFrag = getCurrentFrag();

        if (poppingFrag != null) {
            FragmentTransaction ft = createTransactionWithOptions(null);

            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                fragmentStack.pop();
            }

            ft.replace(mContainerId, fragment, generateTag(fragment));

            ft.commitAllowingStateLoss();

            executePendingTransactions();

            fragmentStack.push(fragment);
            mCurrentFrag = fragment;

            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag, TransactionType.REPLACE);
            }
        }
    }

    @NonNull
    @CheckResult
    private Fragment getRootFragment(int index) throws IllegalStateException {
        Fragment fragment = null;
        if (!mFragmentStacks.get(index).isEmpty()) {
            fragment = mFragmentStacks.get(index).peek();
        } else if (mRootFragmentListener != null) {
            fragment = mRootFragmentListener.getRootFragment(index);

            if (mSelectedTabIndex != NO_TAB) {
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }
        }
        if (fragment == null) {
            throw new IllegalStateException("getRootFragment null !");
        }
        return fragment;
    }

    @Nullable
    private Fragment reattachPreviousFragment(@NonNull FragmentTransaction ft) {
        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
        Fragment fragment = null;
        if (!fragmentStack.isEmpty()) {
            fragment = mFragmentManager.findFragmentByTag(fragmentStack.peek().getTag());
            if (fragment != null) {
                ft.show(fragment);
                fragment.onResume();
            }
        }
        return fragment;
    }

    private void detachCurrentFragment(@NonNull FragmentTransaction ft) {
        Fragment oldFrag = getCurrentFrag();
        if (oldFrag != null) {
            oldFrag.onPause();
            ft.hide(oldFrag);
        }
    }

    @Nullable
    @CheckResult
    public Fragment getCurrentFrag() {
        if (mCurrentFrag != null) {
            return mCurrentFrag;
        } else if (mSelectedTabIndex == NO_TAB) {
            return null;
        } else {
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                mCurrentFrag = mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).peek().getTag());
            }
        }
        return mCurrentFrag;
    }

    private void executePendingTransactions() {
        if (!mExecutingTransaction) {
            mExecutingTransaction = true;
           /* if (containerFragment == null) {
                mFragmentManager.executePendingTransactions();
            }*/
            mExecutingTransaction = false;
        }
    }

    public void clearFragmentManager() {
        if (mFragmentManager.getFragments() != null) {
            FragmentTransaction ft = createTransactionWithOptions(null);
            for (Fragment fragment : mFragmentManager.getFragments()) {
                if (fragment != null) {
                    if (containerFragment != null && containerFragment == fragment) {
                        continue;
                    }
                    ft.remove(fragment);
                }
            }
            ft.commitAllowingStateLoss();
            executePendingTransactions();
        }
    }

    @CheckResult
    private FragmentTransaction createTransactionWithOptions(@Nullable FragNavTransactionOptions transactionOptions) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (transactionOptions == null) {
            transactionOptions = mDefaultTransactionOptions;
        }
        if (transactionOptions != null) {

            ft.setCustomAnimations(transactionOptions.enterAnimation, transactionOptions.exitAnimation, transactionOptions.popEnterAnimation, transactionOptions.popExitAnimation);
            ft.setTransitionStyle(transactionOptions.transitionStyle);

            ft.setTransition(transactionOptions.transition);


            if (transactionOptions.sharedElements != null) {
                for (Pair<View, String> sharedElement : transactionOptions.sharedElements) {
                    ft.addSharedElement(sharedElement.first, sharedElement.second);
                }
            }

            if (transactionOptions.breadCrumbTitle != null) {
                ft.setBreadCrumbTitle(transactionOptions.breadCrumbTitle);
            }

            if (transactionOptions.breadCrumbShortTitle != null) {
                ft.setBreadCrumbShortTitle(transactionOptions.breadCrumbShortTitle);

            }
        }
        return ft;
    }

    @CheckResult
    public int getSize() {
        return mFragmentStacks.size();
    }

    @SuppressWarnings("unchecked")
    @CheckResult
    @Nullable
    public Stack<Fragment> getStack(int index) {
        if (index == NO_TAB) return null;
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Can't getBacSiList an index that's larger than we've setup");
        }
        return (Stack<Fragment>) mFragmentStacks.get(index).clone();
    }

    @SuppressWarnings("unchecked")
    @CheckResult
    @Nullable
    public Stack<Fragment> getCurrentStack() {
        return getStack(mSelectedTabIndex);
    }

    @CheckResult
    public int getCurrentStackIndex() {
        return mSelectedTabIndex;
    }

    @CheckResult
    public boolean isRootFragment() {
        Stack<Fragment> stack = getCurrentStack();

        return stack == null || stack.size() == 1;
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_SELECTED_TAB_INDEX, mSelectedTabIndex);

        if (mCurrentFrag != null) {
            outState.putString(EXTRA_CURRENT_FRAGMENT, mCurrentFrag.getTag());
        }

        try {
            final JSONArray stackArrays = new JSONArray();

            for (Stack<Fragment> stack : mFragmentStacks) {
                final JSONArray stackArray = new JSONArray();

                for (Fragment fragment : stack) {
                    stackArray.put(generateTag(fragment));
                }

                stackArrays.put(stackArray);
            }

            outState.putString(EXTRA_FRAGMENT_STACK, stackArrays.toString());
        } catch (Throwable ignored) {
        }
    }

    private boolean restoreFromBundle(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return false;
        }

        mCurrentFrag = mFragmentManager.findFragmentByTag(savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT));

        try {
            final JSONArray stackArrays = new JSONArray(savedInstanceState.getString(EXTRA_FRAGMENT_STACK));

            for (int x = 0; x < stackArrays.length(); x++) {
                final JSONArray stackArray = stackArrays.getJSONArray(x);
                final Stack<Fragment> stack = new Stack<>();

                if (stackArray.length() == 1) {
                    final String tag = stackArray.getString(0);
                    final Fragment fragment;

                    if (tag == null || "null".equalsIgnoreCase(tag)) {
                        fragment = getRootFragment(x);
                    } else {
                        fragment = mFragmentManager.findFragmentByTag(tag);
                    }

                    if (fragment != null) {
                        stack.add(fragment);
                    }
                } else {
                    for (int y = 0; y < stackArray.length(); y++) {
                        final String tag = stackArray.getString(y);

                        if (tag != null && !"null".equalsIgnoreCase(tag)) {
                            final Fragment fragment = mFragmentManager.findFragmentByTag(tag);

                            if (fragment != null) {
                                stack.add(fragment);
                            }
                        }
                    }
                }

                mFragmentStacks.add(stack);
            }
            int tabIndex = savedInstanceState.getInt(EXTRA_SELECTED_TAB_INDEX);
            if (tabIndex <= numberTab) {
                switchTab(tabIndex);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public void setmRootFragmentListener(@Nullable RootFragmentListener mRootFragmentListener, int numberTab) {
        this.mRootFragmentListener = mRootFragmentListener;
        this.numberTab = numberTab;
        mFragmentStacks = new ArrayList<>(numberTab);
        for (int i = 0; i < numberTab; i++) {
            mFragmentStacks.add(new Stack<>());
        }
    }

    public void setNewFrame(int mContainerId, Fragment fragment) {
        containerFragment = null;
        mRootFragmentListener = null;
        mSelectedTabIndex = 0;
        mCurrentFrag = null;
        this.mContainerId = mContainerId;
        numberTab = 1;
        mFragmentStacks = new ArrayList<>(numberTab);

        Stack<Fragment> stack = new Stack<>();
        stack.add(fragment);
        mFragmentStacks.add(stack);

        clearFragmentManager();
        clearDialogFragment();

        FragmentTransaction ft = createTransactionWithOptions(null);
        ft.replace(mContainerId, fragment, generateTag(fragment));
        ft.commitAllowingStateLoss();
        executePendingTransactions();

        this.currentRootFragment = fragment;

        if (mTransactionListener != null) {
            mTransactionListener.onFragmentTransaction(currentRootFragment, TransactionType.REPLACE);
        }
    }

    public void setFragmentContainer(int mContainerId, @Nullable Fragment containerFragment) {
        mSelectedTabIndex = NO_TAB;
        this.mContainerId = mContainerId;
        this.containerFragment = containerFragment;
    }

    public Fragment getCurrentRootFragment() {
        return currentRootFragment;
    }

    public enum TransactionType {
        PUSH,
        POP,
        REPLACE
    }

    @IntDef({FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, FragmentTransaction.TRANSIT_FRAGMENT_CLOSE, FragmentTransaction.TRANSIT_FRAGMENT_FADE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Transit {
    }

    public interface RootFragmentListener {

        Fragment getRootFragment(int index);
    }

    public interface TransactionListener {

        void onTabTransaction(Fragment fragment, int index);

        void onFragmentTransaction(Fragment fragment, TransactionType transactionType);

    }

    public static final class Builder {
        private final FragmentManager mFragmentManager;
        private final Bundle mSavedInstanceState;
        private TransactionListener mTransactionListener;
        private FragNavTransactionOptions mDefaultTransactionOptions;
        private int mContaierId;

        public Builder(@Nullable Bundle savedInstanceState, FragmentManager mFragmentManager) {
            this.mSavedInstanceState = savedInstanceState;
            this.mFragmentManager = mFragmentManager;
        }

        public Builder setmContaierId(int mContaierId) {
            this.mContaierId = mContaierId;
            return this;
        }

        public Builder defaultTransactionOptions(@NonNull FragNavTransactionOptions transactionOptions) {
            mDefaultTransactionOptions = transactionOptions;
            return this;
        }

        public Builder transactionListener(TransactionListener val) {
            mTransactionListener = val;
            return this;
        }

        public FragNavController build() {
            return new FragNavController(this, mSavedInstanceState);
        }
    }
}
