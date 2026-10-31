package io.pocat.platform.util.clazz;

public class InstantiationFailedException extends RuntimeException {
    public InstantiationFailedException(String msg) {
        super(msg);
    }

    public InstantiationFailedException(String msg, Throwable e) {
        super(msg, e);
    }
}
