package app.contactgroups.domain;

import java.util.List;

/**
 * One condition as it arrives in the request body (contract {@code Condition}), not yet checked.
 */
public record ConditionInput(
    String type,
    List<String> tags,
    String tag,
    String key,
    String value,
    String country,
    String company) {}
