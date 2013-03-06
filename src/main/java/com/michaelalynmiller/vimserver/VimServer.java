/*
 * Copyright (c) 2013, Michael Alyn Miller <malyn@strangeGizmo.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Michael Alyn Miller nor the names of the
 *    contributors to this software may be used to endorse or promote
 *    products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package com.michaelalynmiller.vimserver;

/* JNA imports. */
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser.MSG;

/* JNA platform extension imports. */
import com.michaelalynmiller.jnaplatext.win32.Kernel32;
import com.michaelalynmiller.jnaplatext.win32.User32;
import com.michaelalynmiller.jnaplatext.win32.WinUser;

/**
 * Receives messages from a Win32 Vim client.
 *
 * @author      Michael Alyn Miller <malyn@strangeGizmo.com>
 * @version     1.0.0
 */
public final class VimServer {
    /** Name of this VimServer. */
    private final String name;

    /** true if the VimServer has been started, false otherwise. */
    private boolean started;

    /** The WindowThread that is reading events from the Vim client. */
    private WindowThread windowThread;

    /**
     * Constructs a VimServer with the given server name.  The name must
     * be globally unique on this machine.
     *
     * @param serverName The name of this instance of VimServer.
     */
    public VimServer(final String serverName) {
        this.name = serverName;
    }

    /**
     * Starts the VimServer and begins dispatching received messages to
     * the given data handler.
     *
     * @param dataHandler The handler for messages received from the Vim
     *  client.
     * @exception VimServerException If VimServer has already been
     *  started or if an unrecoverable error occurs while starting
     *  VimServer.
     */
    public synchronized void start(final IVimDataHandler dataHandler)
            throws VimServerException {
        /* Don't start more than once. */
        if (this.started) {
            throw new VimServerException(
                    "VimServer has already been started.");
        }

        /* Create and start the thread that will listen for Win32
         * messages from the Vim client. */
        this.windowThread = new WindowThread(this.name, dataHandler);
        new Thread(this.windowThread).start();
        this.started = true;
    }

    /**
     * Stop the VimServer.  Does nothing if the server has not been
     * started.
     */
    public synchronized void stop() {
        if (this.started) {
            this.windowThread.stop();
            this.started = false;
        }
    }

    /**
     * Creates a Win32 window for receiving Vim events and dispatches
     * those events to a data handler.
     */
    private static final class WindowThread implements Runnable {
        /** Window name. */
        private String windowName;

        /** The handler for messages received from the Vim client. */
        private IVimDataHandler dataHandler;

        /** Window handle. */
        private HWND hwndVimServer;

        /**
         * Creates a new WindowThread with the given name and data
         * handler.
         *
         * @param name Name of the window.
         * @param handler The handler for messages received from the Vim
         *  client.
         */
        public WindowThread(
                final String name,
                final IVimDataHandler handler) {
            windowName = name;
            dataHandler = handler;
        }

        /**
         * Stops the window thread.
         */
        public void stop() {
            User32.INSTANCE.PostMessage(
                this.hwndVimServer, WinUser.WM_QUIT,
                new WPARAM(0), new LPARAM(0));
        }

        /**
         * Runs the window thread.
         */
        public void run() {
            /* Define the Vim window class. */
            final WString windowClass = new WString("VIM_MESSAGES");
            final HINSTANCE hInst = Kernel32.INSTANCE.GetModuleHandle("");

            WinUser.WNDCLASSEX wClass = new WinUser.WNDCLASSEX();
            wClass.hInstance = hInst;
            wClass.lpfnWndProc = new MessageHandler(dataHandler);
            wClass.lpszClassName = windowClass;

            /* Register the window class. */
            User32.INSTANCE.RegisterClassEx(wClass);

            /* Create the window. */
            this.hwndVimServer = User32.INSTANCE.CreateWindowEx(
                    0, windowClass, windowName,
                    WinUser.WS_POPUPWINDOW | WinUser.WS_CAPTION,
                    WinUser.CW_USEDEFAULT, WinUser.CW_USEDEFAULT,
                    WinUser.CW_USEDEFAULT, WinUser.CW_USEDEFAULT,
                    null, null,
                    hInst, null);
            if (this.hwndVimServer == null) {
                throw new RuntimeException(
                        "CreateWindowEx failed with: "
                            + Kernel32.INSTANCE.GetLastError());
            }

            /* Receive messages until we get a WM_QUIT.  Note that this
             * loop must be on the same thread that created the window,
             * otherwise it will never receive any messages. */
            MSG msg = new MSG();
            while (User32.INSTANCE.GetMessage(
                    msg, hwndVimServer, 0, 0) != 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }

            /* WM_QUIT; clean up. */
            User32.INSTANCE.UnregisterClass(windowClass, hInst);
            User32.INSTANCE.DestroyWindow(hwndVimServer);
        }

        /**
         * Process Win32 messages sent to our Vim server window.
         */
        private static final class MessageHandler
                implements WinUser.WindowProc {
            /** Add these keys to the server's input queue. */
            private static final int COPYDATA_KEYS = 0;

            /** Message sent from a server to a client in response to a
             * request. */
            private static final int COPYDATA_REPLY = 1;

            /** Evaluate this expression on the server. */
            private static final int COPYDATA_EXPR = 10;

            /** Response to an expression evaluation. */
            private static final int COPYDATA_RESULT = 11;

            /** Error response sent by a server. */
            private static final int COPYDATA_ERROR_RESULT = 12;

            /** The encoding that the client is using. */
            private static final int COPYDATA_ENCODING = 20;

            /** The handler for messages received from the Vim client. */
            private IVimDataHandler dataHandler;

            /**
             * Constructs a new MessageHandler with the given data
             * handler.
             *
             * @param handler The handler for messages received from the
             *  client.
             */
            public MessageHandler(final IVimDataHandler handler) {
                this.dataHandler = handler;
            }

            /**
            * An application-defined function that processes messages
            * sent to a window.
            *
            * @param hwnd A handle to the window.
            * @param uMsg The message.
            * @param wParam Additional message information.
            * @param lParam Additional message information.
            * @return The result of the message processing and depends
            *  on the message sent
            */
            public LRESULT callback(
                    final HWND hwnd, final int uMsg,
                    final WPARAM wParam, final LPARAM lParam) {
                switch (uMsg) {
                    case WinUser.WM_COPYDATA:
                        processCopyData(
                            new WinUser.COPYDATASTRUCT(lParam.longValue()));
                        return new LRESULT(1);

                    default:
                        return User32.INSTANCE.DefWindowProc(
                                hwnd, uMsg, wParam, lParam);
                }
            }

            /**
             * Process a COPYDATA message from Vim.
             *
             * @param data Data sent from the Vim client.
             */
            private void processCopyData(final WinUser.COPYDATASTRUCT data) {
                switch (data.dwData.intValue()) {
                    case COPYDATA_KEYS:
                        String keys = data.lpData.getString(0);
                        /* TODO: Translate the character encoding. */
                        this.dataHandler.handleReceivedText(keys);
                        break;

                    case COPYDATA_ENCODING:
                        String encoding = data.lpData.getString(0);
                        /* TODO: Handle character encoding. */
                        break;

                    default:
                        /* Unknown message; ignore. */
                        break;
                }
            }
        }
    }
}
