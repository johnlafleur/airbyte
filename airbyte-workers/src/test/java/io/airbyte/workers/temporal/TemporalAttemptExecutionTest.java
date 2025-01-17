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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.airbyte.commons.functional.CheckedConsumer;
import io.airbyte.commons.functional.CheckedFunction;
import io.airbyte.scheduler.models.JobRunConfig;
import io.airbyte.workers.WorkerConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemporalAttemptExecutionTest {

  private static final long JOB_ID = 11L;
  private static final int ATTEMPT_ID = 21;
  private static final JobRunConfig JOB_RUN_CONFIG = new JobRunConfig().withJobId(JOB_ID).withAttemptId((long) ATTEMPT_ID);

  private Path jobRoot;
  private TemporalJobException expectedException;

  private CheckedFunction<Path, String, Exception> execution;
  private BiConsumer<Path, Long> mdcSetter;
  private CheckedConsumer<Path, IOException> jobRootDirCreator;

  private TemporalAttemptExecution<String> attemptExecution;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() throws IOException {
    final Path workspaceRoot = Files.createTempDirectory(Path.of("/tmp"), "temporal_attempt_execution_test");
    jobRoot = workspaceRoot.resolve(String.valueOf(JOB_ID)).resolve(String.valueOf(ATTEMPT_ID));
    expectedException = new TemporalJobException(jobRoot.resolve(WorkerConstants.LOG_FILENAME));

    execution = mock(CheckedFunction.class);
    mdcSetter = mock(BiConsumer.class);
    jobRootDirCreator = mock(CheckedConsumer.class);

    attemptExecution = new TemporalAttemptExecution<>(workspaceRoot, JOB_RUN_CONFIG, execution, mdcSetter, jobRootDirCreator);
  }

  @Test
  void testGet() throws Exception {
    final String expected = "louis XVI";
    when(execution.apply(jobRoot)).thenReturn(expected);

    final String actual = attemptExecution.get();

    assertEquals(expected, actual);
    verify(execution).apply(jobRoot);
    verify(mdcSetter).accept(jobRoot, JOB_ID);
    verify(jobRootDirCreator).accept(jobRoot);
  }

  @Test
  void testThrowsCheckedException() throws Exception {
    when(execution.apply(jobRoot)).thenThrow(new IOException());

    final TemporalJobException actualException = assertThrows(TemporalJobException.class, () -> attemptExecution.get());
    assertEquals(expectedException.getLogPath(), actualException.getLogPath());
    assertEquals(IOException.class, actualException.getCause().getClass());

    verify(execution).apply(jobRoot);
    verify(mdcSetter).accept(jobRoot, JOB_ID);
    verify(jobRootDirCreator).accept(jobRoot);
  }

  @Test
  void testThrowsUnCheckedException() throws Exception {
    when(execution.apply(jobRoot)).thenThrow(new IllegalArgumentException());

    final TemporalJobException actualException = assertThrows(TemporalJobException.class, () -> attemptExecution.get());
    assertEquals(expectedException.getLogPath(), actualException.getLogPath());
    assertEquals(IllegalArgumentException.class, actualException.getCause().getClass());

    verify(execution).apply(jobRoot);
    verify(mdcSetter).accept(jobRoot, JOB_ID);
    verify(jobRootDirCreator).accept(jobRoot);
  }

  @Test
  void testThrowsTemporalJobExceptionException() throws Exception {
    final Path otherFilePath = jobRoot.resolve("other file path");
    when(execution.apply(jobRoot)).thenThrow(new TemporalJobException(otherFilePath));

    final TemporalJobException actualException = assertThrows(TemporalJobException.class, () -> attemptExecution.get());
    assertEquals(otherFilePath, actualException.getLogPath());
    assertNull(actualException.getCause());

    verify(execution).apply(jobRoot);
    verify(mdcSetter).accept(jobRoot, JOB_ID);
    verify(jobRootDirCreator).accept(jobRoot);
  }

}
