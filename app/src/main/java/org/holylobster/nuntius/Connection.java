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

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Connection extends Thread {
    // Used for logging
    private final String TAG = this.getClass().getSimpleName();

    private final BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    private final Context context;

    private BluetoothSocket socket;
    private BufferedOutputStream outputStream;
    private BufferedInputStream inputStream;

    private final Thread senderThread;

    Connection(Context context, BluetoothSocket socket) {
        this.socket = socket;
        this.context = context;

        try {
            InputStream realInputStream = socket.getInputStream();
            inputStream = new BufferedInputStream(realInputStream);
        } catch (IOException e) {
            Log.e(TAG, "Could not create input stream.", e);
        }

        try {
            OutputStream realOutputStream = socket.getOutputStream();
            outputStream = new BufferedOutputStream(realOutputStream);
        } catch (IOException e) {
            Log.e(TAG, "Could not create output stream.", e);
        }

        senderThread = new Thread() {
            public void run() {
                try {
                    while (true) {
                        send(queue.take());
                    }
                } catch (InterruptedException ex) {
                    Log.i(TAG, "Sender thread interrupted.");
                }
            }
        };
        senderThread.start();
    }

    private void send(Message m) {
        if (!isConnected()) {
            // Bluetooth is disabled, we just ignore messages
            return;
        }

        Log.i(TAG, "Sending message over Bluetooth");
        try {
            outputStream.write(m.toJSON(context).getBytes());
            outputStream.write('\r');
            outputStream.write('\n');
            outputStream.flush();
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            } finally {
                outputStream = null;
            }
            Log.e(TAG, "Error sending message over Bluetooth", e);
        }
    }

    public boolean isConnected() {
        return socket != null && outputStream != null && socket.isConnected();
    }

    public boolean enqueue(Message m) {
        return queue.offer(m);
    }

    public void close() {
        senderThread.interrupt();
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing output stream", e);
            } finally {
                outputStream = null;
            }
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            } finally {
                inputStream = null;
            }
        }

        if (socket != null) {
            try {
                socket.getOutputStream().close();
                socket.getInputStream().close();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            } finally {
                socket = null;
            }
        }
        queue.clear();
    }


}
