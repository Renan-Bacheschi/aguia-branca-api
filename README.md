# Águia Branca API

Backend do Challenge Águia Branca com Java 21, Spring Boot 4.1.1, Maven e MongoDB 8.
O backend implementa autenticação JWT e os fluxos de estratégias, ideias, projetos, progresso e resultados para os perfis `OPERATOR`, `MANAGER` e `LEADER`.

## Requisitos

- JDK 21.
- Docker com Docker Compose e daemon em execução.
- Portas 8080 e 27017 disponíveis.

Execute os comandos a partir da raiz do projeto. O Maven Wrapper está incluído; não é necessário instalar o Maven separadamente.
No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

Confira o Java utilizado pelo Maven:

```sh
java -version
./mvnw -version
```

No macOS, selecione o JDK 21 no terminal se outra versão estiver ativa:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Finalidade |
| --- | --- | --- | --- |
| `JWT_SECRET` | Sim | nenhum | Chave Base64 de pelo menos 32 bytes usada para assinar os tokens HS256. |
| `JWT_EXPIRATION_MINUTES` | Não | `120` | Tempo de validade do token em minutos. |
| `APP_SEED_ENABLED` | Não | `false` | Cria as contas de demonstração quando definido como `true`. |
| `MONGODB_URI` | Não | `mongodb://localhost:27017/aguia_branca` | Conexão com o MongoDB. |

Gere uma chave local e configure o ambiente antes de iniciar a aplicação:

```sh
export JWT_SECRET=$(openssl rand -base64 32)
export JWT_EXPIRATION_MINUTES=120
export APP_SEED_ENABLED=true
```

O arquivo `.env.example` lista as variáveis sem incluir uma chave. O Spring Boot não carrega `.env` automaticamente; exporte as variáveis no terminal ou configure-as na IDE.
A aplicação rejeita uma chave ausente, Base64 inválida ou com menos de 32 bytes.

## MongoDB local

Valide a configuração e suba o banco:

```sh
docker compose config
docker compose up -d
docker compose ps
```

O serviço usa a imagem `mongo:8`, o container `aguia-branca-mongodb` e o volume persistente `mongodb_data`.
A porta 27017 fica publicada somente em `127.0.0.1` e o banco local não exige credenciais.

O Compose define `GLIBC_TUNABLES=glibc.pthread.rseq=1` para compatibilidade com o kernel do Docker Desktop usado na validação local.
O ajuste fica restrito ao container de desenvolvimento.

## Contas de demonstração

As contas são criadas somente quando `APP_SEED_ENABLED=true`. O seed pode ser executado novamente sem duplicar usuários e persiste apenas hashes BCrypt.

| Nome | E-mail | Senha | Perfil |
| --- | --- | --- | --- |
| Demo Operator | `operator@demo.com` | `Operator@123` | `OPERATOR` |
| Demo Manager | `manager@demo.com` | `Manager@123` | `MANAGER` |
| Demo Leader | `leader@demo.com` | `Leader@123` | `LEADER` |

Essas credenciais são destinadas somente ao ambiente local de demonstração.

## Executar a aplicação

Com as variáveis configuradas e o MongoDB disponível:

```sh
./mvnw spring-boot:run
```

O e-mail é normalizado para letras minúsculas. O MongoDB cria um índice único para impedir duplicações.

## Autenticação

Faça login:

```sh
curl -i \
  -H 'Content-Type: application/json' \
  -d '{"email":"operator@demo.com","password":"Operator@123"}' \
  http://localhost:8080/api/v1/auth/login
```

A resposta contém `accessToken`, `tokenType`, `expiresInSeconds` e os dados públicos do usuário:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 7200,
  "user": {
    "id": "<id>",
    "name": "Demo Operator",
    "email": "operator@demo.com",
    "role": "OPERATOR"
  }
}
```

Copie o token e consulte o usuário autenticado:

```sh
export ACCESS_TOKEN='<jwt>'

curl -i \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/v1/auth/me
```

`GET /api/v1/auth/me` retorna `id`, `name`, `email` e `role`. A resposta nunca inclui o hash da senha.
Tokens ausentes, inválidos, expirados ou vinculados a um usuário removido, inativo ou com perfil alterado recebem HTTP 401.

## Rotas e segurança

São públicas apenas:

- `POST /api/v1/auth/login`
- `GET /api/v1/status`
- `GET /actuator/health`
- `GET /actuator/info`

Todas as outras rotas exigem `Authorization: Bearer <jwt>`. A aplicação não usa sessão, login por formulário ou HTTP Basic.
A autorização por método aplica as permissões de perfil em cada operação. A propriedade das ideias também é validada no serviço, sem aceitar autoria ou perfil enviados pelo cliente.

Erros de validação, autenticação, autorização, recurso inexistente e conflito são retornados como `application/problem+json`.

## Estratégias

Todos os perfis autenticados consultam estratégias. Somente `LEADER` cria, altera e arquiva.

| Método | Rota | Perfis |
| --- | --- | --- |
| `POST` | `/api/v1/strategies` | `LEADER` |
| `GET` | `/api/v1/strategies` | Todos |
| `GET` | `/api/v1/strategies/{id}` | Todos |
| `PUT` | `/api/v1/strategies/{id}` | `LEADER` |
| `DELETE` | `/api/v1/strategies/{id}` | `LEADER` |
| `GET` | `/api/v1/strategies/{id}/history` | Todos |

As respostas individuais incluem `ETag` com a revisão. Envie esse valor no cabeçalho `If-Match` ao alterar ou arquivar:

```sh
curl -i -X PUT \
  -H "Authorization: Bearer $LEADER_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'If-Match: "1"' \
  -d '{"title":"Eficiência operacional","description":"Reduzir desperdícios","category":"Operações","campaign":"Ciclo 2026","startsOn":"2026-01-01","endsOn":"2026-12-31"}' \
  http://localhost:8080/api/v1/strategies/<id>
```

`GET /api/v1/strategies` aceita `page`, `size` e `active`. Sem `active`, retorna estratégias não arquivadas. `active=true` retorna as vigentes na data atual; `active=false` retorna as demais, inclusive as arquivadas. Cada criação, alteração ou arquivamento acrescenta uma revisão imutável ao histórico.

## Ideias

`OPERATOR` cria e acompanha apenas as próprias ideias. Um rascunho pode ser alterado, removido e enviado. `MANAGER` consulta ideias enviadas ou avaliadas e registra aprovação ou rejeição.

| Método | Rota | Perfis |
| --- | --- | --- |
| `POST` | `/api/v1/ideas` | `OPERATOR` |
| `GET` | `/api/v1/ideas` | `OPERATOR`, `MANAGER` |
| `GET` | `/api/v1/ideas/{id}` | `OPERATOR` proprietário, `MANAGER` após envio |
| `PUT` | `/api/v1/ideas/{id}` | `OPERATOR` proprietário enquanto `DRAFT` |
| `DELETE` | `/api/v1/ideas/{id}` | `OPERATOR` proprietário enquanto `DRAFT` |
| `POST` | `/api/v1/ideas/{id}/submit` | `OPERATOR` proprietário |
| `PATCH` | `/api/v1/ideas/{id}/review` | `MANAGER` |

Exemplo de avaliação:

```sh
curl -i -X PATCH \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","priority":"HIGH","justification":"Alinhada ao ciclo estratégico."}' \
  http://localhost:8080/api/v1/ideas/<id>/review
```

A listagem aceita `page`, `size`, `status`, `priority` e `strategyId`. Os filtros nunca ampliam o escopo permitido para o usuário.

## Projetos e resultados

Somente `MANAGER` cria, altera, atualiza o progresso e arquiva projetos. `MANAGER` e `LEADER` consultam projetos e resultados. Um projeto nasce de uma ideia `APPROVED`; o vínculo com a estratégia é herdado da ideia e cada ideia pode gerar no máximo um projeto.

| Método | Rota | Perfis |
| --- | --- | --- |
| `POST` | `/api/v1/projects` | `MANAGER` |
| `GET` | `/api/v1/projects` | `MANAGER`, `LEADER` |
| `GET` | `/api/v1/projects/{id}` | `MANAGER`, `LEADER` |
| `PUT` | `/api/v1/projects/{id}` | `MANAGER` |
| `DELETE` | `/api/v1/projects/{id}` | `MANAGER` |
| `PATCH` | `/api/v1/projects/{id}/progress` | `MANAGER` |
| `POST` | `/api/v1/projects/{projectId}/results` | `MANAGER` |
| `GET` | `/api/v1/projects/{projectId}/results` | `MANAGER`, `LEADER` |
| `GET` | `/api/v1/projects/{projectId}/results/{resultId}` | `MANAGER`, `LEADER` |
| `PUT` | `/api/v1/projects/{projectId}/results/{resultId}` | `MANAGER` |
| `DELETE` | `/api/v1/projects/{projectId}/results/{resultId}` | `MANAGER` |

A listagem de projetos aceita `page`, `size`, `strategyId`, `ideaId`, `status` e `stage`. Projetos arquivados continuam acessíveis por identificador para preservar o histórico, mas deixam de aceitar alterações.

Para concluir um projeto, envie `stage=CLOSED`, `status=COMPLETED`, `progressPercentage=100` e `actualEndDate`. Projetos cancelados ou arquivados não aceitam novos resultados. Valores monetários são persistidos como `Decimal128` no MongoDB.

Resultados `COST_SAVING` e `ADDITIONAL_REVENUE` exigem `financialAmount`. Os demais tipos exigem `unit`, `baselineValue` e `achievedValue`. A natureza pode ser `FORECAST` ou `ACTUAL`.

## Dashboard e relatórios

Os relatórios são calculados pelo backend a partir dos documentos atuais do MongoDB e não são persistidos. Todos os valores financeiros são interpretados como BRL. Somente usuários `LEADER` podem acessar:

- `GET /api/v1/reports/summary`
- `GET /api/v1/reports/strategies/{strategyId}`
- `GET /api/v1/reports/projects/{projectId}`

`FORECAST` representa uma projeção e usa o investimento planejado. `ACTUAL` representa um valor realizado e usa o investimento realizado. Somente `COST_SAVING` e `ADDITIONAL_REVENUE` compõem os benefícios financeiros.

Os indicadores financeiros seguem estas fórmulas:

```text
forecastNetBenefit = forecastFinancialBenefit - plannedInvestment
actualNetBenefit   = actualFinancialBenefit - actualInvestment

forecastRoi = forecastNetBenefit / plannedInvestment * 100
actualRoi   = actualNetBenefit / actualInvestment * 100
```

Quando o investimento correspondente é zero, o ROI retorna `null` e o campo de disponibilidade retorna `false`. Percentuais disponíveis são arredondados para duas casas com `HALF_UP`. Benefício líquido e ROI negativos indicam que o benefício ficou abaixo do investimento.

O ROI agregado é calculado sobre os benefícios e investimentos totais. Ele nunca é uma média simples dos ROIs dos projetos.

Para métricas operacionais:

```text
PRODUCTIVITY_GAIN = (achievedValue - baselineValue) / baselineValue * 100
TIME_REDUCTION    = (baselineValue - achievedValue) / baselineValue * 100
```

Se `baselineValue` não for maior que zero, `percentage` retorna `null` e `percentageAvailable` retorna `false`. Métricas `OTHER` permanecem disponíveis como itens individuais, sem cálculo automático de percentual. Unidades diferentes nunca são somadas.

Projetos arquivados participam dos totais e indicadores históricos, mas não de `activeProjectCount`. Um projeto ativo é não arquivado e possui status `PLANNED` ou `IN_PROGRESS`. Projetos concluídos ou cancelados permanecem nos totais e agrupamentos por status. Ideias ainda não avaliadas aparecem no agrupamento de prioridade como `UNASSIGNED`.

As consultas são realizadas em lotes por estratégia e projeto. O cálculo em memória atende ao volume atual e evita uma consulta individual para cada item.

Exemplo de relatório geral:

```sh
curl -H "Authorization: Bearer $LEADER_TOKEN" \
  http://localhost:8080/api/v1/reports/summary
```

Exemplo de relatório de estratégia:

```sh
curl -H "Authorization: Bearer $LEADER_TOKEN" \
  http://localhost:8080/api/v1/reports/strategies/<strategyId>
```

Exemplo de relatório de projeto:

```sh
curl -H "Authorization: Bearer $LEADER_TOKEN" \
  http://localhost:8080/api/v1/reports/projects/<projectId>
```

Um projeto com investimento realizado de `10000.00` e benefício financeiro realizado de `15000.00` retorna:

```json
{
  "actualInvestment": 10000.00,
  "actualFinancialBenefit": 15000.00,
  "actualNetBenefit": 5000.00,
  "actualRoi": 50.00,
  "actualRoiAvailable": true
}
```

## Verificar os endpoints públicos

```sh
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/info
curl -i http://localhost:8080/api/v1/status
```

Com o MongoDB disponível, o health check retorna HTTP 200 e `UP`. Falhas na conexão resultam em HTTP 503 e `DOWN`.

## Compilar e testar

```sh
./mvnw test
./mvnw clean verify
```

Os testes automatizados não exigem MongoDB. A criação automática de índices e o indicador MongoDB são desabilitados somente nos testes de contexto que simulam o repositório.

## Parar os serviços

Encerre a aplicação com `Ctrl+C` no terminal em que ela estiver executando.
Para remover o container preservando o volume:

```sh
docker compose down
```

## Problemas comuns

- Se o Maven indicar uma versão incompatível, ajuste `JAVA_HOME` para o JDK 21.
- Se o Docker não alcançar o daemon, inicie o Docker Desktop ou o serviço Docker.
- Se a conexão com o MongoDB falhar, confira `docker compose ps` e `MONGODB_URI`.
- Se a aplicação rejeitar `JWT_SECRET`, gere novamente a chave com `openssl rand -base64 32`.
