# Aplicativo de Upload de Arquivos para Buckets no Oracle Cloud Infrastructure

[![Doar](https://www.paypalobjects.com/pt_BR/BR/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/donate/?hosted_button_id=LZ67TDQWYGKTG)

<!-- TOC -->
* [README - English](/README.md)
* [Sobre](#sobre)
  * [O que você irá precisar](#o-que-voc-ir-precisar)
    * [Service](#service)
  * [Requisitos de sistema](#requisitos-de-sistema)
  * [Características](#caractersticas)
    * [Sob demanda e compatível com serviço](#sob-demanda-e-compatvel-com-servio)
    * [Vários diretórios](#vrios-diretrios)
    * [Múltiplos buckets](#mltiplos-buckets)
    * [Agendamento de Job](#agendamento-de-job)
    * [Tentativas de falha](#tentativas-de-falha)
	* [Compactação para ZIP e encriptação](compactação-para-zip-e-encriptação)
	* [Geração de Requisições Pré-autenticadas](#geração-de-requisições-pré-autenticadas)
    * [Notificação Webhook](#notificação-webhook)
	* [Envio de e-mail com Sendgrid](#envio-de-e-mail-com-sendgrid)
  * [Configurações](#configuraes)
    * [application.properties](#applicationproperties)
  * [Sobre a chave da API OCI (.oci)](#sobre-a-chave-da-api-oci-oci)
  * [Exemplo de arquivo .oci:](#exemplo-de-arquivo-oci)
  * [Exemplo de arquivo application.properties](#exemplo-de-arquivo-applicationproperties)
  * [Construindo](#construindo)
    * [Executando com perfil DEV](#executando-com-perfil-dev)
  * [Executando](#executando)
    * [Sob demanda](#sob-demanda)
  * [Código de Conduta](#cdigo-de-conduta)
  * [Contribuindo](#contribuindo)
  * [Licença](#licena)
<!-- TOC -->

# Sobre

O projeto upload-bucket-service é um aplicativo de upload de arquivo¹ automático para Buckets no serviço Oracle Cloud Infrastructure (OCI) Object Storage.

Com poucas configurações é possível mapear vários diretórios² e fazer upload para o Oracle Object Storage, permitindo configurar sobrescrita de arquivos, agendamento e notificação de webhook.

Você poderá:

- Carregar arquivos de vários diretórios;
- Auxiliar na migração de dados extensos;
- Salve e replique arquivos de backup de forma segura e automatizada.
- Configure webhooks para receber notificação de cada execução com status e detalhes do trabalho.

Veja todos os exemplos em [Wiki.](https://github.com/alexmaycon/upload-bucket-service/wiki)

**Atenção:** este aplicativo não sincroniza arquivos excluídos, ou seja, não exclui o arquivo do Bucket, pois a intenção é fornecer uma forma segura de sincronizar arquivos, principalmente para backups.
Para atualizações de arquivos, ele só atualizará os arquivos modificados se o parâmetro `service.folders[*].overwriteExistingFile` for `true`.

**Observação 1:** *o limite de tamanho do arquivo é de 50 GB.*

**Observação 2:** *quando usado para fins de transferência de backups, você deve remover a permissão OBJECT_READ na política de permissões OCI e desabilitar a substituição de arquivos para o diretório. Recomenda-se desabilitar a permissão OBJECT_DELETE, também na política de permissões OCI para o usuário em que é utilizado para se comunicar com a API do serviço, garantindo assim maior segurança e integridade dos arquivos de backup. [Mais informações, consulte Protegendo o armazenamento de objetos](https://docs.oracle.com/en-us/iaas/Content/Security/Reference/objectstorage_security.htm)*

## O que você irá precisar

- Arquivo de chave da API do cliente OCI e arquivo de chave - veja mais detalhes em [Sobre a chave da API OCI (.oci)](#sobre-a-chave-da-api-oci-oci);
- Configurar diretórios no arquivo application.properties;
- **Só isso!**

Veja mais detalhes em [Configurações](#configura-es) abaixo.

### Service

Veja [Sob demanda e compatível com serviço](#sob-demanda-e-compat-vel-com-servi-o) abaixo.

## Requisitos de sistema

- Java: Java 1.8, Java 11 ou posterior;
- Sistema Operacional: Windows, Unix/Linux, MacOS (não testado);
- RAM: ~5MB (pode variar quando o upload do arquivo está em execução);
- Espaço em disco: ~35MB.

## Características

### Sob demanda e compatível com serviço

O aplicativo pode ser executado sob demanda ou configurado como um serviço no Windows e no Linux.

- [Install as Unix/Linux Service;](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.installing.nix-services)
- [Install as Microsoft Windows Service.](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.installing.windows-services)

### Vários diretórios

O aplicativo permite a configuração de um ou mais diretórios, nos quais é possível habilitá-lo ou desabilitá-lo e habilitar a sobrescrita de arquivos.
A análise de diretórios é realizada em paralelo por vários threads e de forma assíncrona, garantindo assim melhor desempenho e sem gastar recursos por muito tempo.

### Múltiplos buckets

Você pode optar por enviar para o bucket global ou para um bucket diferente por diretório.

- `service.oci.bucket` para configuração global;
- `service.folders[*].oci.bucket` para pasta específica.

Se o bucket não existir, use a opção 'createBucketIfNotExists' para criar um novo bucket.

- `service.oci.createBucketIfNotExists` para configuração global;
- `service.folders[*].oci.createBucketIfNotExists=true` para uma pasta específica.

### Agendamento de Job

A aplicação utiliza a notação de expressão Cron, na qual é possível especificar um intervalo ou tempo para a execução do trabalho. Você pode definir um valor padrão para todos os trabalhos (`service.cron`) ou definir valores específicos para cada pasta (`service.folders[*].cron`).

Exemplos:

| Expressão      | Significado                     |
|----------------|---------------------------------|
| 0 0 12 * * ?   | Executar às 12:00 todos os dias |
| 0 15 10 ? * *  | Executar às 10:15 todos os dias |
| 0/10 * * * * ? | Executar a cada 10 segundos     |
| 0 0/30 * * * ? | Executar a cada 30 minutos      |

### Tentativas de falha

Permite definir tentativas de execução quando ocorre uma falha durante o processamento do trabalho.

### Compactação para ZIP e encriptação

Permite que todo arquivo seja compactado para um arquivo ZIP e encriptado para maior segurança.

Para habilitar, basta adicionar no seu arquivo `application.properties`:

```
service.zip.enabled= true
```

Caso deseja adicionar a encriptação do ZIP atrávez de senha:

```
service.zip.password=Yourpa55w0rd
```

Ou ainda pode definir a váriavel de ambiente `UBS_ZIP_PWD` - ela sempre terá prioridade quando ambos locais tiverem a senha definida.


```bash
export UBS_ZIP_PWD=Yourpa55w0rd
```

### Geração de Requisições Pré-autenticadas

As solicitações pré-autenticadas fornecem uma forma de permitir que os usuários acessem um bucket ou objeto sem ter suas próprias credenciais através de uma URL gerada no momento do envio do arquivo.

As solicitações são criadas com permissão de leitura para o arquivo enviado e não permite escrita ou listagem dos demais arquivos do bucket.

**A data de expiração da solicitação criada é de 6 meses.**

Para habilitar, basta adicionar no seu arquivo `application.properties`:

```
service.oci.generatePreauthenticatedUrl=true
```

Quando habilitado, esse URL é enviado na notificação webhook e no e-mail.

### Notificação Webhook

Você pode configurar um URL de API que será **notificado** quando o trabalho terminar de ser executado (por falha ou sucesso). **Sua API deve aceitar o método POST.**

O corpo pode ser JSON ou XML, basta configurar o `service.hookContentType` com um dos valores:

- `application/json`
- `application/xml`

**Exemplo de JSON de sucesso:**

No campo `details`, cada diretório é separado pelo caractere '¢' e as propriedades do diretório são separadas pelo ';' personagem.

```json
{
	"jobName": "DEFAULT_CRON_JOB",
	"jobStatus": "COMPLETED",
	"details": "DIRECTORY=C:/temp;CRON=0/10 * * * * ?;BUCKET=teste¢DIRECTORY=C:/temp2;CRON=0/10 * * * * ?;BUCKET=teste",
	"createdTime": "2022-10-12T22:26:05+0000",
	"endTime": "2022-10-12T22:26:08+0000",
	"files": [{
		"fileName": "xyz.zip",
		"url": "https://..."
	}],
	"exceptions": []
}
```

**Exemplo de JSON de erro:**

```json
{
  "jobName": "DEFAULT_CRON_JOB",
  "jobStatus": "FAILED",
  "details": "DIRECTORY=C:/temp;CRON=0/10 * * * * ?;BUCKET=teste",
  "createdTime": "2022-10-12T22:57:54+0000",
  "endTime": "2022-10-12T22:57:57+0000",
  "exceptions": [
    {
      "cause": null,
      "stackTrace": [
        {
          "classLoaderName": null,
          "moduleName": null,
          "moduleVersion": null,
          "methodName": "lambda$parseJobExecution$0",
          "fileName": "Hook.java",
          "lineNumber": 68,
          "className": "dev.alexmaycon.bucketservice.hook.model.Hook",
          "nativeMethod": false
        }
      ],
      "message": "It's a test!!!!",
      "suppressed": [],
      "localizedMessage": "It's a test!!!!"
    }
  ]
}
```

### Envio de e-mail com Sendgrid

Caso possua uma conta da Twilio Sendgrid para envio de e-mail, você pode configura-la usando a chave de API de sua conta para habilitar o recebimento de notificações por e-mail.

Caso a criação de requisição pré-autenticada esteja habilitada, será enviado o link do arquivo no e-mail.

Para habilitar o envio do e-mail, basta adicionar no seu arquivo `application.properties`:

```
service.email.sendgrid.apiKey=paste-your-api-key-here
service.email.sender=from@email.com
service.email.recipients[0]=to@email.com
```

Você pode configurar quandos destinatários quiser:

```
service.email.sendgrid.apiKey=
service.email.sender=from@email.com
service.email.recipients[0]=to1@email.com
service.email.recipients[1]=to2@email.com
service.email.recipients[2]=to3@email.com
```

## Configurações

Estrutura da pasta de instalação:

```
root
│   upload-bucket-service.jar
│   .oci
|   application.properties
```

### application.properties

**Atenção:** Você deve ter pelo menos um diretório usando o `service.cron` global.

| Propriedade                              | Descrição                                                                                                   | Obrigatório | Valor padrão              | Tipo    |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------|-------------|---------------------------|---------|
| service.nameDefaultJob                   | Nome para o job padrão                                                                                      | Não         | "DEFAULT_CRON_JOB"        | String  |
| service.cron                             | Expressão cron padrão para execução de todos os jobs                                                        | Não         | "0/10 * * * * ?"          | String  |
| service.hook                             | URL do endpoint da API para ser notificado (POST) no final da execução do JOB                               | Não         |                           | String  |
| service.hookContentType                  | Media type (json/xml)                                                                                       | Não         | "application/json"        | String  |
| service.attemptsFailure                  | Número de tentativas quando ocorre uma falha                                                                | Não         | 1                         | int     |
| service.oci.profile                      | Sessão de perfil de configuração .oci                                                                       | Não         | "DEFAULT"                 | String  |
| service.oci.bucket                       | Nome do bucket OCI                                                                                          | **Sim**     |                           | String  |
| service.oci.generatePreauthenticatedUrl  | Ativar/desativar criação das URLs pré-autenticadas para o objeto na OCI                                     | Não         | false                          | Boolean  |
| service.oci.compartmentOcid              | Compartment OCID - se você quiser criar o bucket em um compartimento específico                             | Não         |                           | String  |
| service.folders[*]                       | Configuração de pastas                                                                                      | **Sim**     |                           | List    |
| service.folders[*].directory             | Caminho da pasta (precisa incluir caractere de escape para \ no Windows)                                    | **Sim**     |                           | String  |
| service.folders[*].cron                  | A expressão Cron especifica para a pasta                                                                    | Não         | Value from *service.cron* | String  |
| service.folders[*].overwriteExistingFile | Ativar/desativar a sobrescrita de arquivos                                                                  | Não         | false                     | boolean |
| service.folders[*].enabled               | Ativar/desativar o processamento de pastas                                                                  | Não         | true                      | boolean |
| service.folders[*].mapToBucketDir        | Defina o diretório a ser usado no bucket. Deixe vazio para usar root.                                       | Não         |                           | String  |
| service.folders[*].oci.profile           | Sessão de perfil de configuração .oci (aplica-se apenas à pasta)                                            | Não         | "DEFAULT"                 | String  |
| service.folders[*].oci.bucket            | Nome do bucket OCI (aplica-se apenas à pasta)                                                               | Não         |                           | String  |
| service.folders[*].oci.compartmentOcid   | Compartment OCID - se você quiser criar o bucket em um compartimento específico. (aplica-se apenas à pasta) | Não         |                           | String  |
| service.email.sendgrid.apiKey            | Chave API de sua conta no Twilio Sendgrid                                                                   | Não         |                           | String  |
| service.email.sendgrid.sender            | E-mail do remetente                                                                                         | Não         |                           | String  |
| service.email.sendgrid.recipients[*]     | E-mail(s) do destinatário                                                                                   | Não         |                           | String  |
| service.zip.enabled                      | Ativar/desativar a compactação do arquivo para ZIP                                                          | Não         | false                          | Boolean  |
| service.zip.password                     | Senha para encriptação do arquivo ZIP. Você também pode definir usando a variávle de ambiente `UBS_ZIP_PWD` | Não         |                           | String  |


##  Sobre a chave da API OCI (.oci)

- [To create a Customer Secret key;](https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcredentials.htm#create-secret-key)
- [To get the config file snippet for an API signing key.](https://docs.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcredentials.htm#)

Após obter o arquivo, renomeie o arquivo para **.oci** e cole-o na mesma pasta do aplicativo.

## Exemplo de arquivo .oci:

```properties
[DEFAULT]
user=
fingerprint=
tenancy=
region=
key_file=
```

## Exemplo de arquivo application.properties

```properties
service.cron=0 0/30 * * * ?
service.oci.profile=DEFAULT
service.oci.bucket=bkp_bd
service.folders[0].directory=C:/temp
service.folders[0].cron=0 0/10 * * * ?
service.folders[1].directory=C:/temp2
service.folders[1].overwriteExistingFile=false
service.folders[1].enabled=true
service.folders[2].directory=C:/temp3
service.folders[2].overwriteExistingFile=false
service.folders[2].enabled=true
service.folders[3].directory=C:/temp4
service.folders[3].overwriteExistingFile=false
service.folders[3].enabled=true
service.folders[4].directory=C:/temp4
service.folders[4].overwriteExistingFile=false
service.folders[4].enabled=true
service.attemptsFailure=5
```

## Construindo

Este projeto foi escrito em Java com Spring Batch e Oracle Cloud Infrastructure Client SDK.

Para construir usamos o Maven.

### Executando com perfil DEV

O perfil DEV deve ser usado para depuração e teste. Você precisará alterar o valor da propriedade `--oci`.

Vá para o diretório raiz do projeto e execute:

```bash
mvn clean package install
mvn spring-boot:run -D"spring-boot.run.profiles=dev" -D"spring-boot.run.arguments"="--oci=/path/to/.oci"
```

O projeto possui arquivos de configuração para o IntelliJ IDEA, permitindo que essas configurações sejam realizadas no IDE:

- Maven
    - [clean, install]
    - [clean, package]
- Spring Boot
    - upload-bucket-service (DEV)

## Executando

### Sob-demanda

```bash
java -jar upload-bucket-service.jar
```

## Código de Conduta

Incentivamos a participação da comunidade, mas fique atento às regras do nosso Código de Conduta.

Veja [CODE OF CONDUCT](/CODE_OF_CONDUCT.md) para detalhes.

## Contribuindo

upload-bucket-service é um projeto de código aberto, fique à vontade para participar conosco.

Veja [CONTRIBUTING](/CONTRIBUTING.md) para detalhes.

## Licença

Copyright (c) 2022, Alex Maycon da Silva, [https://www.alexmaycon.dev](https://www.alexmaycon.dev). All rights reserved. Licensed under the Apache License, Version 2.0 (the "License").

Veja [LICENSE](/LICENSE.md) para detalhes.
