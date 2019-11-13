package io.apicurio.registry.demo;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

/**
 * @author Ales Justin
 */
@ApplicationScoped
public class ApplicationConfiguration {

    @Produces
    public Lifecycle application() {
        return new ApplicationImpl(new String[]{});
    }

    public void init(@Observes StartupEvent event, Lifecycle lifecycle) {
        lifecycle.start();
    }

    public void destroy(@Observes ShutdownEvent event, Lifecycle lifecycle) {
        lifecycle.stop();
    }
}
