package com.demo.grpc.rangechunker;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: peijiepang
 * @date 2020/10/19
 * @Description:
 */
public class ThreadRangeChunkerClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(ThreadRangeChunkerClient.class);
    private static final String HOST = "192.168.202.137";
    private static final int PORT = 8888;
    private static ExecutorService executorService = Executors.newFixedThreadPool(16);

    public static void main(String[] args){
        long start = System.currentTimeMillis();
//        String fileName = "10k-1.txt";
//        String fileName = "65k-1.txt";
//        String fileName = "1Mb-1.txt";
//        String fileName = "100Mb-1.txt";
        String fileName = "200Mb-1.txt";
        if(args.length > 1){
            fileName = args[0];
        }
        try {
            ThreadRangeChunkerClient client = new ThreadRangeChunkerClient(HOST, PORT);
            client.batchThreadDownload(fileName);
            client.shutdown();
            executorService.shutdown();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        long end = System.currentTimeMillis();
        LOGGER.warn("[客户端多线程下载]filename:{}下载完成,下载时间:{}",fileName,end-start);
    }

    private ManagedChannel managedChannel;

    private RangeChunkerGrpc.RangeChunkerBlockingStub blockingStub;

    public ThreadRangeChunkerClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * 客户端多线程下载
     * @param name
     * @throws IOException
     */
    public void batchThreadDownload(String name) throws IOException{
        int chunkSize = 2 * 1024 * 1024;
        String fileName = "/temp/download/"+name;
        RandomAccessFile file = new RandomAccessFile(fileName,"rw");
        FileChannel channel = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(chunkSize);

        List<Future<List<ByteString>>> futures = new ArrayList<>();
        FileSizeRequest fileSizeRequest = FileSizeRequest.newBuilder().setFileName(name).buildPartial();
        FileSizeResponse fileSizeResponse = blockingStub.getFileSize(fileSizeRequest);
        int loopSize = fileSizeResponse.getSize() / chunkSize + 1;
        int start = 0;
        int end = chunkSize;
        for(int i=0;i<loopSize;i++){
            Future<List<ByteString>> future = executorService.submit(new DownloadThread(name,start,end));
            futures.add(future);
            start = end;
            end = start + chunkSize;
        }
        for(int i=0;i<futures.size();i++){
            Future<List<ByteString>> future = futures.get(i);
            try {
                List<ByteString> list = future.get();
                for(int j=0;j<list.size();j++){
                    ByteString byteString = list.get(j);
                    buf.clear();
                    buf.put(byteString.toByteArray());
                    buf.flip();
                    channel.write(buf);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("下载完成:{}",name);
    }

    class DownloadThread implements Callable<List<ByteString>> {

        public DownloadThread(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        private String name;
        private int start;
        private int end;

        @Override
        public List<ByteString> call() throws Exception {
            List<ByteString> result = new ArrayList<>();
            RangeRegionRequest rangeRegionRequest = RangeRegionRequest.newBuilder().setFileName(name).setStart(start).setEnd(end).build();
            Iterator<RangeRegionResponse> rangeRegionResponseIterator = blockingStub.rangeRegion(rangeRegionRequest);
            while (rangeRegionResponseIterator.hasNext()){
                RangeRegionResponse rangeRegionResponse = rangeRegionResponseIterator.next();
                result.add(rangeRegionResponse.getChunk());
            }
            return result;
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    ThreadRangeChunkerClient(ManagedChannelBuilder<?> channelBuilder) {
        managedChannel = channelBuilder.build();
        blockingStub = RangeChunkerGrpc.newBlockingStub(managedChannel);
    }

}
