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

package io.appform.ranger.zookeeper.healthservice.monitor.sample;

import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.healthservice.TimeEntity;
import io.appform.ranger.core.healthservice.monitor.IsolatedHealthMonitor;
import io.appform.ranger.core.healthservice.monitor.sample.DiskSpaceMonitor;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class DiskSpaceMonitorTest {

    @Test
    public void testGetCount() {
        val diskSpaceMonitor = new DiskSpaceMonitor("/", 1000, new TimeEntity(2, TimeUnit.SECONDS));
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
        Assert.assertEquals(HealthcheckStatus.healthy, diskSpaceMonitor.monitor());
    }
}