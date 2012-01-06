/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Kelemen Attila
 */
public final class CachedLinkRequest<QueryArgType> {
    private final QueryArgType queryArg;
    private final long cacheExpireNanos;

    public CachedLinkRequest(QueryArgType queryArg) {
        this(queryArg, 60, TimeUnit.MINUTES);
    }

    public CachedLinkRequest(QueryArgType queryArg, long cacheExpire, TimeUnit timeunit) {
        this.queryArg = queryArg;
        this.cacheExpireNanos = timeunit.toNanos(cacheExpire);
    }

    public long getCacheExpire(TimeUnit timeunit) {
        return timeunit.convert(cacheExpireNanos, TimeUnit.NANOSECONDS);
    }

    public QueryArgType getQueryArg() {
        return queryArg;
    }

    @Override
    public String toString() {
        return "CachedDataRequest{"
                + "Arg=" + queryArg
                + ", Expire=" + getCacheExpire(TimeUnit.MILLISECONDS)
                + " ms}";
    }
}
