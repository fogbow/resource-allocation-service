package cloud.fogbow.ras.api.http;

import cloud.fogbow.common.http.FogbowExceptionToHttpErrorConditionTranslator;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class RasExceptionToHttpErrorConditionTranslator extends FogbowExceptionToHttpErrorConditionTranslator {
}
