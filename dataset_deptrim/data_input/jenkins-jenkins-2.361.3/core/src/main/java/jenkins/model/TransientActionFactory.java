/*
 * The MIT License
 *
 * Copyright 2013 Jesse Glick.
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

package jenkins.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ExtensionList;
import hudson.ExtensionListListener;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.TopLevelItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Allows you to add actions to any kind of object at once.
 * @param <T> the type of object to add to; typically an {@link Actionable} subtype,
 *            but may specify a type such as {@link TopLevelItem} most of whose implementations are in fact {@link Actionable}
 * @see Actionable#getAllActions
 * @since 1.548
 */
@SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
public abstract class TransientActionFactory<T> implements ExtensionPoint {

    /**
     * The type of object this factory cares about.
     * Declared separately, rather than by having {@link #createFor} do a check-cast,
     * so that method bodies are not loaded until actually needed.
     * @return the type of {@link T}
     */
    public abstract Class<T> type();

    /**
     * A supertype of any actions this factory might produce.
     * Defined so that factories which produce irrelevant actions need not be consulted by, e.g., {@link Actionable#getAction(Class)}.
     * For historical reasons this defaults to {@link Action} itself.
     * If your implementation was returning multiple disparate kinds of actions, it is best to split it into two factories.
     * <p>If an API defines a abstract {@link Action} subtype and you are providing a concrete implementation,
     * you may return the API type here to delay class loading.
     * @return a bound for the result of {@link #createFor}
     * @since 2.34
     */
    public /* abstract */ Class<? extends Action> actionType() {
        return Action.class;
    }

    /**
     * Creates actions for a given object.
     * This may be called frequently for the same object, so if your implementation is expensive, do your own caching.
     * @param target an actionable object
     * @return a possible empty set of actions (typically either using {@link Collections#emptySet} or {@link Collections#singleton})
     */
    public abstract @NonNull Collection<? extends Action> createFor(@NonNull T target);

    /** @see <a href="http://stackoverflow.com/a/24336841/12916">no pairs/tuples in Java</a> */
    private static class CacheKey {
        private final Class<?> type;
        private final Class<? extends Action> actionType;

        CacheKey(Class<?> type, Class<? extends Action> actionType) {
            this.type = type;
            this.actionType = actionType;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CacheKey && type == ((CacheKey) obj).type && actionType == ((CacheKey) obj).actionType;
        }

        @Override
        public int hashCode() {
            return type.hashCode() ^ actionType.hashCode();
        }
    }

    @SuppressWarnings("rawtypes")
    private static final LoadingCache<ExtensionList<TransientActionFactory>, LoadingCache<CacheKey, List<TransientActionFactory<?>>>> cache =
        CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<>() {
        @Override
        public LoadingCache<CacheKey, List<TransientActionFactory<?>>> load(final ExtensionList<TransientActionFactory> allFactories) throws Exception {
            final LoadingCache<CacheKey, List<TransientActionFactory<?>>> perJenkinsCache =
                CacheBuilder.newBuilder().build(new CacheLoader<>() {
                @Override
                public List<TransientActionFactory<?>> load(CacheKey key) throws Exception {
                    List<TransientActionFactory<?>> factories = new ArrayList<>();
                    for (TransientActionFactory<?> taf : allFactories) {
                        Class<? extends Action> actionType = taf.actionType();
                        if (taf.type().isAssignableFrom(key.type) && (key.actionType.isAssignableFrom(actionType) || actionType.isAssignableFrom(key.actionType))) {
                            factories.add(taf);
                        }
                    }
                    return factories;
                }
            });
            allFactories.addListener(new ExtensionListListener() {
                @Override
                public void onChange() {
                    perJenkinsCache.invalidateAll();
                }
            });
            return perJenkinsCache;
        }
    });

    @Restricted(NoExternalUse.class) // pending a need for it outside Actionable
    public static Iterable<? extends TransientActionFactory<?>> factoriesFor(Class<?> type, Class<? extends Action> actionType) {
        return cache.getUnchecked(ExtensionList.lookup(TransientActionFactory.class)).getUnchecked(new CacheKey(type, actionType));
    }

}
