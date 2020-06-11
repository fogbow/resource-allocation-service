package cloud.fogbow.ras.requests.api.local.http.util;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TestController {

    @RequestMapping("/")
    public void get() throws Exception {
        Mock.throwException();
    }

    public static class Mock {

        public static void throwException() throws Exception {};

    }

}
