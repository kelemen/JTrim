package org.jtrim2.build.credentials

import java.awt.GraphicsEnvironment
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer
import javax.swing.SwingUtilities
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish

object CredentialUtils {
    private val CREDENTIALS_PROVIDER: CredentialsProvider = UiCredentialsProvider()
    private val CACHED_CREDENTIALS: ConcurrentMap<String, Map<Any, String>> = ConcurrentHashMap()

    fun setupCredentialProvider() {
        if (!GraphicsEnvironment.isHeadless()) {
            CredentialsProvider.setDefault(CREDENTIALS_PROVIDER)
        }
    }

    private data class CredentialItemId(
            private val index: Int,
            private val credentialType: CredentialType,
            private val text: String)

    private class UiCredentialsProvider : CredentialsProvider() {
        override fun isInteractive(): Boolean = true

        override fun supports(vararg items: CredentialItem): Boolean {
            for (item in items) {
                if (selectType(item) == null) {
                    return false
                }
            }
            return true
        }

        private fun selectType(item: CredentialItem): CredentialType? {
            return when (item) {
                is CredentialItem.StringType, is CredentialItem.CharArrayType -> CredentialType.TYPE_STRING
                is CredentialItem.YesNoType -> CredentialType.TYPE_BOOL
                is CredentialItem.InformationalMessage -> CredentialType.TYPE_INFO
                else -> null
            }
        }

        override fun get(uri: URIish, vararg items: CredentialItem): Boolean {
            val itemsById = HashMap<Any, CredentialItem>()

            val uiDefs: MutableList<DisplayedCredentialDef> = ArrayList()
            for (i in items.indices) {
                val item = items[i]
                val credentialType = selectType(item) ?: return false

                val caption = item.promptText
                val id = CredentialItemId(i, credentialType, caption)

                uiDefs.add(DisplayedCredentialDef(id, item.promptText, credentialType))
                itemsById[id] = item
            }

            val cacheKey = uri.toString()
            val result = query(cacheKey, uiDefs)
            if (!hasAll(uiDefs, result)) {
                return false
            }

            result.forEach { (id: Any, value: String) ->
                when (val item = itemsById.remove(id)!!) {
                    is CredentialItem.StringType -> item.value = value
                    is CredentialItem.CharArrayType -> item.value = value.toCharArray()
                    is CredentialItem.YesNoType -> item.value = value.toBoolean()
                }
            }

            CACHED_CREDENTIALS[cacheKey] = result
            return true
        }

        override fun reset(uri: URIish) {
            println("Resetting credential for $uri")
            CACHED_CREDENTIALS.remove(uri.toString())
        }
    }

    private fun tryGetFromCache(cacheKey: String, uiDefs: List<DisplayedCredentialDef>): Map<Any, String>? {
        val cachedResult = CACHED_CREDENTIALS[cacheKey]
        return if (cachedResult != null && hasAll(uiDefs, cachedResult)) cachedResult else null
    }

    private fun hasAll(uiDefs: List<DisplayedCredentialDef>, valuesById: Map<Any, String>): Boolean {
        val remainingKeys = HashSet<Any>(valuesById.keys)
        uiDefs.forEach(Consumer { def: DisplayedCredentialDef -> remainingKeys.remove(def.id) })
        return remainingKeys.isEmpty()
    }

    private fun query(cacheKey: String, uiDefs: List<DisplayedCredentialDef>): Map<Any, String> {
        val cachedResult = tryGetFromCache(cacheKey, uiDefs)
        if (cachedResult != null) {
            return cachedResult
        }

        println("Requesting credential for $cacheKey")

        val result = HashMap<Any, String>()
        invokeUi {
            val valuesById = CredentialQueryDialog.queryVariables(uiDefs)
            if (valuesById != null) {
                result.putAll(valuesById)
            }
        }
        return Collections.unmodifiableMap(result)
    }

    private fun invokeUi(task: Runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run()
        } else {
            SwingUtilities.invokeAndWait(task)
        }
    }
}
