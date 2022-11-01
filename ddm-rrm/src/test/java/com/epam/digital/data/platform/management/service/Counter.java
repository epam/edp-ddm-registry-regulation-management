/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.management.service;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

@Slf4j
class Counter {

  private String threadName;
  private final int steps;
  private int curStep;

  private int completedIterations = 0;

  private final List<AssertionFailedError> errorList;

  public Counter(int steps) {
    this.steps = steps;
    this.curStep = steps - 1;
    errorList = new ArrayList<>();
  }

  public void check(int step) {

    var newThreadName = Thread.currentThread().getName();
    log.info(
        "Called check for thread {} (cur {}), step {} (prev {} total {})", newThreadName,
        threadName, step, curStep, steps);
    synchronized (this) {
      if (step == 0) {
        //checks that this is the first step
        try {
          Assertions.assertEquals(curStep, (steps - 1));
        } catch (AssertionFailedError e) {
          errorList.add(e);
          throw e;
        }

        threadName = newThreadName;
        completedIterations++;
        curStep = step;
      } else {
        // checks that this is the next step in the cur thread
        try {
          Assertions.assertEquals(threadName, newThreadName);
          Assertions.assertEquals(curStep + 1, step);
          Assertions.assertTrue(step < steps);
          Assertions.assertTrue(step > 0);
        } catch (AssertionFailedError e) {
          errorList.add(e);
          throw e;
        }

        curStep++;
      }
    }
  }

  public int getCompletedIterations() {
    synchronized (this) {
      return curStep + 1 == steps ? completedIterations : completedIterations - 1;
    }
  }

  public List<AssertionFailedError> getErrorList() {
    synchronized (this) {
      return errorList;
    }
  }
}
