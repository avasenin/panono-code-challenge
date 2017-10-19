# Panono code challenge

The server implements the following [API](./swagger.yaml) (feel free to use swagger file to generate a client).

My comments & context:
* It was pretty fun. I used the simillar task in the past for interview.
* In case of no data the server returns statistics with 0 `{"count":0, "min": 0, "max":0 ...}`
* Original task description use `{"avg": 1.0, "count": 2}` as an example for `/statistics` endpoint. I think it's wrong, `avg` should be 1.5 in the text
* Due to time discrepancy, clients time could be ahead of server time. Server allows `/upload` requests from the _future_ if `client_epoch - server_epoch <= 1 second`

## How to run server

Type the following command to run server on 8080 port:

```
$ ./gradlew bootRun
```

Use the following command to run unit and integration tests:

```
./gradlew check
```

## How to check manually that everything is working

```
$ http POST :8080/upload timestamp=`date +%s` count=1
HTTP/1.1 202
Content-Length: 0
Date: Thu, 19 Oct 2017 22:06:18 GMT

$ http POST :8080/upload timestamp=`date +%s` count=2
HTTP/1.1 202
Content-Length: 0
Date: Thu, 19 Oct 2017 22:06:23 GMT

$ http GET :8080/statistics
HTTP/1.1 200
Content-Type: application/json;charset=UTF-8
Date: Thu, 19 Oct 2017 22:06:26 GMT
Transfer-Encoding: chunked

{
    "avg": 1.5,
    "count": 2,
    "max": 2,
    "min": 1,
    "sum": 3
}
```
