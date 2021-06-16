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

import com.google.cloud.texttospeech.v1.ListVoicesRequest;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class TextToSpeechDiscovery implements GCPDiscovery {
  private static final String SERVICE = "textToSpeech";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::TextToSpeech::Voice";

    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
      ListVoicesRequest request =
        ListVoicesRequest.newBuilder().build();
      ListVoicesResponse response = textToSpeechClient.listVoices(request);
      response.getVoicesList().forEach(voice -> {
        var data = new GCPResource(voice.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(voice);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":voice"), data.toJsonNode()));
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
