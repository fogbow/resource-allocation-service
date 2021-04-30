package cloud.fogbow.ras.core;

import java.util.Set;

import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.common.models.Permission;

public interface PermissionInstantiator<T extends FogbowOperation> {
    Permission<T> getPermissionInstance(String type, String ... params);
    Permission<T> getPermissionInstance(String type, String name, Set<String> operations);
}
