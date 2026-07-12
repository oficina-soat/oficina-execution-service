package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

class AwsDomainMessagingClientTest {
    @Test
    void deveUsarCadeiaDefaultSemCredenciaisEEndpoint() {
        var provider = AwsDomainMessagingClient.credentialsProvider("", "", "", "");

        assertInstanceOf(DefaultCredentialsProvider.class, provider);
    }

    @Test
    void deveUsarCredencialLocalQuandoHaEndpointAlternativo() {
        var credentials = AwsDomainMessagingClient.credentialsProvider("", "", "", "http://localhost:4566")
                .resolveCredentials();

        assertEquals("local", credentials.accessKeyId());
        assertEquals("local", credentials.secretAccessKey());
    }

    @Test
    void deveUsarCredencialBasicaQuandoOParFoiConfigurado() {
        var credentials = AwsDomainMessagingClient.credentialsProvider("access", "secret", "", "")
                .resolveCredentials();

        assertEquals("access", credentials.accessKeyId());
        assertEquals("secret", credentials.secretAccessKey());
    }

    @Test
    void deveUsarCredencialTemporariaQuandoOTokenFoiConfigurado() {
        var credentials = AwsDomainMessagingClient.credentialsProvider("access", "secret", "token", "")
                .resolveCredentials();
        var sessionCredentials = assertInstanceOf(AwsSessionCredentials.class, credentials);

        assertEquals("token", sessionCredentials.sessionToken());
    }

    @Test
    void deveRejeitarCredenciaisEstaticasIncompletas() {
        assertThrows(IllegalStateException.class,
                () -> AwsDomainMessagingClient.credentialsProvider("access", "", "", ""));
        assertThrows(IllegalStateException.class,
                () -> AwsDomainMessagingClient.credentialsProvider("", "", "token", ""));
    }
}
