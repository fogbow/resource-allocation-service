package cloud.fogbow.ras.core.intercomponent.xmpp;

public enum IqElement {
    QUERY("query"),
    ORDER("order"),
    ORDER_ID("orderId"),
    SYSTEM_USER("systemUser"),
    INSTANCE("instance"),
    INSTANCE_TYPE("instanceType"),
    ORDER_CLASS_NAME("orderClassName"),
    INSTANCE_CLASS_NAME("instanceClassName"),
    USER_QUOTA("userQuota"),
    USER_QUOTA_CLASS_NAME("userQuotaClassName"),
    PROVIDER_ID("providerId"),
    CLOUD_NAME("cloudName"),
    CLOUD_NAMES_LIST("cloudNamesList"),
    CLOUD_NAMES_LIST_CLASS_NAME("cloudNamesListClassName"),
    NEW_STATE("newState"),
    IMAGE_CLASS_NAME("imageClassName"),
    IMAGE_ID("imageId"),
    IMAGE("image"),
    IMAGE_SUMMARY_LIST("imageSummaryList"),
    IMAGE_SUMMARY_LIST_CLASS_NAME("imageSummaryListClassName"),
    RULE_ID("ruleId"),
    SECURITY_RULE_LIST("securityRuleList"),
    SECURITY_RULE_LIST_CLASS_NAME("imagesMapClassName"),
    SECURITY_RULE("securityRule");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}
