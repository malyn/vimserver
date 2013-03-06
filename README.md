# VimServer

VimServer is a Java library that implements the server-side portion of Vim's Win32 [remote protocol][].  Individual instances of Vim are able to send commands and text to each other using this protocol and VimServer makes it possible to participate in that exchange.

VimServer only supports the Win32 version of Vim right now and only implements the barest minimum of the Vim remote protocol.  Pull requests with additional functionality, platform support, etc. are welcome.

[remote protocol]: http://vimdoc.sourceforge.net/htmldoc/remote.html

## Usage

The latest VimServer artifacts are published on Maven Central.  Getting access to VimServer should be as simple as adding the following dependency to your pom.xml file:

```xml
<dependencies>
  <dependency>
    <groupId>com.michaelalynmiller</groupId>
    <artifactId>vimserver</artifactId>
    <version>1.0.0</version> <!-- or whatever the latest version is -->
  </dependency>
</dependencies>
```

Integrating with VimServer is also easy -- just implement IVimDataHandler and start up an instance of VimServer:

```java
/* Start VimServer. */
VimServer vimServer = new VimServer("MyVimTarget");
vimServer.start(new IVimDataHandler() {
    public void handleReceivedText(final String text) {
        System.out.println(">>> " + text);
    }
});

/* Wait until our app shuts down... */
Thread.sleep(10 * 1000);

/* Stop VimServer. */
vimServer.stop();
```

You can send strings of text to VimServer using Vim's [remote\_send()][] function:

```vim
:echo remote_send("MyVimTarget", "Hello VimServer!")
```

Note that `echo` is used here on the assumption that you are typing the above command into Vim's command line.  You should use `call remote_send(...)` if you are trying to talk to VimServer from a script.

[remote\_send()]: http://vimdoc.sourceforge.net/htmldoc/eval.html#remote_send()

## TODO

The following items need to be addressed:

1. Vim supports different character encodings and sends the current encoding to the server.  VimServer ignores that message, but ideally it would process the message and apply the proper character encoding transformation.
2. VimServer should detect if an attempt is made to start a server with the same name as another server (either by GVim, VimServer, or something else).
