## Streams
Streams is a containerized means to handle java streams. It operates off redis streams, however 99% of the codebase is complete to infterface with kafka topics

## Build
Prior to running latest version, pull down a fresh version of runway, puddle, hangar and cloud. In each run
```
docker build -t {project name}
```

From your IDE, export a jar

Next build docker container
```
docker build -t stream .
```

## Run
```
docker compose up -d
```

To view output run, view the logs in the puddle project. All messages are printed to logs
```
docker logs puddle
```


