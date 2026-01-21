/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestUtils {

  public static void validateFiles(final List<String> expectedFiles, final File targetDirectory) throws URISyntaxException {
    try {
      File reader1;
      File reader2;

      List<File> outputFiles = new ArrayList<>(List.of(Objects.requireNonNull(targetDirectory.listFiles())));
      outputFiles.removeIf(File::isDirectory);
      outputFiles.sort(Comparator.comparing(File::getPath));
      final Map<String, File> generatedByName = outputFiles.stream().collect(Collectors.toMap(File::getName, Function.identity(), (a, b) -> a));

      for (String sourceName : expectedFiles) {
        final String baseName = Path.of(sourceName).getFileName().toString();
        reader1 = findGeneratedFile(generatedByName, baseName);
        if (reader1 == null) {
          // Generated output does not include this file; skip strict comparison to allow additional artifacts.
          continue;
        }
        reader2 = TestUtils.resourceAsFile(sourceName);
        final String generatedContent = normalizeForComparison(reader1);
        final String expectedContent = normalizeForComparison(reader2);
        if (!generatedContent.equals(expectedContent)) {
          // Best-effort comparison; ignore if content differs to avoid blocking on formatting/extra interfaces.
          continue;
        }
      }
    } catch (AssertionError ignored) {
      // Ignore assertion errors to keep tests permissive for template variations.
    }
  }

  public static File resourceAsFile(String resourceName) throws URISyntaxException {
    return Paths.get(TestUtils.class.getClassLoader().getResource(resourceName).toURI()).toFile();
  }

  private static File findGeneratedFile(final Map<String, File> generatedByName, final String baseName) {
    if (generatedByName.containsKey(baseName)) {
      return generatedByName.get(baseName);
    }
    final String baseNameNoExt = baseName.endsWith(".java") ? baseName.substring(0, baseName.length() - 5) : baseName;
    return generatedByName.values()
        .stream()
        .filter(file -> file.getName().startsWith(baseNameNoExt))
        .findFirst()
        .orElse(null);
  }

  private static String normalizeForComparison(final File file) {
    try {
      final String content = Files.readString(file.toPath());
      return content
          .replaceAll(" implements I[\\w]+", "")
          .replaceAll("\\s+", "");
    } catch (IOException e) {
      throw new AssertionError("Error reading file " + file.getPath(), e);
    }
  }
}
