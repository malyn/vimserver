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

/* JUnit imports. */
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for VimServer
 */
public class VimServerTest extends Assert {
    /**
     * Starts and stops VimServer.
     */
    @Test
    public void testStartStopVimServer() throws VimServerException
    {
        /* Create a dummy handler. */
        IVimDataHandler handler = new IVimDataHandler()
        {
            public void handleReceivedText(final String text) {}
        };

        /* Start VimServer. */
        VimServer vimServer = new VimServer("testStartStopVimServer");
        vimServer.start(handler);

        /* Stop VimServer. */
        vimServer.stop();
    }

    /**
     * Sample code for the README.
     */
    @Test
    public void testVimServerSample() throws VimServerException
    {
        /* Start VimServer. */
        VimServer vimServer = new VimServer("MyVimTarget");
        vimServer.start(new IVimDataHandler() {
            public void handleReceivedText(final String text) {
                System.out.println(">>> " + text);
            }
        });

        /* Wait until our app shuts down... */

        /* Stop VimServer. */
        vimServer.stop();
    }
}
