package com.anb.root;

import java.io.IOException;

/** Typed failures keep user guidance independent of message wording. */
public final class RecoveryFailure extends IOException {
    public final FailureText.Kind kind;
    public RecoveryFailure(FailureText.Kind kind,String message){super(message);this.kind=kind;}
}
