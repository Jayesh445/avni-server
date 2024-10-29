package org.avni.server.framework.tomcat;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TomcatContainerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatContainerCustomizer.class);

    @Value("${server.tomcat.accesslog.max-days}")
    int accessLogMaxDays;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        if (factory != null) {
            factory.addConnectorCustomizers(connector -> {
                connector.setScheme("https");
                connector.setProxyPort(443);
            });
            factory.addContextCustomizers(sameSiteCookiesConfig());
            LOGGER.info("Enabled secure scheme (https).");

            Collection<Valve> engineValves = factory.getEngineValves();
            for (Valve tempObject : engineValves) {
                if (tempObject instanceof AccessLogValve) {
                    AccessLogValve accessLogValve = (AccessLogValve) tempObject;
                    accessLogValve.setMaxDays(accessLogMaxDays);
                }
            }
        } else {
            LOGGER.warn("Could not change protocol scheme because Tomcat is not used as servlet container.");
        }
    }

    public TomcatContextCustomizer sameSiteCookiesConfig() {
        return context -> {
            final Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
            cookieProcessor.setSameSiteCookies(SameSiteCookies.LAX.getValue());
            context.setCookieProcessor(cookieProcessor);
        };
    }
}
