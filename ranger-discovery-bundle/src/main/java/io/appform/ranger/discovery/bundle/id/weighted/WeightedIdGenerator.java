package io.appform.ranger.discovery.bundle.id.weighted;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.appform.ranger.discovery.bundle.id.config.IdGeneratorRetryConfig;
import io.appform.ranger.discovery.bundle.id.config.PartitionRange;
import io.appform.ranger.discovery.bundle.id.config.WeightedIdConfig;
import io.appform.ranger.discovery.bundle.id.constraints.PartitionValidationConstraint;
import io.appform.ranger.discovery.bundle.id.formatter.IdFormatter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Weighted Id Generation
 */
@Slf4j
public class WeightedIdGenerator extends DistributedIdGenerator {
    private int maxShardWeight;
    private final RangeMap<Integer, PartitionRange> partitionRangeMap;

    public WeightedIdGenerator(final int partitionSize,
                               final Function<String, Integer> partitionResolverSupplier,
                               final IdGeneratorRetryConfig retryConfig,
                               final WeightedIdConfig weightedIdConfig) {
        super(partitionSize, partitionResolverSupplier, retryConfig);
        partitionRangeMap = createWeightRangeMap(weightedIdConfig);
    }

    public WeightedIdGenerator(final int partitionSize,
                               final Function<String, Integer> partitionResolverSupplier,
                               final IdGeneratorRetryConfig retryConfig,
                               final WeightedIdConfig weightedIdConfig,
                               final IdFormatter idFormatterInstance) {
        super(partitionSize, partitionResolverSupplier, retryConfig, idFormatterInstance);
        partitionRangeMap = createWeightRangeMap(weightedIdConfig);
    }

    private RangeMap<Integer, PartitionRange> createWeightRangeMap(final WeightedIdConfig weightedIdConfig) {
        RangeMap<Integer, PartitionRange> partitionGroups = TreeRangeMap.create();
        int end = -1;
        for (val shard : weightedIdConfig.getPartitions()) {
            int start = end + 1;
            end += shard.getWeight();
            partitionGroups.put(Range.closed(start, end), shard.getPartitionRange());
        }
        maxShardWeight = end;
        return partitionGroups;
    }

    @Override
    protected int getTargetPartitionId() {
        val randomNum = SECURE_RANDOM.nextInt(maxShardWeight);
        val partitionRange = Objects.requireNonNull(partitionRangeMap.getEntry(randomNum)).getValue();
        return SECURE_RANDOM.nextInt(partitionRange.getEnd() - partitionRange.getStart() + 1) + partitionRange.getStart();
    }

    @Override
    protected Optional<Integer> getTargetPartitionId(final List<PartitionValidationConstraint> inConstraints, final boolean skipGlobal) {
        return Optional.ofNullable(
                retrier.get(this::getTargetPartitionId))
                .filter(key -> validateId(inConstraints, key, skipGlobal));
    }
}

