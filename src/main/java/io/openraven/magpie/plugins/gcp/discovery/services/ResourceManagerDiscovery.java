/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManagerOptions;
import com.google.cloud.resourcemanager.v3.Organization;
import com.google.cloud.resourcemanager.v3.OrganizationsClient;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class ResourceManagerDiscovery implements GCPDiscovery {
  private static final String SERVICE = "resourceManager";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverOrganization(mapper, projectId, session, emitter);
    discoverProjects(mapper, projectId, session, emitter);
  }

  private void discoverOrganization(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ResourceManager::Organization";

    try (var projectsClient = OrganizationsClient.create()) {
      for (var organization : projectsClient.searchOrganizations("").iterateAll()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, organization.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(organization))
          .build();

        discoverOrganizationIamPolicy(projectsClient, organization, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":organization"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverOrganizationIamPolicy(OrganizationsClient projectsClient, Organization organization, MagpieResource data) {
    final String fieldName = "iamPolicy";

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, projectsClient.getIamPolicy(organization.getName()).toBuilder()));
  }

  private void discoverProjects(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ResourceManager::Project";

    try (var projectsClient = ProjectsClient.create()) {
      getProjectList().forEach(project -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, project.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(project))
          .build();

        discoverProjectIamPolicy(projectsClient, project, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":project"), data.toJsonNode()));
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverProjectIamPolicy(ProjectsClient projectsClient, Project project, MagpieResource data) {
    final String fieldName = "iamPolicy";
    String resource = ProjectName.of(project.getProjectId()).toString();

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, projectsClient.getIamPolicy(resource).toBuilder()));
  }

  Iterable<com.google.cloud.resourcemanager.Project> getProjectList() {
    var resourceManager = ResourceManagerOptions.getDefaultInstance().getService();
    return resourceManager.list().iterateAll();
  }
}
