# SupportCenter

## Leverantör

Sundsvalls kommun

## Beskrivning
SupportCenter är en tjänst som hanterar ärendeuppdateringar mot POB.

## Tekniska detaljer

### Integrationer
Tjänsten integrerar mot:

* POB

### Starta tjänsten

|Konfigurationsnyckel|Beskrivning|
|---|---|
|`integration.pob.url`|URL till backend-system POB|


### Paketera och starta tjänsten
Applikationen kan paketeras genom:

```
./mvnw package
```
Kommandot skapar filen `api-service-supportcenter-<version>.jar` i katalogen `target`. Tjänsten kan nu köras genom kommandot `java -jar target/api-service-supportcenter-<version>.jar`.

### Bygga och starta med Docker
Exekvera följande kommando för att bygga en Docker-image:

```
docker build -f src/main/docker/Dockerfile -t api.sundsvall.se/ms-supportcenter:latest .
```

Exekvera följande kommando för att starta samma Docker-image i en container:

```
docker run -i --rm -p8080:8080 api.sundsvall.se/ms-supportcenter

```

#### Kör applikationen lokalt

Exekvera följande kommando för att bygga och starta en container i sandbox mode:  

```
docker-compose -f src/main/docker/docker-compose-sandbox.yaml build && docker-compose -f src/main/docker/docker-compose-sandbox.yaml up
```


## 
Copyright (c) 2021 Sundsvalls kommun