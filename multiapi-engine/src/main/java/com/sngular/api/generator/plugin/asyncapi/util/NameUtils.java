package com.sngular.api.generator.plugin.asyncapi.util;

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
        if (baseName.endsWith(suffix)) {
            return baseName;
        }
        return baseName + suffix;
    }

    /**
     * Adds a suffix when it is not already present (idempotent helper).
     */
    public static String withOneSuffix(String baseName, String suffix) {
        return withSuffix(baseName, suffix);
    }

    /**
     * A variant that adds a prefix + suffix, avoiding duplication.
     * @param prefix
     * @param baseName
     * @param suffix
     * @return
     */
    public static String withPrefixAndSuffix(String prefix, String baseName, String suffix) {
        if (baseName == null || baseName.isEmpty()) {
            return baseName;
        }

        String withPrefix = (prefix == null ? "" : prefix) + baseName;
        return withSuffix(withPrefix, suffix);
    }
}
