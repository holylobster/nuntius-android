/*
 * Copyright (C) 2015 - Holy Lobster
 *
 * Nuntius is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Nuntius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Nuntius. If not, see <http://www.gnu.org/licenses/>.
 */

package org.holylobster.nuntius;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.holylobster.nuntius.bluetooth.BluetoothSocketAdapter;
import org.holylobster.nuntius.bluetooth.Connection;
import org.holylobster.nuntius.notifications.Handler;
import org.holylobster.nuntius.notifications.IncomingMessage;
import org.holylobster.nuntius.notifications.IntentRequestCodes;
import org.holylobster.nuntius.notifications.Message;
import org.holylobster.nuntius.notifications.NotificationListenerService;
import org.holylobster.nuntius.data.BlacklistedApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Server extends BroadcastReceiver implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private static final String TAG = Server.class.getSimpleName();

    private static final UUID uuidSpp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final List<Connection> connections = new ArrayList<>();
    private Thread acceptThread;
    private BluetoothServerSocket serverSocket;

    private Set<String> blacklistedApp;

    private int minNotificationPriority = Notification.PRIORITY_DEFAULT;

    private final NotificationListenerService context;

    public Server(NotificationListenerService context) {
        this.context = context;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        blacklistedApp = defaultSharedPreferences.getStringSet("BlackList", new HashSet<String>());
    }

    public static boolean bluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static boolean bluetoothAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        if (filter(sbn)) {
            sendMessage("notificationPosted", sbn);
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (filter(sbn)) {
            sendMessage("notificationRemoved", sbn);
        }
    }

    private boolean filter(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Log.d("blacklist", " " + blacklistedApp);
        return
                notification != null
                // Filter low priority notifications
                && notification.priority >= minNotificationPriority
                // Notification flags
                && !isOngoing(notification)
                && !isLocalOnly(notification)
                && !isBlacklisted(sbn);
    }

    private boolean isBlacklisted(StatusBarNotification sbn) {
        return blacklistedApp.contains(sbn.getPackageName());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private static boolean isLocalOnly(Notification notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
            return false;
        }
        boolean local = (notification.flags & Notification.FLAG_LOCAL_ONLY) != 0;
        Log.d(TAG, String.format("Notification is local: %1s", local));
        return local;

    }

    private static boolean isOngoing(Notification notification) {
        boolean ongoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        Log.d(TAG, String.format("Notification is ongoing: %1s", ongoing));
        return ongoing;
    }

    private void sendMessage(String event, StatusBarNotification sbn) {
        for (Connection connection : connections) {
            boolean queued = connection.enqueue(new Message(event, sbn));
            if (!queued) {
                Log.w(TAG, "Unable to enqueue message on connection " + connection);
            }
        }
    }

    public void start() {
        Log.i(TAG, "Server starting...");
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(this, filter);

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        boolean mustRun = defaultSharedPreferences.getBoolean("main_enable_switch", true);

        if (mustRun) {
            startThread();
        }
    }

    public void stop() {
        Log.d(TAG, "Server stopping...");

        context.unregisterReceiver(this);

        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);

        stopThread();
    }

    public String getStatusMessage() {
        if (bluetoothEnabled() && getNumberOfConnections() == 0 ) {
            return "pair";
        } else if (acceptThread != null && acceptThread.isAlive()) {
            return  "connection";
        } else if (!NotificationListenerService.isNotificationAccessEnabled()) {
            return "notification";
        } else if (!bluetoothEnabled()) {
            return "bluetooth";
        } else {
            return "...";
        }
    }

    public int getNumberOfConnections() {
        return connections.size();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_TURNING_ON:
                    stopThread();
                    break;
                case BluetoothAdapter.STATE_ON:
                    startThread();
                    break;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "Changes to preference " + key);
        switch (key) {
            case "main_enable_switch":
                if (sharedPreferences.getBoolean("main_enable_switch", true)) {
                    startThread();
                } else {
                    stopThread();
                }
                break;
            case "pref_min_notification_priority":
                minNotificationPriority = Integer.parseInt(sharedPreferences.getString("pref_min_notification_priority", String.valueOf(Notification.PRIORITY_DEFAULT)));
                break;
            case "BlackList":
                blacklistedApp = sharedPreferences.getStringSet("BlackList", new HashSet<String>());
                break;
            default:
        }
    }

    private void startThread() {
        if (!bluetoothEnabled()) {
            Log.i(TAG, "Bluetooth not available or enabled. Cannot start server thread.");
            return;
        }

        notifyListener(getStatusMessage());

        acceptThread = new Thread() {
            public void run() {
                Log.i(TAG, "Listen server started");

                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                try {
                    serverSocket = btAdapter.listenUsingInsecureRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM, uuidSpp);
                    Log.d(TAG, "Server socket created");

                    while (serverSocket != null && btAdapter.isEnabled()) {
                        try {
                            BluetoothSocket bluetoothSocket = serverSocket.accept();
                            Socket socket = new BluetoothSocketAdapter(bluetoothSocket);
                            Log.i(TAG, ">>Connection opened (" + socket.getDestination() + ")");
                            connections.add(new Connection(context, socket, new Handler() {
                                @Override
                                public void onMessageReceived(IncomingMessage message) {
                                    Log.d(TAG, "Message received: " + message);
                                    manageNotificationActions(message);
                                }

                                @Override
                                public void onConnectionClosed(Connection connection) {
                                    connections.remove(connection);
                                    Log.i(TAG, ">>Connection closed (" + connection.getDestination() + ")");
                                    notifyListener(getStatusMessage());
                                }
                            }));
                            notifyListener(getStatusMessage());
                        } catch (IOException e) {
                            Log.e(TAG, "Error during accept", e);
                            Log.i(TAG, "Waiting 5 seconds before accepting again...");
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in listenUsingRfcommWithServiceRecord", e);
                }
                Log.i(TAG, "Listen server stopped");
            }
        };
        acceptThread.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void manageNotificationActions(IncomingMessage message) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        String key = message.getKey();
        if (key != null) {
            StatusBarNotification[] activeNotifications = context.getActiveNotifications(new String[] { key });

            if (activeNotifications.length > 0) {
                StatusBarNotification activeNotification = activeNotifications[0];
                if ("dismiss".equals(message.getAction())) {
                    context.cancelNotification(key);
                } else if (message.getCustomAction() != null) {
                    for (Notification.Action action : activeNotification.getNotification().actions) {
                        if (message.getCustomAction().equals(action.title)) {
                            try {
                                action.actionIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                                Log.e(TAG, "Pending Intent for action " + message.getCustomAction() + " was cancelled.", e);
                            }
                        }
                    }
                }
            }
        }

    }

    private void stopThread() {
        Log.i(TAG, "Stopping server thread.");
        if (acceptThread != null) {
            acceptThread.interrupt();
            for (Connection connection : connections) {
                connection.close();
            }
            connections.clear();
            Log.i(TAG, "Server thread stopped.");
        } else {
            Log.i(TAG, "Server thread already stopped.");
        }

        if (serverSocket != null) {
            Log.i(TAG, "Closing server listening socket...");
            try {
                serverSocket.close();
                Log.i(TAG, "Server listening socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Unable to close server socket", e);
            } finally {
                serverSocket = null;
            }
        }
        notifyListener(getStatusMessage());
    }

    private void notifyListener(String status) {
        Intent intent = new Intent(IntentRequestCodes.INTENT_SERVER_STATUS_CHANGE);
        intent.putExtra("status", status);
        Log.d(TAG, "Sending server status change: " + status);
        context.sendBroadcast(intent);
    }

}
