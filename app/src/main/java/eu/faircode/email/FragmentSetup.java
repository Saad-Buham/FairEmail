package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2021 by Marcel Bokhorst (M66B)
*/

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.List;

public class FragmentSetup extends FragmentBase {
    private ViewGroup view;

    private ImageButton ibHelp;
    private Button btnQuick;
    private TextView tvQuickNew;

    private ImageButton ibManual;
    private TextView tvManual;

    private Button btnAccount;

    private Button btnIdentity;
    private TextView tvExchangeSupport;
    private TextView tvIdentityWhat;
    private TextView tvFree;
    private TextView tvNoComposable;

    private TextView tvPermissionsDone;
    private Button btnPermissions;

    private TextView tvDozeDone;
    private Button btnDoze;

    private Button btnBackgroundRestricted;
    private Button btnDataSaver;

    private TextView tvBatteryUsage;
    private TextView tvSyncStopped;

    private Button btnInbox;

    private Group grpManual;
    private Group grpDoze;
    private Group grpBackgroundRestricted;
    private Group grpDataSaver;

    private int textColorPrimary;
    private int colorWarning;
    private Drawable check;

    private boolean manual = false;

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.title_setup);

        if (savedInstanceState == null) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent.hasExtra("manual")) {
                    manual = intent.getBooleanExtra("manual", false);
                    intent.removeExtra("manual");
                    activity.setIntent(intent);
                }
            }
        } else
            manual = savedInstanceState.getBoolean("fair:manual");

        textColorPrimary = Helper.resolveColor(getContext(), android.R.attr.textColorPrimary);
        colorWarning = Helper.resolveColor(getContext(), R.attr.colorWarning);
        check = getContext().getDrawable(R.drawable.twotone_check_24);

        view = (ViewGroup) inflater.inflate(R.layout.fragment_setup, container, false);

        // Get controls

        ibHelp = view.findViewById(R.id.ibHelp);
        btnQuick = view.findViewById(R.id.btnQuick);
        tvQuickNew = view.findViewById(R.id.tvQuickNew);

        ibManual = view.findViewById(R.id.ibManual);
        tvManual = view.findViewById(R.id.tvManual);

        btnAccount = view.findViewById(R.id.btnAccount);

        btnIdentity = view.findViewById(R.id.btnIdentity);
        tvExchangeSupport = view.findViewById(R.id.tvExchangeSupport);
        tvIdentityWhat = view.findViewById(R.id.tvIdentityWhat);
        tvFree = view.findViewById(R.id.tvFree);
        tvNoComposable = view.findViewById(R.id.tvNoComposable);

        tvPermissionsDone = view.findViewById(R.id.tvPermissionsDone);
        btnPermissions = view.findViewById(R.id.btnPermissions);

        tvDozeDone = view.findViewById(R.id.tvDozeDone);
        btnDoze = view.findViewById(R.id.btnDoze);

        btnBackgroundRestricted = view.findViewById(R.id.btnBackgroundRestricted);
        btnDataSaver = view.findViewById(R.id.btnDataSaver);

        tvBatteryUsage = view.findViewById(R.id.tvBatteryUsage);
        tvSyncStopped = view.findViewById(R.id.tvSyncStopped);

        btnInbox = view.findViewById(R.id.btnInbox);

        grpManual = view.findViewById(R.id.grpManual);
        grpDoze = view.findViewById(R.id.grpDoze);
        grpBackgroundRestricted = view.findViewById(R.id.grpBackgroundRestricted);
        grpDataSaver = view.findViewById(R.id.grpDataSaver);

        // Wire controls

        ibHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putString("name", "SETUP.md");
                FragmentDialogMarkdown fragment = new FragmentDialogMarkdown();
                fragment.setArguments(args);
                fragment.show(getChildFragmentManager(), "help");
            }
        });

        btnQuick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(getContext(), getViewLifecycleOwner(), btnQuick);

                int order = 1;
                popupMenu.getMenu().add(Menu.NONE, R.string.title_setup_gmail, order++, R.string.title_setup_gmail);
                popupMenu.getMenu().add(Menu.NONE, R.string.title_setup_outlook, order++, R.string.title_setup_outlook);

                for (EmailProvider provider : EmailProvider.loadProfiles(getContext()))
                    if (provider.oauth != null &&
                            (provider.oauth.enabled || BuildConfig.DEBUG))
                        popupMenu.getMenu()
                                .add(Menu.NONE, -1, order++, getString(R.string.title_setup_oauth, provider.name))
                                .setIntent(new Intent(ActivitySetup.ACTION_QUICK_OAUTH)
                                        .putExtra("id", provider.id)
                                        .putExtra("name", provider.name)
                                        .putExtra("askAccount", provider.oauth.askAccount));

                //popupMenu.getMenu().add(Menu.NONE, R.string.title_setup_activesync, order++, R.string.title_setup_activesync);
                popupMenu.getMenu().add(Menu.NONE, R.string.title_setup_other, order++, R.string.title_setup_other);

                SpannableString ss = new SpannableString(getString(R.string.title_setup_pop3));
                ss.setSpan(new RelativeSizeSpan(0.9f), 0, ss.length(), 0);
                popupMenu.getMenu().add(Menu.NONE, R.string.title_setup_pop3, order++, ss);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
                        int itemId = item.getItemId();
                        if (itemId == R.string.title_setup_gmail) {
                            if (Helper.hasValidFingerprint(getContext()) || BuildConfig.DEBUG)
                                lbm.sendBroadcast(new Intent(ActivitySetup.ACTION_QUICK_GMAIL));
                            else
                                ToastEx.makeText(getContext(), R.string.title_setup_gmail_support, Toast.LENGTH_LONG).show();
                            return true;
                        } else if (itemId == R.string.title_setup_activesync) {
                            Helper.viewFAQ(getContext(), 133);
                            return true;
                        } else if (itemId == R.string.title_setup_outlook || itemId == R.string.title_setup_other) {
                            lbm.sendBroadcast(new Intent(ActivitySetup.ACTION_QUICK_SETUP));
                            return true;
                        } else if (itemId == R.string.title_setup_pop3) {
                            lbm.sendBroadcast(new Intent(ActivitySetup.ACTION_QUICK_POP3));
                            return true;
                        }
                        if (item.getIntent() == null)
                            return false;
                        else {
                            if (Helper.hasValidFingerprint(getContext()) || BuildConfig.DEBUG)
                                lbm.sendBroadcast(item.getIntent());
                            else
                                ToastEx.makeText(getContext(), R.string.title_setup_oauth_permission, Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }
                });

                popupMenu.show();
            }
        });

        tvQuickNew.setPaintFlags(tvQuickNew.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvQuickNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 112);
            }
        });

        ibManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manual = !manual;
                ibManual.setImageLevel(manual ? 0 /* less */ : 1 /* more */);
                grpManual.setVisibility(manual ? View.VISIBLE : View.GONE);
            }
        });

        tvManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibManual.setPressed(true);
                ibManual.setPressed(false);
                ibManual.performClick();
            }
        });

        ibManual.setImageLevel(manual ? 0 /* less */ : 1 /* more */);
        grpManual.setVisibility(manual ? View.VISIBLE : View.GONE);

        btnAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
                lbm.sendBroadcast(new Intent(ActivitySetup.ACTION_VIEW_ACCOUNTS));
            }
        });

        btnIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
                lbm.sendBroadcast(new Intent(ActivitySetup.ACTION_VIEW_IDENTITIES));
            }
        });

        tvExchangeSupport.setPaintFlags(tvExchangeSupport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvExchangeSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 8);
            }
        });

        tvIdentityWhat.setPaintFlags(tvIdentityWhat.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvIdentityWhat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 9);
            }
        });

        tvFree.setPaintFlags(tvFree.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 19);
            }
        });

        btnPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnPermissions.setEnabled(false);
                String permission = Manifest.permission.READ_CONTACTS;
                requestPermissions(new String[]{permission}, ActivitySetup.REQUEST_PERMISSION);
            }
        });

        btnDoze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FragmentDialogDoze().show(getParentFragmentManager(), "setup:doze");
            }
        });

        tvBatteryUsage.setPaintFlags(tvBatteryUsage.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvBatteryUsage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 39);
            }
        });

        tvSyncStopped.setPaintFlags(tvSyncStopped.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvSyncStopped.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 16);
            }
        });

        PackageManager pm = getContext().getPackageManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final Intent settings = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID));

            btnBackgroundRestricted.setEnabled(settings.resolveActivity(pm) != null); // system whitelisted
            btnBackgroundRestricted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(settings);
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final Intent settings = new Intent(
                    Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID));

            btnDataSaver.setEnabled(settings.resolveActivity(pm) != null); // system whitelisted
            btnDataSaver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(settings);
                }
            });
        }

        btnInbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FragmentBase) getParentFragment()).finish();
            }
        });

        // Initialize
        btnIdentity.setEnabled(false);
        tvNoComposable.setVisibility(View.GONE);

        tvPermissionsDone.setText(null);
        tvPermissionsDone.setCompoundDrawables(null, null, null, null);

        tvDozeDone.setText(null);
        tvDozeDone.setCompoundDrawables(null, null, null, null);
        btnDoze.setEnabled(false);

        btnInbox.setEnabled(false);

        grpDoze.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? View.GONE : View.VISIBLE);
        grpBackgroundRestricted.setVisibility(View.GONE);
        grpDataSaver.setVisibility(View.GONE);

        setContactsPermission(hasPermission(Manifest.permission.READ_CONTACTS));

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("fair:manual", manual);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final DB db = DB.getInstance(getContext());
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Create outbox
        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) {
                DB db = DB.getInstance(context);
                try {
                    db.beginTransaction();

                    EntityFolder outbox = db.folder().getOutbox();
                    if (outbox == null) {
                        outbox = EntityFolder.getOutbox();
                        outbox.id = db.folder().insertFolder(outbox);
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return null;
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.execute(this, new Bundle(), "outbox:create");

        db.account().liveSynchronizingAccounts().observe(getViewLifecycleOwner(), new Observer<List<EntityAccount>>() {
            private boolean done = false;

            @Override
            public void onChanged(@Nullable List<EntityAccount> accounts) {
                done = (accounts != null && accounts.size() > 0);

                btnIdentity.setEnabled(done);
                btnInbox.setEnabled(done);

                prefs.edit().putBoolean("has_accounts", done).apply();
            }
        });

        db.identity().liveComposableIdentities().observe(getViewLifecycleOwner(), new Observer<List<TupleIdentityEx>>() {
            @Override
            public void onChanged(@Nullable List<TupleIdentityEx> identities) {
                boolean done = (identities != null && identities.size() > 0);
                tvNoComposable.setVisibility(done ? View.GONE : View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Doze
        Boolean ignoring = Helper.isIgnoringOptimizations(getContext());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            btnDoze.setEnabled(false);
        else {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            PackageManager pm = getContext().getPackageManager();
            if (intent.resolveActivity(pm) == null)
                btnDoze.setEnabled(false);
            else
                btnDoze.setEnabled((ignoring != null && !ignoring) || BuildConfig.DEBUG);
        }

        tvDozeDone.setText(ignoring == null || ignoring ? R.string.title_setup_done : R.string.title_setup_to_do);
        tvDozeDone.setTextColor(ignoring == null || ignoring ? textColorPrimary : colorWarning);
        tvDozeDone.setTypeface(null, ignoring == null || ignoring ? Typeface.NORMAL : Typeface.BOLD);
        tvDozeDone.setCompoundDrawablesWithIntrinsicBounds(ignoring == null || ignoring ? check : null, null, null, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            grpBackgroundRestricted.setVisibility(am.isBackgroundRestricted() ? View.VISIBLE : View.GONE);
        }

        // https://developer.android.com/training/basics/network-ops/data-saver.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                int status = cm.getRestrictBackgroundStatus();
                grpDataSaver.setVisibility(
                        status == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
                                ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++)
            if (Manifest.permission.READ_CONTACTS.equals(permissions[i]))
                setContactsPermission(grantResults[i] == PackageManager.PERMISSION_GRANTED);
    }

    private void setContactsPermission(boolean granted) {
        if (granted)
            ContactInfo.init(getContext());

        tvPermissionsDone.setText(granted ? R.string.title_setup_done : R.string.title_setup_to_do);
        tvPermissionsDone.setTextColor(granted ? textColorPrimary : colorWarning);
        tvPermissionsDone.setTypeface(null, granted ? Typeface.NORMAL : Typeface.BOLD);
        tvPermissionsDone.setCompoundDrawablesWithIntrinsicBounds(granted ? check : null, null, null, null);
        btnPermissions.setEnabled(!granted);
    }

    public static class FragmentDialogDoze extends FragmentDialogBase {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setMessage(R.string.title_setup_doze_instructions)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                            } catch (Throwable ex) {
                                Log.e(ex);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }
}
