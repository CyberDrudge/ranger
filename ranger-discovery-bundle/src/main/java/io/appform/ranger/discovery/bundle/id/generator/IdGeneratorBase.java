package io.appform.ranger.discovery.bundle.id.generator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.appform.ranger.discovery.bundle.id.Domain;
import io.appform.ranger.discovery.bundle.id.Id;
import io.appform.ranger.discovery.bundle.id.IdInfo;
import io.appform.ranger.discovery.bundle.id.constraints.IdValidationConstraint;
import io.appform.ranger.discovery.bundle.id.formatter.IdFormatter;
import io.appform.ranger.discovery.bundle.id.nonce.NonceGeneratorBase;
import io.appform.ranger.discovery.bundle.id.nonce.RandomNonceGenerator;
import io.appform.ranger.discovery.bundle.id.request.IdGenerationRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Base Id Generator
 */
@SuppressWarnings("unused")
@Slf4j
public class IdGeneratorBase {

    private final int MINIMUM_ID_LENGTH;
    protected final DateTimeFormatter DATE_TIME_FORMATTER;
    private final Pattern PATTERN;
    private static int NODE_ID;
    private final IdFormatter idFormatter;
    protected final NonceGeneratorBase nonceGenerator;

    public static void initialize(int node) {
        NODE_ID = node;
    }

    public IdGeneratorBase(final IdFormatter idFormatter,
                           final int minimumIdLength,
                           final DateTimeFormatter dateTimeFormatter,
                           final Pattern pattern) {
        this.idFormatter = idFormatter;
        this.nonceGenerator = new RandomNonceGenerator(NODE_ID, idFormatter);
        this.MINIMUM_ID_LENGTH = minimumIdLength;
        this.DATE_TIME_FORMATTER = dateTimeFormatter;
        this.PATTERN = pattern;
    }

    public synchronized void cleanUp() {
        nonceGenerator.cleanUp();
    }

    public void registerDomain(final Domain domain) {
        nonceGenerator.registerDomain(domain);
    }

    public synchronized void registerGlobalConstraints(final IdValidationConstraint... constraints) {
        registerGlobalConstraints(ImmutableList.copyOf(constraints));
    }

    public synchronized void registerGlobalConstraints(final List<IdValidationConstraint> constraints) {
        Preconditions.checkArgument(null != constraints && !constraints.isEmpty());
        nonceGenerator.registerGlobalConstraints(constraints);
    }

    public synchronized void registerDomainSpecificConstraints(
            final String domain,
            final IdValidationConstraint... validationConstraints) {
        registerDomainSpecificConstraints(domain, ImmutableList.copyOf(validationConstraints));
    }

    public synchronized void registerDomainSpecificConstraints(
            final String domain,
            final List<IdValidationConstraint> validationConstraints) {
        Preconditions.checkArgument(null != validationConstraints && !validationConstraints.isEmpty());
        nonceGenerator.registerDomainSpecificConstraints(domain, validationConstraints);
    }

    /**
     * Generate id with given namespace
     *
     * @param namespace String namespace for ID to be generated
     * @return Generated Id
     */
    public Id generate(final String namespace) {
        val idInfo = nonceGenerator.generate(namespace);
        return nonceGenerator.getIdFromIdInfo(idInfo, namespace, idFormatter);
    }

    public Id generate(final String namespace, final IdFormatter idFormatter) {
        val idInfo = nonceGenerator.generate(namespace);
        return nonceGenerator.getIdFromIdInfo(idInfo, namespace, idFormatter);
    }

    /**
     * Generate id that matches all passed constraints.
     * NOTE: There are performance implications for this.
     * The evaluation of constraints will take its toll on id generation rates.
     *
     * @param namespace     String namespace
     * @param domain     Domain for constraint selection
     * @param skipGlobal Skip global constrains and use only passed ones
     * @return ID if it could be generated
     */
    public Optional<Id> generateWithConstraints(final String namespace, final String domain, final boolean skipGlobal) {
        Optional<IdInfo> idInfoOptional = nonceGenerator.generateWithConstraints(namespace, domain, skipGlobal);
        return idInfoOptional.map(idInfo -> nonceGenerator.getIdFromIdInfo(idInfo, namespace, idFormatter));
    }

    public Optional<Id> generateWithConstraints(final String namespace,
                                                final List<IdValidationConstraint> inConstraints,
                                                final boolean skipGlobal) {
        Optional<IdInfo> idInfoOptional = nonceGenerator.generateWithConstraints(namespace, inConstraints, skipGlobal);
        return idInfoOptional.map(idInfo -> nonceGenerator.getIdFromIdInfo(idInfo, namespace, idFormatter));
    }

    public Optional<Id> generateWithConstraints(final IdGenerationRequest request) {
        val idInfo = nonceGenerator.generateWithConstraints(request);
        return idInfo.map(info -> (nonceGenerator.getIdFromIdInfo(info, request.getPrefix(), request.getIdFormatter())));
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
            if (!matcher.find()) {
                return Optional.empty();
            }
            return Optional.of(Id.builder()
                    .id(idString)
                    .node(Integer.parseInt(matcher.group(3)))
                    .exponent(Integer.parseInt(matcher.group(4)))
                    .generatedDate(DATE_TIME_FORMATTER.parseDateTime(matcher.group(2)).toDate())
                    .build());
        } catch (Exception e) {
            log.warn("Could not parse idString {}", e.getMessage());
            return Optional.empty();
        }
    }
}
