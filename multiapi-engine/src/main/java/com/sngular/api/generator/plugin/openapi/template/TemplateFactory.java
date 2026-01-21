/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.plugin.openapi.template;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.sngular.api.generator.plugin.common.template.CommonTemplateFactory;
import com.sngular.api.generator.plugin.openapi.model.AuthObject;
import com.sngular.api.generator.plugin.openapi.model.PathObject;
import com.sngular.api.generator.plugin.openapi.parameter.SpecFile;
import org.apache.commons.lang3.StringUtils;

public class TemplateFactory extends CommonTemplateFactory {

  private static final String DEFAULT_API_PACKAGE = "com.sngular.api";

  public TemplateFactory(
      boolean enableOverwrite,
      final File targetFolder,
      final String processedGeneratedSourcesFolder,
      final File baseDir) {
    super(enableOverwrite, targetFolder, processedGeneratedSourcesFolder, baseDir, new ClasspathTemplateLoader());
  }

  public final void clearData() {
    cleanData();
  }

  @Override
  protected void clearRoot() {
    delFromRoot("className");
    delFromRoot("pathObjects");
    delFromRoot("packageApi");
    delFromRoot("packageModel");
    delFromRoot("exceptionPackage");
    delFromRoot("authObject");
    delFromRoot("clientPackage");
    delFromRoot("javaEEPackage");
    delFromRoot("docTitle");
    delFromRoot("docVersion");
    delFromRoot("docDescription");
    delFromRoot("docContactName");
    delFromRoot("docContactEmail");
    delFromRoot("docContactUrl");
    delFromRoot("docLicenseName");
    delFromRoot("docLicenseUrl");
    delFromRoot("docExternalDocsDescription");
    delFromRoot("docExternalDocsUrl");
    delFromRoot("docTags");
    delFromRoot("docExtensions");
  }

  public final void fillTemplates() {
    generateTemplates();
  }

  public final void setDocumentMetadata(final JsonNode openAPI) {
    if (openAPI == null || !openAPI.has("info")) {
      return;
    }
    final var info = openAPI.get("info");
    addToRoot("docTitle", info.path("title").asText(null));
    addToRoot("docVersion", info.path("version").asText(null));
    addToRoot("docDescription", info.path("description").asText(null));
    if (info.has("contact")) {
      final var contact = info.get("contact");
      addToRoot("docContactName", contact.path("name").asText(null));
      addToRoot("docContactEmail", contact.path("email").asText(null));
      addToRoot("docContactUrl", contact.path("url").asText(null));
    }
    if (info.has("license")) {
      final var license = info.get("license");
      addToRoot("docLicenseName", license.path("name").asText(null));
      addToRoot("docLicenseUrl", license.path("url").asText(null));
    }
    if (openAPI.has("externalDocs")) {
      final var external = openAPI.get("externalDocs");
      addToRoot("docExternalDocsDescription", external.path("description").asText(null));
      addToRoot("docExternalDocsUrl", external.path("url").asText(null));
    }
    if (openAPI.has("tags")) {
      final var tagsIt = openAPI.get("tags").elements();
      final var tags = new java.util.ArrayList<String>();
      while (tagsIt.hasNext()) {
        final var tag = tagsIt.next();
        if (tag.has("name")) {
          tags.add(tag.get("name").asText());
        }
      }
      addToRoot("docTags", tags);
    }
    final var extensions = new java.util.HashMap<String, String>();
    info.fields().forEachRemaining(entry -> {
      if (entry.getKey().startsWith("x-")) {
        extensions.put(entry.getKey(), entry.getValue().toString());
      }
    });
    openAPI.fields().forEachRemaining(entry -> {
      if (entry.getKey().startsWith("x-")) {
        extensions.put(entry.getKey(), entry.getValue().toString());
      }
    });
    addToRoot("docExtensions", extensions.isEmpty() ? null : extensions);

    if (openAPI.has("servers")) {
      final var servers = new ArrayList<HashMap<String, Object>>();
      openAPI.get("servers").forEach(server -> {
        final var s = new HashMap<String, Object>();
        s.put("url", server.path("url").asText(null));
        s.put("description", server.path("description").asText(null));
        if (server.has("security")) {
          final var secNames = new ArrayList<String>();
          server.get("security").forEach(sec -> sec.fieldNames().forEachRemaining(secNames::add));
          s.put("security", secNames);
        }
        if (!s.isEmpty()) {
          servers.add(s);
        }
      });
      addToRoot("docServers", servers.isEmpty() ? null : servers);
    }
  }

  public final void fillTemplateWebClient(final String filePathToSave) throws IOException {
    writeTemplateToFile(TemplateIndexConstants.TEMPLATE_WEB_CLIENT, filePathToSave, "ApiWebClient");
  }

  public final void fillTemplateRestClient(final String filePathToSave) throws IOException {
    writeTemplateToFile(TemplateIndexConstants.TEMPLATE_REST_CLIENT, filePathToSave, "ApiRestClient");
  }

  public final void fillTemplateAuth(final String apiPackage, final String authName) throws IOException {
    writeTemplateToFile(createNameTemplate(authName), apiPackage, authName);
  }

  private String createNameTemplate(final String classNameAuth) {
    return "template" + classNameAuth + ".ftlh";
  }

  public final void fillTemplate(
      final SpecFile specFile, final String className,
      final List<PathObject> pathObjects, final AuthObject authObject) throws IOException {

    addToRoot("className", className);
    addToRoot("pathObjects", pathObjects);

    if (Objects.nonNull(specFile.getApiPackage())) {
      addToRoot("packageApi", StringUtils.defaultIfEmpty(specFile.getApiPackage(), DEFAULT_API_PACKAGE));
    }
    if (Objects.nonNull(specFile.getModelPackage())) {
      addToRoot("packageModel", specFile.getModelPackage());
      addToRoot("exceptionPackage", specFile.getModelPackage());
    }

    if (specFile.isCallMode()) {
      addToRoot("authObject", authObject);
      addToRoot("clientPackage", specFile.getClientPackage());
    }

    writeTemplateToFile(specFile.isCallMode() ? getTemplateClientApi(specFile) : getTemplateApi(specFile),
                        StringUtils.defaultIfEmpty(specFile.getApiPackage(), DEFAULT_API_PACKAGE), className + "Api");
  }

  private String getTemplateClientApi(final SpecFile specFile) {
    return specFile.isReactive() ? TemplateIndexConstants.TEMPLATE_CALL_WEB_API : TemplateIndexConstants.TEMPLATE_CALL_REST_API;
  }

  private String getTemplateApi(final SpecFile specFile) {
    return specFile.isReactive() ? TemplateIndexConstants.TEMPLATE_REACTIVE_API : TemplateIndexConstants.TEMPLATE_INTERFACE_API;
  }

  public final void calculateJavaEEPackage(final Integer springBootVersion) {
    if (3 <= springBootVersion) {
      addToRoot("javaEEPackage", "jakarta");
    } else {
      addToRoot("javaEEPackage", "javax");
    }
  }

  public final void setPackageName(final String packageName) {
    addToRoot("package", packageName);
  }

  public final void setModelPackageName(final String packageName) {
    addToRoot("packageModel", packageName);
  }

  public final void setWebClientPackageName(final String packageName) {
    addToRoot("packageClient", packageName);
  }

  public final void setAuthPackageName(final String packageName) {
    addToRoot("packageAuth", packageName);
  }

}
