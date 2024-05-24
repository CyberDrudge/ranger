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
package io.appform.ranger.core.finder;

import io.appform.ranger.core.finder.nodeselector.RoundRobinServiceNodeSelector;
import io.appform.ranger.core.finder.serviceregistry.MapBasedServiceRegistry;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.model.ShardSelector;
import io.appform.ranger.core.units.TestNodeData;
import io.appform.ranger.core.utils.RangerTestUtils;
import io.appform.ranger.core.utils.RegistryTestUtils;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

class SimpleShardFinderTest {

    static class TestSimpleShardSelector<T> implements ShardSelector<T, MapBasedServiceRegistry<T>> {

        @Override
        public List<ServiceNode<T>> nodes(Predicate<T> criteria, MapBasedServiceRegistry<T> serviceRegistry) {
            return Collections.emptyList();
        }
    }

    @Test
    void testSimpleShardedFinder() {
        val serviceRegistry = RegistryTestUtils.getServiceRegistry();
        val shardSelector = new TestSimpleShardSelector<TestNodeData>();
        val roundRobinServiceNodeSelector = new RoundRobinServiceNodeSelector<TestNodeData>();
        val simpleShardedFinder = new SimpleShardedServiceFinder<>(
                serviceRegistry, shardSelector, roundRobinServiceNodeSelector);
        val testNodeDataServiceNode = simpleShardedFinder.get(
                RangerTestUtils.getCriteria(2));
        Assertions.assertFalse(testNodeDataServiceNode.isPresent());
    }
}
