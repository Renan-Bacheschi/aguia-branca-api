# Águia Branca API

Backend do Challenge Águia Branca com Java 21, Spring Boot 4.1.1, Maven e MongoDB 8.
Esta etapa prepara a infraestrutura local, a inicialização da aplicação e os endpoints de saúde e status.

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
./mvnw -version
```

## MongoDB local

Com o Docker em execução, valide a configuração e suba o banco:

```sh
docker compose config
docker compose up -d
```

Confira o container e aguarde o healthcheck indicar `healthy`:

```sh
docker compose ps
docker inspect --format '{{.State.Health.Status}}' aguia-branca-mongodb
```

Para consultar os logs:

```sh
docker compose logs mongodb
```

O serviço `mongodb` usa a imagem `mongo:8`, o container `aguia-branca-mongodb` e o volume persistente `mongodb_data` em `/data/db`.
A porta 27017 está publicada somente em `127.0.0.1`. O banco local não exige credenciais.

O Compose define `GLIBC_TUNABLES=glibc.pthread.rseq=1` para compatibilidade com o kernel do Docker Desktop usado na validação local (`7.0.12-linuxkit`).
Essa configuração evita o uso do cache por CPU do TCMalloc afetado pela incompatibilidade com kernels recentes e segue o [teste de compatibilidade do próprio MongoDB](https://github.com/mongodb/mongo/blob/r8.3.11/jstests/noPassthrough/rseq_linux_compatibility/rseq_kernel_compatibility_check.js).
O ajuste fica restrito ao container e pode ter impacto no desempenho do alocador; ele atende ao ambiente de desenvolvimento local desta etapa.

## Executar a aplicação

```sh
./mvnw spring-boot:run
```

A aplicação usa a porta 8080 e a conexão `mongodb://localhost:27017/aguia_branca` por padrão.
Para sobrescrever a conexão no macOS ou Linux:

```sh
MONGODB_URI=mongodb://localhost:27017/aguia_branca ./mvnw spring-boot:run
```

O arquivo `.env.example` documenta a variável disponível, sem segredos.
O Spring Boot não carrega `.env` automaticamente; exporte `MONGODB_URI` no terminal ou configure-a no ambiente de execução da IDE.

## Verificar os endpoints

Com a aplicação executando, use outro terminal:

```sh
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/info
curl -i http://localhost:8080/api/v1/status
```

Com o MongoDB disponível, o health check retorna HTTP 200 e o campo `status` com valor `UP`.
Ele verifica também a conexão com o banco; uma falha nessa conexão resulta em HTTP 503 e status `DOWN`.
O endpoint de informações retorna HTTP 200 com `{}` nesta etapa.

O endpoint de status retorna HTTP 200 com o nome configurado da aplicação, status `UP` e timestamp UTC no formato ISO 8601. Exemplo:

```json
{
  "application": "aguia-branca-api",
  "status": "UP",
  "timestamp": "2026-09-20T12:00:00Z"
}
```

O status confirma que a API responde; a saúde do MongoDB é consultada no Actuator.
Somente `/actuator/health`, `/actuator/info` e `/api/v1/status` têm acesso público.
Outras rotas exigem autenticação e uma requisição GET anônima recebe HTTP 401:

```sh
curl -i http://localhost:8080/api/v1/private
```

Somente `health` e `info` estão expostos no Actuator. A autenticação real será implementada em uma etapa posterior; login por formulário e HTTP Basic estão desabilitados, e a proteção CSRF permanece ativa.

## Compilar e testar

```sh
./mvnw clean verify
```

Esse comando compila, executa os testes e gera o JAR executável em `target/aguia-branca-api-0.0.1-SNAPSHOT.jar`.
Os testes automatizados podem rodar sem MongoDB. Apenas no contexto dos testes, o indicador de saúde do MongoDB fica desabilitado; a conexão real deve ser validada pelos comandos de execução local acima.

## Parar os serviços

Encerre a aplicação com `Ctrl+C` no terminal em que ela está executando.
Para parar e remover o container do MongoDB, preservando o volume e os dados:

```sh
docker compose down
```

## Problemas comuns

- Se o Maven indicar uma versão incompatível de Java, ajuste `JAVA_HOME` para o JDK 21 e confira `./mvnw -version`.
- Se o Docker informar que não consegue conectar ao daemon, inicie o Docker Desktop ou o serviço Docker e repita `docker info`.
- Se a conexão com o MongoDB for recusada, confira `docker compose ps`, o healthcheck e o valor de `MONGODB_URI`.
