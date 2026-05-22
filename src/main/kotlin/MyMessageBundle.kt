package bynarig

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

/**
 * Resource-bundle accessor for the plugin's localisable strings.
 *
 * Backed by `messages/MyMessageBundle.properties` (and any locale-specific siblings).
 * `DynamicBundle` honours the IDE's current language pack at runtime, so once strings
 * are translated into Japanese/Chinese/Korean the plugin will switch automatically.
 *
 * Usage:
 *   ```
 *   val title = MyMessageBundle.message("terminal.tab.name")
 *   val toast = MyMessageBundle.message("notification.launch.failed", reason)
 *   ```
 */
private const val BUNDLE = "messages.MyMessageBundle"

internal object MyMessageBundle {

    // A single shared bundle instance — DynamicBundle caches lookups internally,
    // so re-using one object across the plugin is the recommended pattern.
    private val instance = DynamicBundle(MyMessageBundle::class.java, BUNDLE)

    /**
     * Resolve a key to a localised, formatted string.
     *
     * @param key    property key, validated against the bundle at compile time
     *               thanks to the `@PropertyKey` annotation.
     * @param params optional positional arguments for `MessageFormat` placeholders
     *               such as `{0}`, `{1}`, …
     */
    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): @Nls String {
        return instance.getMessage(key, *params)
    }

    /**
     * Lazy variant used by platform APIs (e.g. notifications) that defer string
     * resolution until the message is actually rendered.
     */
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): Supplier<@Nls String> {
        return instance.getLazyMessage(key, *params)
    }
}
