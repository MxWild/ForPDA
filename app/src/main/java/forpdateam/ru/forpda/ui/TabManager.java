package forpdateam.ru.forpda.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.ui.fragments.TabFragment;
import forpdateam.ru.forpda.ui.views.DetailsTransition;

public class TabManager {
    private final static String LOG_TAG = TabManager.class.getSimpleName();
    private final static String TAB_PREFIX = "tab_";
    private final static String BUNDLE_PREFIX = "tab_manager_";
    private final static String BUNDLE_ACTIVE_TAG = "active_tag";
    private final static String BUNDLE_ACTIVE_INDEX = "active_index";
    private static TabManager instance;
    private FragmentManager fragmentManager;
    private TabListener tabListener;
    private static String activeTag = "";
    private static int activeIndex = 0;
    private List<TabFragment> existingFragments = new ArrayList<>();

    public interface TabListener {
        void onAddTab(TabFragment fragment);

        void onRemoveTab(TabFragment fragment);

        void onSelectTab(TabFragment fragment);

        void onChange();
    }

    public static TabManager init(AppCompatActivity activity, TabListener listener) {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
        instance = new TabManager(activity, listener);
        return instance;
    }

    private void clear() {
        fragmentManager = null;
        tabListener = null;
        existingFragments.clear();
        existingFragments = null;
    }

    public static TabManager get() {
        return instance;
    }

    public TabManager(AppCompatActivity activity, TabListener listener) {
        fragmentManager = activity.getSupportFragmentManager();
        tabListener = listener;
        updateFragmentList();
    }

    public void saveState(Bundle outState) {
        if (outState == null) return;
        outState.putString(BUNDLE_PREFIX.concat(BUNDLE_ACTIVE_TAG), activeTag);
        outState.putInt(BUNDLE_PREFIX.concat(BUNDLE_ACTIVE_INDEX), activeIndex);
        Log.d(LOG_TAG, "saveState: " + activeTag + " : " + activeIndex);
    }

    public void loadState(Bundle state) {
        if (state == null) return;
        activeTag = state.getString(BUNDLE_PREFIX.concat(BUNDLE_ACTIVE_TAG), "");
        activeIndex = state.getInt(BUNDLE_PREFIX.concat(BUNDLE_ACTIVE_INDEX), 0);
        Log.d(LOG_TAG, "loadState: " + activeTag + " : " + activeIndex);
    }

    public int getSize() {
        return existingFragments.size();
    }

    public boolean isEmpty() {
        return getSize() == 0;
    }

    public static String getActiveTag() {
        return activeTag;
    }

    public static int getActiveIndex() {
        return activeIndex;
    }

    public List<TabFragment> getFragments() {
        return existingFragments;
    }

    public void updateFragmentList() {
        Log.d(LOG_TAG, "updateFragmentList");
        existingFragments.clear();
        if (fragmentManager.getFragments() == null) return;
        for (int i = 0; i < fragmentManager.getFragments().size(); i++) {
            if (fragmentManager.getFragments().get(i) != null) {
                Log.d(LOG_TAG, "update fragment " + fragmentManager.getFragments().get(i));
                existingFragments.add((TabFragment) fragmentManager.getFragments().get(i));
            }
        }
        Collections.sort(existingFragments, (o1, o2) -> o1.getTag().compareTo(o2.getTag()));
    }

    private void hideTabs(FragmentTransaction transaction) {
        for (Fragment fragment : existingFragments) {
            if (fragment != null && !fragment.isHidden()) {
                transaction.hide(fragment);
                fragment.onPause();
            }
        }
    }

    private TabFragment findTabByTag(String tag) {
        if (tag == null) return null;
        for (TabFragment tab : existingFragments)
            if (tab.getTag().equals(tag))
                return tab;
        return null;
    }

    public TabFragment getActive() {
        TabFragment active = null;
        try {
            active = get(activeIndex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return active;
    }

    public TabFragment get(final int index) {
        return existingFragments.get(index);
    }

    public TabFragment get(final String tag) {
        if (tag == null) return null;
        for (Fragment fragment : existingFragments)
            if (fragment.getTag().equals(tag))
                return (TabFragment) fragment;
        return null;
    }

    public void add(Class<? extends TabFragment> tClass) {
        add(tClass, null);
    }

    public void add(Class<? extends TabFragment> tClass, Bundle args) {
        TabFragment.Builder builder = new TabFragment.Builder<>(tClass);
        if (args != null) {
            builder.setArgs(args);
        }
        TabFragment fragment = builder.build();
        add(fragment);
    }

    public void add(TabFragment tabFragment) {
        Log.d(LOG_TAG, "add: " + tabFragment);
        if (tabFragment == null)
            return;
        String check = null;
        if (tabFragment.getConfiguration().isAlone()) {
            check = getTagContainClass(tabFragment.getClass());
        }
        if (check != null) {
            select(check);
            return;
        }

        activeTag = TAB_PREFIX.concat(Long.toString(System.currentTimeMillis()));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        hideTabs(transaction);
        //transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.add(R.id.fragments_container, tabFragment, activeTag).commit();
        fragmentManager.executePendingTransactions();
        updateFragmentList();
        activeIndex = existingFragments.indexOf(tabFragment);
        tabListener.onChange();
        tabListener.onAddTab(tabFragment);
    }

    public void add(TabFragment tabFragment, View sharedElement, Fragment fragment) {
        if (tabFragment == null)
            return;
        String check = null;
        if (tabFragment.getConfiguration().isAlone()) {
            check = getTagContainClass(tabFragment.getClass());
        }
        if (check != null) {
            select(check);
            return;
        }

        activeTag = TAB_PREFIX.concat(Long.toString(System.currentTimeMillis()));
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        hideTabs(transaction);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tabFragment.setSharedElementEnterTransition(new DetailsTransition());
            tabFragment.setEnterTransition(new Fade());
            fragment.setExitTransition(new Fade());
            tabFragment.setSharedElementReturnTransition(new DetailsTransition());
        }

        transaction.addSharedElement(sharedElement, "detailsCover");
        transaction.add(R.id.fragments_container, tabFragment, activeTag).commit();
        fragmentManager.executePendingTransactions();
        updateFragmentList();
        activeIndex = existingFragments.indexOf(tabFragment);
        tabListener.onChange();
        tabListener.onAddTab(tabFragment);
    }

    public String getTagContainClass(final Class aClass) {
        String className = aClass.getSimpleName();
        for (TabFragment fragment : existingFragments) {
            if (fragment.getClass().getSimpleName().equals(className)) return fragment.getTag();
        }
        return null;
    }

    public TabFragment getByClass(final Class aClass) {
        String className = aClass.getSimpleName();
        for (TabFragment fragment : existingFragments) {
            if (fragment.getClass().getSimpleName().equals(className))
                return fragment;
        }
        return null;
    }

    public void remove(final String tag) {
        remove(get(tag));
    }

    public void remove(TabFragment tabFragment) {
        Log.d(LOG_TAG, "remove: " + tabFragment);
        if (tabFragment == null)
            return;

        String tabTag = tabFragment.getTag();
        int tabIndex = existingFragments.indexOf(tabFragment);
        fragmentManager.beginTransaction().remove(tabFragment).commit();
        fragmentManager.executePendingTransactions();
        updateFragmentList();

        TabFragment parent = null;
        if (tabFragment.getParentTag() != null && !tabFragment.getParentTag().equals(""))
            parent = findTabByTag(tabFragment.getParentTag());

        if (parent == null) {
            if (existingFragments.size() >= 1) {
                if (existingFragments.size() <= activeIndex)
                    activeIndex = existingFragments.size() - 1;

                activeTag = existingFragments.get(activeIndex).getTag();
            } else {
                activeIndex = 0;
                activeTag = "";
            }
        } else {
            Log.e(LOG_TAG, "Compare " + activeTag + " : " +tabTag);
            if (activeTag.equals(tabTag)) {
                activeTag = tabFragment.getParentTag();
                activeIndex = existingFragments.indexOf(parent);
            }
        }

        select(activeTag);
        tabListener.onChange();
        tabListener.onRemoveTab(tabFragment);
    }

    public void select(final String tag) {
        select(get(tag));
    }

    public void select(TabFragment tabFragment) {
        Log.d(LOG_TAG, "select: " + tabFragment);
        if (tabFragment == null)
            return;
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        hideTabs(transaction);
        transaction.show(tabFragment).commit();
        tabFragment.onResume();
        fragmentManager.executePendingTransactions();
        updateFragmentList();
        activeTag = tabFragment.getTag();
        activeIndex = existingFragments.indexOf(tabFragment);
        tabListener.onChange();
        tabListener.onSelectTab(tabFragment);
    }

    public void notifyTabDataChanged() {
        tabListener.onChange();
    }
}
