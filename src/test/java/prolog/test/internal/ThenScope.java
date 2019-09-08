package prolog.test.internal;

import prolog.test.Then;

import java.io.Closeable;

/**
 * Used to preserve the scope of an enclosing then
 */
public class ThenScope implements Closeable {

    private static ThreadLocal<Then> thenScope = new ThreadLocal<>();

    private final Then prior;

    public ThenScope(Then thisThen) {
        prior = thenScope.get();
        thenScope.set(thisThen);
    }

    @Override
    public void close() {
        if (prior == null) {
            thenScope.remove();
        } else {
            thenScope.set(prior);
        }
    }


    public static Then getScope() {
        return thenScope.get();
    }
}
