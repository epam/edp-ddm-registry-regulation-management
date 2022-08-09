package com.epam.digital.data.platform.management.service;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

@Slf4j
class Counter {

  private String threadName;
  private int steps;
  private int curStep;

  private int completedIterations = 0;

  private List<AssertionFailedError> errorList;

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
