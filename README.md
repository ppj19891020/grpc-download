## 说明
1. arira2c下载

    `aria2c -s 6  -j 50 -x 16 -k 2M -d /temp/download --timeout=600 --max-tries=2 --stop=1800 --allow-overwrite=true --enable-http-keep-alive=true --log-level=warn  http://192.168.202.137:9999/download/`
    
    详细参数
    
    `https://aimigit.github.io/2018/12/27/aria2-command/`
2. grpc下载
    - 服务端流式下载
    - 客户端单线程分片下载
    - 客户端多线程分片下载


## 文件下载性能比较

| 下载方式                                                   | 10kb | 65kb | 1mb  | 100mb | 200mb |
| ---------------------------------------------------------- | ---- | ---- | ---- | ----- | ----- |
| 64k-Grpc服务端流式（服务器按照每批64k数据发送数据包）      | 1391 | 1241 | 1309 | 17389 | 36095 |
| 64k-Gprc客户端普通下载（客户端每批按照64k数据下载数据包）  | 1226 | 1365 | 1711 | 18056 | 38956 |
| 64k-Grpc客户端多线程下载（16线程数，多线程分批下载数据包） | 1213 | 1292 | 1326 | 18933 | 36039 |
| aria2c http下载(分片2MB)                                   | 168  | 169  | 299  | 16938 | 35372 |
| 2M-Grpc服务端流式（服务器按照每批2M数据发送数据包）        | 1367 | 1142 | 1678 | 17456 | 35773 |
| 2M-Gprc客户端普通下载（客户端每批按照2M数据下载数据包）    | 1687 | 1426 | 1573 | 18719 | 35774 |
| 2M-Grpc客户端多线程下载（16线程数，多线程分批下载数据包）  | 1316 | 1216 | 1293 | 18264 | 36022 |

