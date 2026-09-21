# Águia Branca API

Backend do Challenge Águia Branca com Java 21, Spring Boot 4.1.1, Maven e MongoDB 8.
Esta etapa implementa autenticação JWT e prepara a autorização dos perfis `OPERATOR`, `MANAGER` e `LEADER`.

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
A autorização por método está habilitada para os próximos módulos usarem regras como `@PreAuthorize("hasRole('LEADER')")`.

Erros de validação, autenticação, autorização, recurso inexistente e conflito são retornados como `application/problem+json`.

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
