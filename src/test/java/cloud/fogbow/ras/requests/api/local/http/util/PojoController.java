package cloud.fogbow.ras.requests.api.local.http.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PojoController {

    @Autowired
    private PojoService pojoService;

    @RequestMapping("/")
    public void get() throws Exception {
        this.pojoService.throwException();
    }

}
