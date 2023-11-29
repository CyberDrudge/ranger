/*
 * Copyright 2015 Flipkart Internet Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.ranger.zookeeper.healthservice.monitor;

import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.healthservice.monitor.RollingWindowHealthQueue;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RollingWindowHealthQueueTest {

    @Test
    void testCheckInRollingWindow1() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(5, 3);
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
    }

    @Test
    void testCheckInRollingWindowEdge() {
        try {
            val rollingWindowHealthQueue = new RollingWindowHealthQueue(1, 3);
            Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        } catch (Exception u) {
            Assertions.assertTrue(u instanceof UnsupportedOperationException);
        }
    }

    @Test
    void testCheckInRollingWindowEdge2() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(3, 3);
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
    }

    @Test
    void testCheckInRollingWindowEdge3() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(5, 1);
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
    }


    @Test
    void testCheckInRollingWindow2() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(5, 3);
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
    }


    @Test
    void testCheckInRollingWindow3() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(5, 3);
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
    }

    @Test
    void testCheckInRollingWindow4() {
        val rollingWindowHealthQueue = new RollingWindowHealthQueue(5, 3);
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
        Assertions.assertFalse(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.healthy));
        Assertions.assertTrue(rollingWindowHealthQueue.checkInRollingWindow(HealthcheckStatus.unhealthy));
    }
}