package br.com.oficina.execution.framework.messaging;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MessagingRuntimeDependencies {
    private final AwsDomainMessagingClient messagingClient;

    MessagingRuntimeDependencies(AwsDomainMessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    public void validar() {
        messagingClient.validarDependencias();
    }
}
