package com.anb.root;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Bounded, draining process runner; only short control commands use this runner. */
public final class CommandRunner {
    public static final class Result {
        public int exitCode;
        public String output;
        public boolean timedOut, truncated;
    }
    public static Result run(String[] command, int timeoutMs) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean[] truncated = {false};
        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] chunk = new byte[4096]; int n;
                while ((n=input.read(chunk))!=-1) {
                    synchronized (bytes) {
                        int keep=Math.min(n,65536-bytes.size());
                        if(keep>0)bytes.write(chunk,0,keep);
                        if(keep<n)truncated[0]=true;
                    }
                }
            } catch(IOException ignored) { }
        }, "command-output");
        reader.setDaemon(true); reader.start();
        Result result = new Result();
        result.timedOut = !process.waitFor(timeoutMs,TimeUnit.MILLISECONDS);
        if(result.timedOut) process.destroyForcibly();
        reader.join(1000);
        result.exitCode = result.timedOut ? -1 : process.exitValue();
        synchronized(bytes) {
            result.output = new String(bytes.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
            result.truncated = truncated[0];
        }
        return result;
    }
}
