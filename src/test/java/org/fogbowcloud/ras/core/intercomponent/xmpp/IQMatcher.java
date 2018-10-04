package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.mockito.ArgumentMatcher;
import org.xmpp.packet.IQ;

/**
 * As the {@link IQ} class does not implement {@link Object#equals(Object)} we need this helper IQMatcher class
 * to ease {@link org.mockito.Mockito#verify(Object)} calls.
 */
public class IQMatcher extends ArgumentMatcher<IQ> {

    public static final String NO_ID_REGEX = "id\\=\".*\"";

    private final IQ expectedIQ;

    public IQMatcher(IQ expectedIQ) {
        this.expectedIQ = expectedIQ;
    }

    @Override
    public boolean matches(Object argument) {
        IQ comparedIq = (IQ) argument;

        // jamppa generates random iq ids by default, our matcher ignores ids
        return removeIdAttribute(expectedIQ.toString()).equals(removeIdAttribute(comparedIq.toString()));
    }

    private String removeIdAttribute(String s) {
        return s.replaceFirst(NO_ID_REGEX, "");
    }

}
