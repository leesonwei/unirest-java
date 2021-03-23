package kong.unirest;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @program: retrofit-spring-boot-starter
 * @author: LIZONG.WEI
 * @since:
 * @Create on 2021-03-23
 **/
public class GetWithBodyTest {
    private Logger logger = LoggerFactory.getLogger(GetWithBodyTest.class);
    @Test
    void getWithBody(){

        Unirest.config().defaultBaseUrl("http://localhost:8085/").instrumentWith(requestSummary -> {
            long startNanos = System.nanoTime();
            logger.info("request: {}", requestSummary.asString());
            return (responseSummary,exception) -> {
                if (responseSummary != null) {
                    logger.info("path: {} status: {} time: {}",
                            requestSummary.getRawPath(),
                            responseSummary.getStatus(),
                            TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos));
                }
                if (exception != null) {
                    logger.info("exception: {}, trace: {}", exception.getMessage(), exception.getStackTrace());
                }
            };
        });
        HttpResponse<String> stringHttpResponse = Unirest.getWithBody("test")
                .body("{\"partitions\":[{\"topic\":\"rest-proxy-test\",\"partition\":0}]}").contentType("application/json").asString();
        System.out.println(stringHttpResponse.getBody());
    }

    @Test
    void get(){
        HttpResponse<String> stringHttpResponse = Unirest.get("http://localhost:8085/base/test")
                .queryString("partition", "testget").asString();
        System.out.println(stringHttpResponse.getBody());
    }
}
