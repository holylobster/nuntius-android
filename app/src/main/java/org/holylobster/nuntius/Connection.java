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

    private final Thread senderThread;

    private final Handler handler;

    Connection(final Context context, final BluetoothSocket socket, Handler handler) {
        this.handler = handler;
        senderThread = new Thread() {
            public void run() {
                try {
                    OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    while (checkConnected(socket)) {
                        Message message = queue.take();
                        Log.i(TAG, "Sending message over Bluetooth");
                        outputStream.write(message.toJSON(context).getBytes());
                        outputStream.write('\r');
                        outputStream.write('\n');
                        outputStream.flush();
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "Sender thread interrupted");
                } catch (IOException e) {
                    Log.e(TAG, "Error in sender thread", e);
                }
                cleanup(socket);
            }
            private boolean checkConnected(BluetoothSocket socket) {
                return socket != null && socket.isConnected();
            }
        };
        senderThread.start();
    }

    private void cleanup(BluetoothSocket socket) {
        if (socket != null) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            } catch (IOException e) {
            }

            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
        queue.clear();
        handler.onConnectionClosed(this);
    }

    public boolean enqueue(Message m) {
        return queue.offer(m);
    }

    public void close() {
        senderThread.interrupt();
    }
}
