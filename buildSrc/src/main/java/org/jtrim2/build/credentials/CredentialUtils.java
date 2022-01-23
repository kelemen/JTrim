package org.jtrim2.build.credentials;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.SwingUtilities;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public final class CredentialUtils {
    private static final CredentialsProvider CREDENTIALS_PROVIDER = new UiCredentialsProvider();

    public static void setupCredentialProvider() {
        if (!GraphicsEnvironment.isHeadless()) {
            CredentialsProvider.setDefault(CREDENTIALS_PROVIDER);
        }
    }

    private static final class CredentialItemId {
        private final int index;
        private final CredentialType credentialType;
        private final String text;

        public CredentialItemId(int index, CredentialType credentialType, String text) {
            this.index = index;
            this.credentialType = credentialType;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CredentialItemId that = (CredentialItemId) o;
            return index == that.index
                    && credentialType == that.credentialType
                    && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, credentialType, text);
        }
    }

    private static final class UiCredentialsProvider extends CredentialsProvider {
        private static final ConcurrentMap<String, Map<Object, String>> CACHED_CREDENTIALS = new ConcurrentHashMap<>();

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public boolean supports(CredentialItem... items) {
            for (CredentialItem item : items) {
                if (selectType(item) == null) {
                    return false;
                }
            }
            return true;
        }

        private CredentialType selectType(CredentialItem item) {
            if (item instanceof CredentialItem.StringType || item instanceof CredentialItem.CharArrayType) {
                return CredentialType.TYPE_STRING;
            } else if (item instanceof CredentialItem.YesNoType) {
                return CredentialType.TYPE_BOOL;
            } else if (item instanceof CredentialItem.InformationalMessage) {
                return CredentialType.TYPE_INFO;
            } else {
                return null;
            }
        }

        @Override
        public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
            Map<Object, CredentialItem> itemsById = new HashMap<>();

            List<DisplayedCredentialDef> uiDefs = new ArrayList<>();
            for (int i = 0; i < items.length; i++) {
                CredentialItem item = items[i];
                CredentialType credentialType = selectType(item);
                if (credentialType == null) {
                    return false;
                }

                String caption = item.getPromptText();
                CredentialItemId id = new CredentialItemId(i, credentialType, caption);

                uiDefs.add(new DisplayedCredentialDef(id, item.getPromptText(), credentialType));
                itemsById.put(id, item);
            }

            String cacheKey = uri.toString();
            Map<Object, String> result = query(cacheKey, uiDefs);
            if (!hasAll(uiDefs, result)) {
                return false;
            }

            result.forEach((id, value) -> {
                CredentialItem item = Objects.requireNonNull(itemsById.remove(id), "item");

                if (item instanceof CredentialItem.StringType) {
                    ((CredentialItem.StringType) item).setValue(value);
                } else if (item instanceof CredentialItem.CharArrayType) {
                    ((CredentialItem.CharArrayType) item).setValue(value.toCharArray());
                } else if (item instanceof CredentialItem.YesNoType) {
                    ((CredentialItem.YesNoType) item).setValue(Boolean.parseBoolean(value));
                }
            });

            CACHED_CREDENTIALS.put(cacheKey, result);
            return true;
        }

        private static Map<Object, String> tryGetFromCache(String cacheKey, List<DisplayedCredentialDef> uiDefs) {
            Map<Object, String> cachedResult = CACHED_CREDENTIALS.get(cacheKey);
            return cachedResult != null && hasAll(uiDefs, cachedResult)
                    ? cachedResult
                    : null;
        }

        private static boolean hasAll(List<DisplayedCredentialDef> uiDefs, Map<Object, String> valuesById) {
            Set<Object> remainingKeys = new HashSet<>(valuesById.keySet());
            uiDefs.forEach(def -> {
                remainingKeys.remove(def.getId());
            });
            return remainingKeys.isEmpty();
        }

        private static Map<Object, String> query(String cacheKey, List<DisplayedCredentialDef> uiDefs) {
            Map<Object, String> cachedResult = tryGetFromCache(cacheKey, uiDefs);
            if (cachedResult != null) {
                return cachedResult;
            }

            System.out.println("Requesting credential for " + cacheKey);

            Map<Object, String> result = new HashMap<>();
            invokeUi(() -> {
                Map<Object, String> valuesById = CredentialQueryDialog.queryVariables(uiDefs);
                result.putAll(valuesById);
            });
            return Collections.unmodifiableMap(result);
        }

        private static void invokeUi(Runnable task) {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(task);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException(ex.getTargetException());
                }
            }
        }

        @Override
        public void reset(URIish uri) {
            System.out.println("Resetting credential for " + uri);
            CACHED_CREDENTIALS.remove(uri.toString());
        }
    }

    private CredentialUtils() {
        throw new AssertionError();
    }
}
