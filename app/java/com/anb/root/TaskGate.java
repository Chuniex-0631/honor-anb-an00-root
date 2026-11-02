package com.anb.root;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** A single owner across checking, recovery, settings and manager work. */
public final class TaskGate {
    public enum Task { CHECK, RECOVERY, MANAGER, SETTINGS }
    private final AtomicReference<Lease> owner=new AtomicReference<>();
    public final class Lease implements AutoCloseable {
        public final Task task;
        private final AtomicBoolean closed=new AtomicBoolean();
        private Lease(Task task){this.task=task;}
        @Override public void close(){if(closed.compareAndSet(false,true))owner.compareAndSet(this,null);}
    }
    public Lease acquire(Task task) {
        if(task==null)throw new IllegalArgumentException("Missing task");
        Lease lease=new Lease(task);return owner.compareAndSet(null,lease)?lease:null;
    }
    public boolean busy(){return owner.get()!=null;}
    public boolean checking(){Lease lease=owner.get();return lease!=null&&lease.task==Task.CHECK;}
}
