/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.Collection;

/**
 *
 * @author Kelemen Attila
 */
public interface CachedQuery<CacheRefType> {
    public Collection<CacheRefType> clearCache();
    public boolean removeFromCache(CacheRefType arg);
}
