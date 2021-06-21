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

import com.google.cloud.monitoring.v3.AlertPolicyServiceClient;
import com.google.cloud.monitoring.v3.GroupServiceClient;
import com.google.cloud.monitoring.v3.ServiceMonitoringServiceClient;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.monitoring.v3.AlertPolicy;
import com.google.monitoring.v3.Group;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class MonitoringDiscovery implements GCPDiscovery {
  private static final String SERVICE = "monitoring";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    discoverMonitoringGroups(projectId, session, emitter);
    discoverAlertPolicies(projectId, session, emitter);
    discoverServices(projectId, session, emitter);
  }

  private void discoverMonitoringGroups(String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::Monitoring::Group";

    try (GroupServiceClient groupServiceClient = GroupServiceClient.create()) {
      for (Group group : groupServiceClient.listGroups(ProjectName.of(projectId)).iterateAll()) {
        var data = new GCPResource(group.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(group);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":group"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverAlertPolicies(String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::Monitoring::AlertPolicy";

    try (AlertPolicyServiceClient alertPolicyServiceClient = AlertPolicyServiceClient.create()) {
      for (AlertPolicy alertPolicy : alertPolicyServiceClient.listAlertPolicies(ProjectName.of(projectId)).iterateAll()) {
        var data = new GCPResource(alertPolicy.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(alertPolicy);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":alertPolicy"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverServices(String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::Monitoring::Service";

    try (var serviceMonitoringServiceClient = ServiceMonitoringServiceClient.create()) {
      for (var service : serviceMonitoringServiceClient.listServices(ProjectName.of(projectId)).iterateAll()) {
        var data = new GCPResource(service.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(service);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":service"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
