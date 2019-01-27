package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

public interface RemoteRequest<T> {

    T send() throws Exception;
}
