package io.pocat.platform.env;

import org.junit.Test;

import java.util.Map;

public class EnvironmentProviderTest {
    @Test
    public void testBuild() {
        Map<String, String> initEnv = Map.of("io.pocat.home", "D:\\work\\pocat-can\\test\\dir");
        FileEnvProviderFactory factory = new FileEnvProviderFactory();
        EnvironmentProviderImpl provider = (EnvironmentProviderImpl) factory.createProvider(initEnv);
    }

    @Test
    public void testInitProvider() {
        Map<String, String> initEnv = Map.of("io.pocat.home", "D:\\work\\pocat-can\\test\\dir");
        InitialEnvProvider envProvider = new InitialEnvProvider(initEnv);

        System.out.println(envProvider.getProperty("test"));
    }

    @Test
    public void testRefCountProvider() {
        Map<String, String> initEnv = Map.of("io.pocat.home", "D:\\work\\pocat-can\\test\\dir");
        InitialEnvProvider envProvider = new InitialEnvProvider(initEnv);
        InitialEnvProvider envProvider2 = new InitialEnvProvider(initEnv);
        InitialEnvProvider envProvider3 = new InitialEnvProvider(initEnv);

        System.out.println(envProvider.getProperty("test"));

        envProvider2.close();
        envProvider3.close();
        envProvider.close();
        System.out.println(envProvider.getProperty("test"));
    }
}
