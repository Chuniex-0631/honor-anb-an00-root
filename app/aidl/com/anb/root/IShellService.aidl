package com.anb.root;
import android.os.Bundle;
interface IShellService {
    Bundle execute(String command, int timeoutMs) = 0;
    void destroy() = 16777114;
}
