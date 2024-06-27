package io.appform.ranger.discovery.bundle.id.weighted;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import io.appform.ranger.discovery.bundle.id.Id;
import io.appform.ranger.discovery.bundle.id.IdGenerator;
import io.appform.ranger.discovery.bundle.id.config.IdGeneratorConfig;
import io.appform.ranger.discovery.bundle.id.constraints.PartitionValidationConstraint;
import io.appform.ranger.discovery.bundle.id.formatter.IdFormatter;
import io.appform.ranger.discovery.bundle.id.formatter.IdFormatters;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Distributed ID Generation
 */
@SuppressWarnings("unused")
@Slf4j
abstract class DistributedIdGenerator {

    private static final int MINIMUM_ID_LENGTH = 22;
    protected static final SecureRandom SECURE_RANDOM = new SecureRandom(Long.toBinaryString(System.currentTimeMillis()).getBytes());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyMMddHHmmss");
    protected final FailsafeExecutor<Integer> retrier;
    private static final Pattern PATTERN = Pattern.compile("(.*)([0-9]{12})([0-9]{4})([0-9]{6})");
    private static final List<PartitionValidationConstraint> GLOBAL_CONSTRAINTS = new ArrayList<>();
    private static final Map<String, List<PartitionValidationConstraint>> DOMAIN_SPECIFIC_CONSTRAINTS = new HashMap<>();
    protected static final int NODE_ID = IdGenerator.getNodeId();
    private final Map<String, PartitionIdTracker[]> idStore = new ConcurrentHashMap<>();
    protected final IdFormatter idFormatter;
    protected final Function<String, Integer> partitionResolver;
    protected final IdGeneratorConfig idGeneratorConfig;
    private final Meter retryLimitBreachedMeter;

    /*  idStore Structure
    {
        prefix: [
            <timestamp>: {
                partitions: [
                {
                    ids: [],
                    pointer: <int>
                },
                {
                    ids: [],
                    pointer: <int>
                } ...
            ],
                counter: <int>
            }
        ]
    }
    */

    protected DistributedIdGenerator(final IdGeneratorConfig idGeneratorConfig,
                                     final Function<String, Integer> partitionResolverSupplier,
                                     final IdFormatter idFormatterInstance,
                                     final MetricRegistry metricRegistry) {
        this.idGeneratorConfig = idGeneratorConfig;
        this.partitionResolver = partitionResolverSupplier;
        this.idFormatter = idFormatterInstance;
        this.retryLimitBreachedMeter = metricRegistry.meter("idGenerator.RetryLimitBreached");
        RetryPolicy<Integer> retryPolicy = RetryPolicy.<Integer>builder()
                .withMaxAttempts(idGeneratorConfig.getRetryConfig().getPartitionRetryCount())
                .handleIf(throwable -> true)
                .handleResultIf(Objects::isNull)
                .build();
        retrier = Failsafe.with(Collections.singletonList(retryPolicy));
    }

    protected DistributedIdGenerator(final IdGeneratorConfig idGeneratorConfig,
                                     final Function<String, Integer> partitionResolverSupplier,
                                     final MetricRegistry metricRegistry) {
        this(idGeneratorConfig, partitionResolverSupplier, IdFormatters.partitionAware(), metricRegistry);
    }

    public synchronized void registerGlobalConstraints(final PartitionValidationConstraint... constraints) {
        registerGlobalConstraints(ImmutableList.copyOf(constraints));
    }

    public synchronized void registerGlobalConstraints(final List<PartitionValidationConstraint> constraints) {
        Preconditions.checkArgument(null != constraints && !constraints.isEmpty());
        GLOBAL_CONSTRAINTS.addAll(constraints);
    }

    public synchronized void registerDomainSpecificConstraints(
            final String domain,
            final PartitionValidationConstraint... validationConstraints) {
        registerDomainSpecificConstraints(domain, ImmutableList.copyOf(validationConstraints));
    }

    public synchronized void registerDomainSpecificConstraints(
            final String domain,
            final List<PartitionValidationConstraint> validationConstraints) {
        Preconditions.checkArgument(null != validationConstraints && !validationConstraints.isEmpty());
        DOMAIN_SPECIFIC_CONSTRAINTS.computeIfAbsent(domain, key -> new ArrayList<>())
                .addAll(validationConstraints);
    }

    /**
     * Generate id with given prefix
     *
     * @param prefix String prefix for ID to be generated
     * @return Generated ID
     */
    public Optional<Id> generate(final String prefix) {
        val targetPartitionId = getTargetPartitionId();
        return generateForPartition(prefix, targetPartitionId);
    }

    public Optional<Id> generateForPartition(final String prefix, final int targetPartitionId) {
        val currentTimestamp = new DateTime();
        val prefixIdMap = idStore.computeIfAbsent(prefix, k -> new PartitionIdTracker[idGeneratorConfig.getMaxDataBufferTimeInSeconds()]);
        val timeKey = getTimeKey(currentTimestamp.getMillis() / 1000);
        val partitionTracker = getPartitionTracker(prefixIdMap, currentTimestamp);
        val idCounter = generateForAllPartitions(
                partitionTracker,
                prefix,
                currentTimestamp,
                targetPartitionId);
        if (idCounter.isPresent()) {
            val id = String.format("%s%s", prefix, idFormatter.format(currentTimestamp, NODE_ID, idCounter.get()));
            return Optional.of(
                    Id.builder()
                            .id(id)
                            .exponent(idCounter.get())
                            .generatedDate(currentTimestamp.toDate())
                            .node(NODE_ID)
                            .build());
        } else {
            return Optional.empty();
        }
    }

    private Optional<Integer> generateForAllPartitions(final PartitionIdTracker partitionIdTracker,
                                                       final String prefix,
                                                       final DateTime timestamp,
                                                       final int targetPartitionId) {
        val idPool = partitionIdTracker.getPartition(targetPartitionId);
        int retryCount = 0;
        try {
            while (retryCount < idGeneratorConfig.getRetryConfig().getIdGenerationRetryCount()) {
                val counterValue = partitionIdTracker.getIdCounter();
                val txnId = String.format("%s%s", prefix, idFormatter.format(timestamp, NODE_ID, counterValue));
                val mappedPartitionId = partitionResolver.apply(txnId);
                partitionIdTracker.addId(mappedPartitionId, counterValue);
                retryCount += 1;
                val idOptional = idPool.getNextId();
                if (idOptional.isPresent()) {
                    return idOptional;
                }
            }

//            Retry Limit Breached
            retryLimitBreachedMeter.mark();
            log.debug("Retry Limit reached - {} - {}", retryCount, targetPartitionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error while generating IDs", e);
            return Optional.empty();
        }
    }

    /**
     * Generate id that matches all passed constraints.
     * NOTE: There are performance implications for this.
     * The evaluation of constraints will take its toll on ID generation rates.
     *
     * @param prefix String prefix
     * @param domain Domain for constraint selection
     * @return Return generated id or empty if it was impossible to satisfy constraints and generate
     */
    public Optional<Id> generateWithConstraints(final String prefix, final String domain) {
        return generateWithConstraints(prefix, domain, true);
    }

    /**
     * Generate id that matches all passed constraints.
     * NOTE: There are performance implications for this.
     * The evaluation of constraints will take its toll on id generation rates.
     *
     * @param prefix     String prefix
     * @param domain     Domain for constraint selection
     * @param skipGlobal Skip global constrains and use only passed ones
     * @return ID if it could be generated
     */
    public Optional<Id> generateWithConstraints(final String prefix, final String domain, final boolean skipGlobal) {
        val targetPartitionId = getTargetPartitionId(DOMAIN_SPECIFIC_CONSTRAINTS.getOrDefault(domain, Collections.emptyList()), skipGlobal);
        return targetPartitionId.map(partitionId -> {
            val id = generateForPartition(prefix, partitionId);
            return id.orElse(null);
        });
    }

    public Optional<Id> generateWithConstraints(final String prefix,
                                                final List<PartitionValidationConstraint> inConstraints,
                                                final boolean skipGlobal) {
        val targetPartitionId = getTargetPartitionId(inConstraints, skipGlobal);
        return targetPartitionId.map(partitionId -> {
            val id = generateForPartition(prefix, partitionId);
            return id.orElse(null);
        });
    }

    /**
     * Generate id by parsing given string
     *
     * @param idString String idString
     * @return ID if it could be generated
     */
    public Optional<Id> parse(final String idString) {
        if (idString == null
                || idString.length() < MINIMUM_ID_LENGTH) {
            return Optional.empty();
        }
        try {
            val matcher = PATTERN.matcher(idString);
            if (matcher.find()) {
                return Optional.of(Id.builder()
                        .id(idString)
                        .node(Integer.parseInt(matcher.group(3)))
                        .exponent(Integer.parseInt(matcher.group(4)))
                        .generatedDate(DATE_TIME_FORMATTER.parseDateTime(matcher.group(2)).toDate())
                        .build());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Could not parse idString {}", e.getMessage());
            return Optional.empty();
        }
    }

    protected abstract int getTargetPartitionId();

    protected abstract Optional<Integer> getTargetPartitionId(final List<PartitionValidationConstraint> inConstraints,
                                                              final boolean skipGlobal);

    protected boolean validateId(final List<PartitionValidationConstraint> inConstraints,
                                 final int partitionId,
                                 final boolean skipGlobal) {
        //First evaluate global constraints
        val failedGlobalConstraint
                = skipGlobal
                ? null
                : GLOBAL_CONSTRAINTS.stream()
                .filter(constraint -> !constraint.isValid(partitionId))
                .findFirst()
                .orElse(null);
        if (null != failedGlobalConstraint) {
            return false;
        }
        //Evaluate param constraints
        val failedLocalConstraint
                = null == inConstraints
                ? null
                : inConstraints.stream()
                .filter(constraint -> !constraint.isValid(partitionId))
                .findFirst()
                .orElse(null);
        return null == failedLocalConstraint;
    }

    private int getTimeKey(long timeInSeconds) {
        return (int) timeInSeconds % idGeneratorConfig.getMaxDataBufferTimeInSeconds();
    }

    private synchronized PartitionIdTracker getPartitionTracker(PartitionIdTracker[] partitionTrackerList, final DateTime timestamp) {
        val timeKey = getTimeKey(timestamp.getMillis() / 1000);
        if (timeKey >= partitionTrackerList.length) {
            throw new IndexOutOfBoundsException("Key should be less than " + partitionTrackerList.length);
        }
        if (partitionTrackerList[timeKey] == null) {
            partitionTrackerList[timeKey] = new PartitionIdTracker(idGeneratorConfig.getPartitionCount(), idGeneratorConfig.getIdPoolSize(), timestamp);
        }
        val partitionTracker = partitionTrackerList[timeKey];
        if (partitionTracker.getTimestamp().getMillis() / 1000 != timestamp.getMillis() / 1000) {
            partitionTracker.reset(timestamp);
        }
        return partitionTracker;
    }

}
