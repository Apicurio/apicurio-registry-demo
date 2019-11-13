package io.apicurio.registry.demo;

/**
 * @author Ales Justin
 */
public class Main {
    public static void main(String[] args) {
        Lifecycle application = new ApplicationImpl(args);
        Runtime.getRuntime().addShutdownHook(new Thread(application::stop, "Registry-Demo-Shutdown-Thread"));
        application.start();
    }
}
