package org.fogbowcloud.manager.core.intercomponent.xmpp;

import org.mockito.ArgumentMatcher;
import org.xmpp.packet.IQ;

/**
 * As the {@link IQ} class does not implement {@link Object#equals(Object)} we need this helper IQMatcher class
 * to ease {@link org.mockito.Mockito#verify(Object)} calls.
 */
public class IQMatcher extends ArgumentMatcher<IQ> {

    private final IQ expectedIQ;

    public IQMatcher(IQ expectedIQ) {
        this.expectedIQ = expectedIQ;
    }

    @Override
    public boolean matches(Object argument) {
        IQ comparedRequest = (IQ) argument;
        return expectedIQ.toString().equals(comparedRequest.toString());
    }
}
