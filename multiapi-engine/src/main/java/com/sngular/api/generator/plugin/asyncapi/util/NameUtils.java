package main.java.com.sngular.api.generator.plugin.asyncapi.util;

public final class NameUtils {

    private NameUtils() {}

    /**
     *  * Concatenates the base name with a suffix, but avoids duplication
     * when the name already ends with the same suffix.
     * @param baseName
     * @param suffix
     * @return
     */
    public static String withSuffix(String baseName, String suffix) {
        if (baseName == null || baseName.isEmpty() || suffix == null || suffix.isEmpty()) {
            return baseName;
        }
        // Avoid concatenation when the base already ends with the suffix
        if (baseName.endsWith(suffix)) {
            return baseName;
        }
        return baseName + suffix;
    }

    /**
     * A variant that adds a prefix + suffix, avoiding duplication.
     * @param prefix
     * @param baseName
     * @param suffix
     * @return
     */
    public static String withPrefixAndSuffix(String prefix, String baseName, String suffix) {
        String combined = prefix == null ? baseName : prefix + baseName;
        return withSuffix(combined, suffix);
    }
}
