package com.anb.root;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Bounded, draining process runner; only short control commands use this runner. */
public final class CommandRunner {
    public static final class Result {
        public int exitCode;
        public String output;
        public boolean timedOut, truncated, incomplete;
    }
    public static Result run(String[] command, int timeoutMs) throws Exception {
        if(timeoutMs<=0)throw new IllegalArgumentException("Invalid timeout");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean[] truncated = {false}, readFailed = {false};
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
            } catch(IOException ignored) { synchronized(bytes){readFailed[0]=true;} }
        }, "command-output");
        reader.setDaemon(true); reader.start();
        try {
            // Control commands never read input. Closing stdin also prevents accidental hangs.
            process.getOutputStream().close();
            Result result = new Result();
            result.timedOut = !process.waitFor(timeoutMs,TimeUnit.MILLISECONDS);
            if(result.timedOut){process.destroyForcibly();process.waitFor(1000,TimeUnit.MILLISECONDS);}
            reader.join(1000);
            result.incomplete=reader.isAlive();
            result.exitCode = result.timedOut ? -1 : process.exitValue();
            synchronized(bytes) {
                result.output = new String(bytes.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
                result.truncated = truncated[0];result.incomplete|=readFailed[0];
            }
            return result;
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();throw e;
        } finally {
            // Own only this short control process; never kill detached recovery descendants.
            if(process.isAlive())process.destroyForcibly();
            close(process.getOutputStream());close(process.getInputStream());close(process.getErrorStream());
            try{reader.join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
    private static void close(Closeable stream){try{stream.close();}catch(IOException ignored){}}
}
