/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.workers.temporal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.airbyte.commons.json.Jsons;
import io.airbyte.config.JobCheckConnectionConfig;
import io.airbyte.config.JobDiscoverCatalogConfig;
import io.airbyte.config.JobGetSpecConfig;
import io.airbyte.config.JobSyncConfig;
import io.airbyte.config.StandardCheckConnectionInput;
import io.airbyte.config.StandardDiscoverCatalogInput;
import io.airbyte.config.StandardSyncInput;
import io.airbyte.protocol.models.ConfiguredAirbyteCatalog;
import io.airbyte.scheduler.models.IntegrationLauncherConfig;
import io.airbyte.scheduler.models.JobRunConfig;
import io.temporal.client.WorkflowClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemporalClientTest {

  private static final long JOB_ID = 11L;
  private static final int ATTEMPT_ID = 21;
  private static final JobRunConfig JOB_RUN_CONFIG = new JobRunConfig().withJobId(JOB_ID).withAttemptId((long) ATTEMPT_ID);
  private static final String IMAGE_NAME1 = "hms invincible";
  private static final String IMAGE_NAME2 = "hms defiant";
  private static final IntegrationLauncherConfig LAUNCHER_CONFIG = new IntegrationLauncherConfig()
      .withJobId(JOB_ID)
      .withAttemptId((long) ATTEMPT_ID)
      .withDockerImage(IMAGE_NAME1);

  private WorkflowClient workflowClient;
  private TemporalClient temporalClient;

  @BeforeEach
  void setup() {
    workflowClient = mock(WorkflowClient.class);
    temporalClient = new TemporalClient(workflowClient);
  }

  @Test
  void testSubmitGetSpec() throws TemporalJobException {
    final SpecWorkflow specWorkflow = mock(SpecWorkflow.class);
    when(workflowClient.newWorkflowStub(SpecWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.GET_SPEC))).thenReturn(specWorkflow);
    final JobGetSpecConfig getSpecConfig = new JobGetSpecConfig().withDockerImage(IMAGE_NAME1);

    temporalClient.submitGetSpec(JOB_ID, ATTEMPT_ID, getSpecConfig);
    specWorkflow.run(JOB_RUN_CONFIG, LAUNCHER_CONFIG);
    verify(workflowClient).newWorkflowStub(SpecWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.GET_SPEC));
  }

  @Test
  void testSubmitCheckConnection() throws TemporalJobException {
    final CheckConnectionWorkflow checkConnectionWorkflow = mock(CheckConnectionWorkflow.class);
    when(workflowClient.newWorkflowStub(CheckConnectionWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.CHECK_CONNECTION)))
        .thenReturn(checkConnectionWorkflow);
    final JobCheckConnectionConfig checkConnectionConfig = new JobCheckConnectionConfig()
        .withDockerImage(IMAGE_NAME1)
        .withConnectionConfiguration(Jsons.emptyObject());
    final StandardCheckConnectionInput input = new StandardCheckConnectionInput()
        .withConnectionConfiguration(checkConnectionConfig.getConnectionConfiguration());

    temporalClient.submitCheckConnection(JOB_ID, ATTEMPT_ID, checkConnectionConfig);
    checkConnectionWorkflow.run(JOB_RUN_CONFIG, LAUNCHER_CONFIG, input);
    verify(workflowClient).newWorkflowStub(CheckConnectionWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.CHECK_CONNECTION));
  }

  @Test
  void testSubmitDiscoverSchema() throws TemporalJobException {
    final DiscoverCatalogWorkflow discoverCatalogWorkflow = mock(DiscoverCatalogWorkflow.class);
    when(workflowClient.newWorkflowStub(DiscoverCatalogWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.DISCOVER_SCHEMA)))
        .thenReturn(discoverCatalogWorkflow);
    final JobDiscoverCatalogConfig checkConnectionConfig = new JobDiscoverCatalogConfig()
        .withDockerImage(IMAGE_NAME1)
        .withConnectionConfiguration(Jsons.emptyObject());
    final StandardDiscoverCatalogInput input = new StandardDiscoverCatalogInput()
        .withConnectionConfiguration(checkConnectionConfig.getConnectionConfiguration());

    temporalClient.submitDiscoverSchema(JOB_ID, ATTEMPT_ID, checkConnectionConfig);
    discoverCatalogWorkflow.run(JOB_RUN_CONFIG, LAUNCHER_CONFIG, input);
    verify(workflowClient).newWorkflowStub(DiscoverCatalogWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.DISCOVER_SCHEMA));
  }

  @Test
  void testSubmitSync() throws TemporalJobException {
    final SyncWorkflow discoverCatalogWorkflow = mock(SyncWorkflow.class);
    when(workflowClient.newWorkflowStub(SyncWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.SYNC)))
        .thenReturn(discoverCatalogWorkflow);
    final JobSyncConfig syncConfig = new JobSyncConfig()
        .withSourceDockerImage(IMAGE_NAME1)
        .withSourceDockerImage(IMAGE_NAME2)
        .withSourceConfiguration(Jsons.emptyObject())
        .withDestinationConfiguration(Jsons.emptyObject())
        .withConfiguredAirbyteCatalog(new ConfiguredAirbyteCatalog());
    final StandardSyncInput input = new StandardSyncInput()
        .withPrefix(syncConfig.getPrefix())
        .withSourceConfiguration(syncConfig.getSourceConfiguration())
        .withDestinationConfiguration(syncConfig.getDestinationConfiguration())
        .withCatalog(syncConfig.getConfiguredAirbyteCatalog())
        .withState(syncConfig.getState());

    final IntegrationLauncherConfig destinationLauncherConfig = new IntegrationLauncherConfig()
        .withJobId(JOB_ID)
        .withAttemptId((long) ATTEMPT_ID)
        .withDockerImage(IMAGE_NAME2);

    temporalClient.submitSync(JOB_ID, ATTEMPT_ID, syncConfig);
    discoverCatalogWorkflow.run(JOB_RUN_CONFIG, LAUNCHER_CONFIG, destinationLauncherConfig, input);
    verify(workflowClient).newWorkflowStub(SyncWorkflow.class, TemporalUtils.getWorkflowOptions(TemporalJobType.SYNC));
  }

}
