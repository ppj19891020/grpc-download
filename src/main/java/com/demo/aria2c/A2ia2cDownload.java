package com.demo.aria2c;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: peijiepang
 * @date 2020/10/23
 * @Description:
 */
public class A2ia2cDownload {

    private final static Logger LOGGER = LoggerFactory.getLogger(A2ia2cDownload.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
//        String fileName = "10k-1.txt";
//        String fileName = "65k-1.txt";
//        String fileName = "1Mb-1.txt";
        String fileName = "100Mb-1.txt";
//        String fileName = "200Mb-1.txt";

        String command = "aria2c -s 6  -j 50 -x 16 -k 2M -d /temp/download --timeout=600 --max-tries=2 --stop=1800 "
            + "--allow-overwrite=true --enable-http-keep-alive=true --log-level=warn  http://192.168.202.137:9999/download/"+fileName;
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.waitFor();
        long end = System.currentTimeMillis();
        LOGGER.warn("[ari2c下载]filename:{}下载完成,下载时间:{}",fileName,end-start);

    }

}
