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

package org.holylobster.nuntius.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import org.holylobster.nuntius.connection.ConnectionManager;
import org.holylobster.nuntius.connection.ConnectionProvider;
import org.holylobster.nuntius.connection.Socket;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkConnectionProvider implements ConnectionProvider {

    private static final String TAG = NetworkConnectionProvider.class.getSimpleName();

    private final ConnectionManager connectionManager;

    private ServerSocket serverSocket;

    private final Thread thread;

    private NsdManager.RegistrationListener registrationListener;

    public int port = 12233;

    NsdManager nsdManager;

    public NetworkConnectionProvider(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.thread = new Thread() {
            public void run() {
                Log.i(TAG, "Listen server started");

                try {
                    serverSocket = getServerSocket();
                    port = serverSocket.getLocalPort();

                    Log.d(TAG, "Server socket created " + serverSocket.getLocalSocketAddress() + ", bound: " + serverSocket.isBound());

                    while (serverSocket != null) {
                        try {
                            java.net.Socket networkSocket = serverSocket.accept();
                            Socket socket = new NetworkSocketAdapter(networkSocket);
                            Log.i(TAG, ">>Connection opened (" + socket.getDestination() + ")");
                            NetworkConnectionProvider.this.connectionManager.newConnection(socket);
                        } catch (IOException e) {
                            Log.e(TAG, "Error during accept", e);
                            Log.i(TAG, "Waiting 5 seconds before accepting again...");
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in accept", e);
                }
                Log.i(TAG, "Listen server stopped");
            }
        };
    }

    public void initializeRegistrationListener() {
         registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                Log.d(TAG, "registered : " + NsdServiceInfo.toString());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Failed registration :" + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
         };
    }

    public void registerService(Context context) {
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("NuntiusAndroid");
        serviceInfo.setServiceType("_nuntius._tcp.");
        serviceInfo.setPort(port);
        Log.d(TAG, port + "");

        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        Log.d(TAG, "register : " + serviceInfo.toString());
        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);

    }

    public void unregisterService() {
        nsdManager.unregisterService(registrationListener);
    }

    ServerSocket getServerSocket() throws Exception {
        return new ServerSocket(port);
    }

    public void close() {
        thread.interrupt();
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
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public boolean isAlive() {
        return thread.isAlive();
    }
}
