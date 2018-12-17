package org.fogbowcloud.ras.core.intercomponent.xmpp;

public enum IqElement {
    QUERY("query"),
    ORDER("order"),
    ORDER_ID("orderId"),
    FEDERATION_USER("federationUser"),
    INSTANCE("instance"),
    INSTANCE_TYPE("instanceType"),
    ORDER_CLASS_NAME("orderClassName"),
    INSTANCE_CLASS_NAME("instanceClassName"),
    USER_QUOTA("userQuota"),
    USER_QUOTA_CLASS_NAME("userQuotaClassName"),
    MEMBER_ID("memberId"),
    CLOUD_NAME("cloudName"),
    CLOUD_NAMES_LIST("cloudNamesList"),
    CLOUD_NAMES_LIST_CLASS_NAME("cloudNamesListClassName"),
    EVENT("event"),
    IMAGE_CLASS_NAME("imageClassName"),
    IMAGE_ID("imageId"),
    IMAGE("image"),
    IMAGES_MAP("imagesMap"),
    IMAGES_MAP_CLASS_NAME("imagesMapClassName"),
    RULE_ID("ruleId"),
    SECURITY_RULE_LIST("securityRuleList"),
    SECURITY_RULE_LIST_CLASS_NAME("imagesMapClassName"),
    SECURITY_RULE("securityRule"),
    GENERIC_REQUEST("genericRequest"),
    GENERIC_REQUEST_RESPONSE("genericRequestResponse"),
    GENERIC_REQUEST_RESPONSE_CLASS_NAME("genericRequestResponseClassName");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}
