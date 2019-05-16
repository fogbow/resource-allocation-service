package cloud.fogbow.ras.api.http.request;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class UserInterfaceDocumentation {
    @RequestMapping(value = "/ras/ui", method = RequestMethod.GET)
    public String home() {
        return "redirect:/swagger-ui.html";
    }
}