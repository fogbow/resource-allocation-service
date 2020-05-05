package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import com.google.common.annotations.VisibleForTesting;

public abstract class AzureAsync {

    protected AsyncInstanceCreationManager asyncInstanceCreation = new AsyncInstanceCreationManager();

    @VisibleForTesting
    protected abstract void setAsyncInstanceCreation(AsyncInstanceCreationManager asyncInstanceCreation);

}
