# oficina-execution-service

Microsserviço responsável por catálogo técnico, peças, serviços, estoque, diagnóstico, execução e reparo da plataforma de oficina.

Este repositório segue a governança definida em [../oficina-platform](../oficina-platform/). Para tarefas automatizadas, leia também [AGENTS.md](AGENTS.md) e [TODO.md](TODO.md).

## Responsabilidades

- manter catálogo técnico de peças e serviços;
- controlar saldo e movimentos de estoque;
- registrar diagnóstico, execução e reparo;
- manter histórico operacional;
- produzir e consumir eventos operacionais e de estoque usados pela Saga.

O serviço não é dono de Cliente, Veículo, estado global da Ordem de Serviço, orçamento ou pagamento.

## Stack

- Java 25
- Quarkus 3.37.0
- Amazon DynamoDB
- JWT, OpenAPI, Health, métricas Prometheus, logs JSON e OpenTelemetry

## Execução local

```bash
./mvnw test -Pdynamodb
./mvnw package -Pdynamodb
```

## Cobertura

O JaCoCo é executado no `verify`, gera relatório em `target/jacoco-report/` e falha o build quando a cobertura de instruções do bundle fica abaixo de 80%. O [Template GitHub Actions para Microsserviços](../oficina-platform/templates/github-actions/README.md) publica esse diretório como artifact `jacoco-report-oficina-execution-service`.

Evidência local de cobertura em 2026-07-01:

```text
./mvnw -B verify -Pdynamodb -DskipITs=false -DfailIfNoTests=false
instruction=90.85% branch=68.17% line=90.69% complexity=72.85%
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Validação de contratos

O teste [PlatformContractsTest](src/test/java/br/com/oficina/execution/contracts/PlatformContractsTest.java) valida o serviço contra os contratos canônicos em `../oficina-platform/contracts`: OpenAPI, schemas JSON de eventos, [Contrato de Erros REST](../oficina-platform/contracts/error-model.md), [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md) e [Contrato de Saga do oficina-os-service](../oficina-platform/contracts/saga/oficina-os-saga-v1.md).

## Docker

```bash
docker build --build-arg MAVEN_PROFILE=dynamodb -t oficina-execution-service:local .
docker run --rm -p 8080:8080 oficina-execution-service:local
```

## Endpoint técnico

- `GET /api/v1/status`: expõe identidade do serviço, ambiente e status técnico básico.

Health checks do Quarkus ficam em `/q/health`, `/q/health/live` e `/q/health/ready`.

## Contratos

- [Contrato de APIs REST](../oficina-platform/contracts/Contrato%20de%20APIs%20REST.md)
- [OpenAPI do oficina-execution-service](../oficina-platform/contracts/openapi/oficina-execution-service.yaml)
- [Contrato de Eventos de Domínio](../oficina-platform/contracts/Contrato%20de%20Eventos%20de%20Domínio.md)
- [Contrato de Tópicos de Mensageria](../oficina-platform/contracts/Contrato%20de%20Tópicos%20de%20Mensageria.md)
- [Contrato de Erros REST](../oficina-platform/contracts/error-model.md)
- [Contrato de Idempotência](../oficina-platform/contracts/idempotency.md)
- [Padrão DynamoDB do oficina-execution-service](../oficina-platform/docs/dynamodb-execution-service.md)

## Variáveis principais

- `AWS_REGION`
- `OFICINA_DYNAMODB_TABLE_PREFIX`
- `DYNAMODB_ENDPOINT_OVERRIDE`
- `OFICINA_AUTH_ISSUER`
- `OFICINA_AUTH_AUDIENCE`
- `MP_JWT_VERIFY_PUBLICKEY_LOCATION`
- `OTEL_EXPORTER_OTLP_ENDPOINT`
- `DEPLOYMENT_ENVIRONMENT`

## Estrutura

```text
src/main/java/br/com/oficina/execution/
  core/
  interfaces/
  framework/
```

## Próximo Trabalho

O backlog local está em [TODO.md](TODO.md). O próximo incremento esperado no Épico B2 é copiar e adaptar workflows de CI/CD, garantindo build, testes, Quality Gate, publicação de imagem e deploy automatizado, mantendo alinhamento com o [ROADMAP da plataforma](../oficina-platform/ROADMAP.md).
