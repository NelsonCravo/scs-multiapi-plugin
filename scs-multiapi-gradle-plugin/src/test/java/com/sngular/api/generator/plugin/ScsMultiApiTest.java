/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.sngular.api.generator.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import com.sngular.api.generator.plugin.OpenApiTask;
import com.sngular.api.generator.plugin.AsyncApiTask;

class ScsMultiApiTest {

  @Test
  void greeterPluginAddsOpenApiTaskToProject() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("com.sngular.scs-multiapi-gradle-plugin");

    assertTrue(OpenApiTask.class.isAssignableFrom(project.getTasks().getByName("openApiTask").getClass()));
  }

  @Test
  void greeterPluginAddsAsyncApiTaskTaskToProject() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("com.sngular.scs-multiapi-gradle-plugin");

    assertTrue(AsyncApiTask.class.isAssignableFrom(project.getTasks().getByName("asyncApiTask").getClass()));
  }

}
