package com.asusoftware.SafeDocs_api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Security security = new Security();
    private Google google = new Google();
    private Mail mail = new Mail();
    private Storage storage = new Storage();
    private Reminders reminders = new Reminders();

    @Data public static class Security {
        private Jwt jwt = new Jwt();
        private Cors cors = new Cors();
        @Data public static class Jwt {
            private String issuer;
            private String secret;
            private String algorithm;
            private int accessTokenTtlMinutes;
            private int refreshTokenTtlDays;
        }
        @Data public static class Cors {
            private List<String> allowedOrigins;
            private String allowedMethods;
            private String allowedHeaders;
            private boolean allowCredentials;
        }
    }
    @Data public static class Google { private String clientId; }
    @Data public static class Mail { private String from; private boolean enabled; }
    @Data public static class Storage { private String location; private String publicBaseUrl; }
    @Data public static class Reminders { private int dailyHour; private String timezone; private List<Integer> offsets; }
}
