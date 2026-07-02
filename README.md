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

## CI/CD

Os workflows ficam em [.github/workflows/service-ci.yml](.github/workflows/service-ci.yml) e [.github/workflows/open-pr-to-main.yml](.github/workflows/open-pr-to-main.yml), derivados do [Template GitHub Actions para Microsserviços](../oficina-platform/templates/github-actions/README.md).

Pull requests e pushes na `main` executam o check `service-ci-validate` com `./mvnw -B verify -Pdynamodb -DskipITs=false -DfailIfNoTests=false`, validam a cobertura mínima de 80%, executam o Quality Gate SonarCloud e publicam o artifact `jacoco-report-oficina-execution-service`.

A publicação de imagem e o deploy Kubernetes são condicionais:

- `ENABLE_IMAGE_PUBLISH=true` habilita consulta ao ECR, build/push da imagem Docker e release com metadados da imagem;
- `ENABLE_K8S_DEPLOY=true` habilita atualização do Deployment no EKS;
- em `workflow_dispatch`, os inputs `publish_image` e `deploy` permitem acionar esses estágios manualmente.

Enquanto a estratégia de manifestos Kubernetes por microsserviço estiver aberta na plataforma, mantenha `ENABLE_K8S_DEPLOY=false` e use o job de validação como checagem obrigatória de branch.

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

## Swagger/OpenAPI

O contrato canônico do serviço é a [OpenAPI do oficina-execution-service](../oficina-platform/contracts/openapi/oficina-execution-service.yaml), mantida no repositório de plataforma.

Com o serviço em execução local na porta `8080`, a documentação gerada pelo Quarkus fica disponível em:

- Swagger UI: `http://localhost:8080/q/swagger-ui/`;
- OpenAPI YAML: `http://localhost:8080/q/openapi`;
- OpenAPI JSON: `http://localhost:8080/q/openapi?format=json`.

O teste [PlatformContractsTest](src/test/java/br/com/oficina/execution/contracts/PlatformContractsTest.java) valida que a OpenAPI gerada em runtime mantém os caminhos e métodos definidos no contrato canônico.

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

O backlog local está em [TODO.md](TODO.md). Os próximos incrementos esperados no Épico B2 são configurar a proteção da branch `main` e documentar a justificativa da Saga orquestrada pelo `oficina-os-service`, mantendo alinhamento com o [ROADMAP da plataforma](../oficina-platform/ROADMAP.md).
