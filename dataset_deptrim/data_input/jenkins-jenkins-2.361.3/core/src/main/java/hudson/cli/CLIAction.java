/*
 * The MIT License
 *
 * Copyright (c) 2013 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.cli;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import jenkins.util.FullDuplexHttpService;
import jenkins.websocket.WebSocketSession;
import jenkins.websocket.WebSockets;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.security.core.Authentication;

/**
 * Shows usage of CLI and commands.
 *
 * @author ogondza
 */
@Extension @Symbol("cli")
@Restricted(NoExternalUse.class)
public class CLIAction implements UnprotectedRootAction, StaplerProxy {

    private static final Logger LOGGER = Logger.getLogger(CLIAction.class.getName());

    private final transient Map<UUID, FullDuplexHttpService> duplexServices = new HashMap<>();

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Jenkins CLI";
    }

    @Override
    public String getUrlName() {
        return "cli";
    }

    public void doCommand(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        final Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.READ);

        // Strip trailing slash
        final String commandName = req.getRestOfPath().substring(1);
        CLICommand command = CLICommand.clone(commandName);
        if (command == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "No such command");
            return;
        }

        req.setAttribute("command", command);
        req.getView(this, "command.jelly").forward(req, rsp);
    }

    /** for Jelly */
    public boolean isWebSocketSupported() {
        return WebSockets.isSupported();
    }

    /**
     * WebSocket endpoint.
     */
    public HttpResponse doWs() {
        if (!WebSockets.isSupported()) {
            return HttpResponses.notFound();
        }
        Authentication authentication = Jenkins.getAuthentication2();
        return WebSockets.upgrade(new WebSocketSession() {
            ServerSideImpl connection;
            long sentBytes, sentCount, receivedBytes, receivedCount;
            class OutputImpl implements PlainCLIProtocol.Output {
                @Override
                public void send(byte[] data) throws IOException {
                    sendBinary(ByteBuffer.wrap(data));
                    sentBytes += data.length;
                    sentCount++;
                }

                @Override
                public void close() throws IOException {
                    doClose();
                }
            }

            private void doClose() throws IOException {
                close();
            }

            @Override
            protected void opened() {
                try {
                    connection = new ServerSideImpl(new OutputImpl(), authentication);
                } catch (IOException x) {
                    error(x);
                    return;
                }
                new Thread(() -> {
                    try {
                        try {
                            connection.run();
                        } finally {
                            connection.close();
                        }
                    } catch (Exception x) {
                        error(x);
                    }
                }, "CLI handler for " + authentication.getName()).start();
            }

            @Override
            protected void binary(byte[] payload, int offset, int len) {
                try {
                    connection.handle(new DataInputStream(new ByteArrayInputStream(payload, offset, len)));
                    receivedBytes += len;
                    receivedCount++;
                } catch (IOException x) {
                    error(x);
                }
            }

            @Override
            protected void error(Throwable cause) {
                LOGGER.log(Level.WARNING, null, cause);
            }

            @Override
            protected void closed(int statusCode, String reason) {
                LOGGER.fine(() -> "closed: " + statusCode + ": " + reason);
                LOGGER.fine(() -> "received " + receivedCount + " packets of " + receivedBytes + " bytes; sent " + sentCount + " packets of " + sentBytes + " bytes");
                connection.handleClose();
            }
        });
    }

    @Override
    public Object getTarget() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req.getRestOfPath().length() == 0 && "POST".equals(req.getMethod())) {
            // CLI connection request
            if ("false".equals(req.getParameter("remoting"))) {
                throw new PlainCliEndpointResponse();
            } else {
                // remoting=true (the historical default) no longer supported.
                throw HttpResponses.forbidden();
            }
        } else {
            return this;
        }
    }

    static class ServerSideImpl extends PlainCLIProtocol.ServerSide {
        private Thread runningThread;
        private boolean ready;
        private final List<String> args = new ArrayList<>();
        private Locale locale = Locale.getDefault();
        private Charset encoding = Charset.defaultCharset();
        private final PipedInputStream stdin = new PipedInputStream();
        private final PipedOutputStream stdinMatch = new PipedOutputStream();
        private final Authentication authentication;

        ServerSideImpl(PlainCLIProtocol.Output out, Authentication authentication) throws IOException {
            super(out);
            stdinMatch.connect(stdin);
            this.authentication = authentication;
        }

        @Override
        protected void onArg(String text) {
            args.add(text);
        }

        @Override
        protected void onLocale(String text) {
            for (Locale _locale : Locale.getAvailableLocales()) {
                if (_locale.toString().equals(text)) {
                    locale = _locale;
                    return;
                }
            }
            LOGGER.log(Level.WARNING, "unknown client locale {0}", text);
        }

        @Override
        protected void onEncoding(String text) {
            try {
                encoding = Charset.forName(text);
            } catch (UnsupportedCharsetException x) {
                LOGGER.log(Level.WARNING, "unknown client charset {0}", text);
            }
        }

        @Override
        protected void onStart() {
            ready();
        }

        @Override
        protected void onStdin(byte[] chunk) throws IOException {
            stdinMatch.write(chunk);
        }

        @Override
        protected void onEndStdin() throws IOException {
            stdinMatch.close();
        }

        @Override
        protected void handleClose() {
            ready();
            if (runningThread != null) {
                runningThread.interrupt();
            }
        }

        private synchronized void ready() {
            ready = true;
            notifyAll();
        }

        void run() throws IOException, InterruptedException {
            synchronized (this) {
                while (!ready) {
                    wait();
                }
            }
            PrintStream stdout = new PrintStream(streamStdout(), false, encoding);
            PrintStream stderr = new PrintStream(streamStderr(), true, encoding);
            if (args.isEmpty()) {
                stderr.println("Connection closed before arguments received");
                sendExit(2);
                return;
            }
            String commandName = args.get(0);
            CLICommand command = CLICommand.clone(commandName);
            if (command == null) {
                stderr.println("No such command " + commandName);
                sendExit(2);
                return;
            }
            command.setTransportAuth2(authentication);
            command.setClientCharset(encoding);
            CLICommand orig = CLICommand.setCurrent(command);
            try {
                runningThread = Thread.currentThread();
                int exit = command.main(args.subList(1, args.size()), locale, stdin, stdout, stderr);
                stdout.flush();
                sendExit(exit);
                try { // seems to avoid ReadPendingException from Jetty
                    Thread.sleep(1000);
                } catch (InterruptedException x) {
                    // expected; ignore
                }
            } finally {
                CLICommand.setCurrent(orig);
                runningThread = null;
            }
        }
    }

    /**
     * Serves {@link PlainCLIProtocol} response.
     */
    private class PlainCliEndpointResponse extends FullDuplexHttpService.Response {

        PlainCliEndpointResponse() {
            super(duplexServices);
        }

        @Override
        protected FullDuplexHttpService createService(StaplerRequest req, UUID uuid) throws IOException {
            return new FullDuplexHttpService(uuid) {
                @Override
                protected void run(InputStream upload, OutputStream download) throws IOException, InterruptedException {
                    try (ServerSideImpl connection = new ServerSideImpl(new PlainCLIProtocol.FramedOutput(download), Jenkins.getAuthentication2())) {
                        new PlainCLIProtocol.FramedReader(connection, upload).start();
                        connection.run();
                    }
                }
            };
        }
    }

}
