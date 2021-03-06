package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.tasks.EFIDroidInstallServiceTask;
import org.efidroid.efidroidmanager.tasks.EFIDroidUninstallServiceTask;
import org.efidroid.efidroidmanager.types.InstallationEntry;
import org.efidroid.efidroidmanager.types.InstallationStatus;
import org.efidroid.efidroidmanager.types.ProgressReceiver;
import org.efidroid.efidroidmanager.view.ProgressCircle;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class InstallFragment extends Fragment implements AppBarLayout.OnOffsetChangedListener, ProgressReceiver.OnStatusChangeListener {
    // listener
    private OnInstallFragmentInteractionListener mListener;

    // UI
    private ProgressReceiver mProgressReceiver;
    private ProgressCircle mProgressCircle;
    private TextView mProgressDescription;
    private ArrayList<InstallStatusRecyclerViewAdapter.Item> mListData = new ArrayList<>();
    private InstallStatusRecyclerViewAdapter mListAdapter;

    private int getColor(int id) {
        return ResourcesCompat.getColor(getResources(), id, getContext().getTheme());
    }

    private InstallStatusLoadCallback mInstallStatusLoadCallback = new InstallStatusLoadCallback() {
        @Override
        public void onStatusLoaded() {
            loadUiData(true);
        }

        @Override
        public void onStatusLoadError() {
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgError), false, 0);
            mProgressCircle.setContentText(R.string.error);
            mProgressDescription.setText(R.string.cant_reload_install_status);
            mProgressCircle.setClickable(true);
        }
    };

    private void startReload() {
        mProgressCircle.setProgressHidden(true, true, 200);
        mProgressCircle.setFillColor(getColor(R.color.colorCircleBgLoading), true, 200);
        mProgressCircle.setClickable(false);
        mProgressCircle.setContentText(R.string.reloading);
        mProgressDescription.setText(R.string.reloading_info);
        mListener.reloadInstallStatus(mInstallStatusLoadCallback);
    }

    public InstallFragment() {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_install, menu);
    }

    void startInstallService() {
        Bundle extras = new Bundle();
        extras.putParcelable(EFIDroidInstallServiceTask.ARG_DEVICE_INFO, mListener.getDeviceInfo());
        extras.putParcelable(EFIDroidInstallServiceTask.ARG_INSTALL_STATUS, mListener.getInstallStatus());
        mProgressReceiver.setServiceBundle(extras);
        mProgressReceiver.setServiceHandler(EFIDroidInstallServiceTask.class);
        mProgressReceiver.startService();
    }

    void startUnInstallService() {
        Bundle extras = new Bundle();
        extras.putParcelable(EFIDroidInstallServiceTask.ARG_DEVICE_INFO, mListener.getDeviceInfo());
        extras.putParcelable(EFIDroidInstallServiceTask.ARG_INSTALL_STATUS, mListener.getInstallStatus());
        mProgressReceiver.setServiceBundle(extras);
        mProgressReceiver.setServiceHandler(EFIDroidUninstallServiceTask.class);
        mProgressReceiver.startService();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                startReload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadListData() {
        mListData.clear();

        InstallationEntry entry = mListener.getInstallStatus().getWorkingEntry();
        if (entry != null) {
            Date date = new Date(entry.getTimeStamp() * 1000L);
            DateFormat format = DateFormat.getDateTimeInstance();

            mListData.add(new InstallStatusRecyclerViewAdapter.Item(getString(R.string.efidroid_version), entry.getEFIDroidReleaseVersionString()));
            mListData.add(new InstallStatusRecyclerViewAdapter.Item(getString(R.string.build_time), format.format(date)));
            mListData.add(new InstallStatusRecyclerViewAdapter.Item(getString(R.string.efi_spec), entry.getEfiSpecVersionMajor() + "." + entry.getEfiSpecVersionMinor()));
        }

        mListAdapter.notifyDataSetChanged();
    }

    private View.OnClickListener mInstallClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mProgressReceiver.isFinished()) {
                mListener.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mProgressCircle.setClickable(false);
                mProgressCircle.setValue(0, false, 0);
                mProgressCircle.setProgressHidden(false, true, 200);
                mProgressCircle.setFillColor(getColor(R.color.colorCircleBgLoading), true, 200);

                Util.animateVisibility(mListener.getFAB(), View.VISIBLE, 200);
                mListener.getFAB().setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_cancel, getActivity().getTheme()));
                mListener.getFAB().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GenericProgressIntentService.stopCurrentTask(getContext(), GenericProgressIntentService.class);
                    }
                });

                startInstallService();
            }
        }
    };

    private View.OnClickListener mUinstallClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mProgressReceiver.isFinished()) {
                mListener.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mProgressCircle.setClickable(false);
                mProgressCircle.setValue(0, false, 0);
                mProgressCircle.setProgressHidden(false, true, 200);
                mProgressCircle.setFillColor(getColor(R.color.colorCircleBgLoading), true, 200);
                mProgressCircle.setContentText(R.string.uninstall);
                mProgressDescription.setText("");

                Util.animateVisibility(mListener.getFAB(), View.VISIBLE, 200);
                mListener.getFAB().setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_cancel, getActivity().getTheme()));
                mListener.getFAB().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GenericProgressIntentService.stopCurrentTask(getContext(), GenericProgressIntentService.class);
                    }
                });

                startUnInstallService();
            }
        }
    };

    private void loadUiData(boolean animate) {
        // progress circle
        InstallationStatus installStatus = mListener.getInstallStatus();

        mProgressCircle.setOnClickListener(mInstallClickListener);
        mProgressCircle.setClickable(true);

        if (!installStatus.isInstalled()) {
            // not installed
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgError), animate, 200);
            mProgressCircle.setContentText(R.string.install);
            mProgressDescription.setText("");
            mListener.getFAB().setVisibility(View.GONE);
        } else if (installStatus.isBroken()) {
            // hide FAB
            FloatingActionButton fab = mListener.getFAB();
            fab.setVisibility(View.GONE);

            // broken
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgError), animate, 200);
            mProgressCircle.setContentText(R.string.repair);

            ArrayList<InstallationEntry> installEntries = new ArrayList<>();
            installEntries.addAll(installStatus.getInstallationEntries());

            // remove 'OK' entries
            Iterator<InstallationEntry> iterator = installEntries.iterator();
            while (iterator.hasNext()) {
                InstallationEntry entry = iterator.next();

                int status = entry.getStatus();
                if (status == InstallationEntry.STATUS_OK)
                    iterator.remove();
            }

            String text = "";
            for (int i = 0; i < installEntries.size(); i++) {
                InstallationEntry entry = installEntries.get(i);
                int status = entry.getStatus();
                if (status == InstallationEntry.STATUS_OK)
                    continue;

                if (i != 0 && i == installEntries.size() - 1)
                    text += " " + getString(R.string.and) + " ";
                else if (i != 0) {
                    text += ", ";
                }

                text += entry.getFsTabEntry().getMountPoint().substring(1) + " ";

                if (status == InstallationEntry.STATUS_ESP_ONLY)
                    text += getString(R.string.status_esp_only);

                else if (status == InstallationEntry.STATUS_ESP_MISSING)
                    text += getString(R.string.status_esp_missing);

                else if (status == InstallationEntry.STATUS_WRONG_DEVICE)
                    text += getString(R.string.status_wrong_device);
                else if (status == InstallationEntry.STATUS_NOT_INSTALLED)
                    text += getString(R.string.status_not_installed);
            }

            mProgressDescription.setText(text);
        } else if (installStatus.isUpdateAvailable()) {
            DateFormat format = DateFormat.getDateTimeInstance();

            // show FAB
            FloatingActionButton fab = mListener.getFAB();
            fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_delete, getActivity().getTheme()));
            fab.setOnClickListener(mUinstallClickListener);
            fab.setVisibility(View.VISIBLE);

            // installed
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgWarning), animate, 200);
            mProgressCircle.setContentText(R.string.update);
            mProgressDescription.setText(format.format(installStatus.getUpdateDate()));
        } else {
            // show FAB
            FloatingActionButton fab = mListener.getFAB();
            fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_delete, getActivity().getTheme()));
            fab.setOnClickListener(mUinstallClickListener);
            fab.setVisibility(View.VISIBLE);

            // installed
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgSuccess), animate, 200);
            mProgressCircle.setContentText(R.string.reinstall);
            mProgressDescription.setText(R.string.installed_and_updated);
        }

        loadListData();
    }

    private void initToolbar() {
        // get colors
        int colorToolBarGrey = ResourcesCompat.getColor(getResources(), R.color.colorToolBarGrey, getActivity().getTheme());
        int colorToolBarGreyDark = ResourcesCompat.getColor(getResources(), R.color.colorToolBarGreyDark, getActivity().getTheme());

        // toolbar
        CollapsingToolbarLayout collapsingToolbarLayout = mListener.getCollapsingToolbarLayout();
        collapsingToolbarLayout.setContentScrimColor(colorToolBarGrey);
        collapsingToolbarLayout.setBackgroundColor(colorToolBarGrey);
        collapsingToolbarLayout.setStatusBarScrimColor(colorToolBarGreyDark);
        collapsingToolbarLayout.setScrimVisibleHeightTrigger(-1);

        // appbar
        AppBarLayout appBarLayout = mListener.getAppBarLayout();
        Util.setToolBarHeight(appBarLayout, 300, true);
        appBarLayout.addOnOffsetChangedListener(this);

        // inflate toolbar layout
        FrameLayout toolbarFrameLayout = mListener.getToolbarFrameLayout();
        LayoutInflater inflater = LayoutInflater.from(toolbarFrameLayout.getContext());
        toolbarFrameLayout.removeAllViews();
        View toolbarView = inflater.inflate(R.layout.toolbar_layout_install, toolbarFrameLayout, true);
        mProgressCircle = (ProgressCircle) toolbarView.findViewById(R.id.progressCircle);
        mProgressDescription = (TextView) toolbarView.findViewById(R.id.description);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_install, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            mListData.clear();
            mListAdapter = new InstallStatusRecyclerViewAdapter(mListData);

            recyclerView.setAdapter(mListAdapter);
            recyclerView.setNestedScrollingEnabled(true);
        }

        // menu
        setHasOptionsMenu(true);

        // toolbar
        initToolbar();

        // create progress receiver
        mProgressReceiver = new ProgressReceiver(getContext(), this, null, EFIDroidInstallServiceTask.class, null);
        // restore status
        if (savedInstanceState != null) {
            mProgressReceiver.onRestoreInstanceState(savedInstanceState);
        }

        loadUiData(false);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInstallFragmentInteractionListener) {
            mListener = (OnInstallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInstallFragmentInteractionListener");
        }
    }

    @Override
    public void onPause() {
        mProgressReceiver.notifyPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mProgressReceiver.notifyResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.getAppBarLayout().removeOnOffsetChangedListener(this);
        mListener = null;
    }

    @Override
    public void onDestroy() {
        mProgressReceiver.notifyDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mProgressReceiver.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        FloatingActionButton fab = mListener.getFAB();
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.TOP;

        float margin_top = Util.getToolBarHeight(getContext()) + Util.dp2px(appBarLayout.getContext(), 300) - (fab.getHeight() / 2) + verticalOffset;
        layoutParams.setMargins(layoutParams.leftMargin, (int) margin_top, layoutParams.rightMargin, layoutParams.bottomMargin);
        fab.setLayoutParams(layoutParams);
    }

    @Override
    public void onStatusUpdate(int progress, String text) {
        mProgressCircle.setValue(progress, true, 100);
        mProgressCircle.setContentText(progress + "%");
        mProgressDescription.setText(text);
    }

    @Override
    public void onCompleted(boolean success) {
        mListener.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        Util.animateVisibility(mListener.getFAB(), View.GONE, 200);
        mProgressReceiver.reset();

        if (success) {
            startReload();
        } else {
            mProgressCircle.setProgressHidden(true, true, 1000);
            mProgressCircle.setFillColor(getColor(R.color.colorCircleBgError), true, 1000);
        }
    }

    public interface InstallStatusLoadCallback {
        void onStatusLoaded();

        void onStatusLoadError();
    }

    public interface OnInstallFragmentInteractionListener {
        DeviceInfo getDeviceInfo();

        InstallationStatus getInstallStatus();

        void reloadInstallStatus(InstallStatusLoadCallback callback);

        FloatingActionButton getFAB();

        Toolbar getToolbar();

        CollapsingToolbarLayout getCollapsingToolbarLayout();

        AppBarLayout getAppBarLayout();

        FrameLayout getToolbarFrameLayout();

        DrawerLayout getDrawerLayout();
    }
}
