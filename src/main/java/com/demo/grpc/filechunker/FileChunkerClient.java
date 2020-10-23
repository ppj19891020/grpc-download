package com.demo.grpc.filechunker;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: peijiepang
 * @date 2020/10/19
 * @Description:
 */
public class FileChunkerClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileChunkerClient.class);
    private static final String HOST = "192.168.202.137";
    private static final int PORT = 8888;
    private int chunkSize = 64 * 1024;

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
            FileChunkerClient client = new FileChunkerClient(HOST, PORT);
            client.download(fileName);
            client.shutdown();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        long end = System.currentTimeMillis();
        LOGGER.warn("[服务端流式下载]filename:{}下载完成,下载时间:{}",fileName,end-start);
    }

    private ManagedChannel managedChannel;

    private ChunkerGrpc.ChunkerBlockingStub blockingStub;

    public FileChunkerClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * 服务端流式-客户端下载
     * @param name
     * @throws IOException
     */
    public void download(String name) throws IOException{
        String fileName = "/temp/download/"+name;
        ChunkRequest request = ChunkRequest.newBuilder().setFileName(name).build();
        Iterator<ChunkResponse> responseIterator = blockingStub.chunker(request);
        RandomAccessFile file = new RandomAccessFile(fileName,"rw");
        FileChannel channel = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(chunkSize);

        while (responseIterator.hasNext()){
            ChunkResponse chunkResponse = responseIterator.next();
            LOGGER.info(Thread.currentThread().getName()+"-"+chunkResponse.getBatchid());
            buf.clear();
            buf.put(chunkResponse.getChunk().toByteArray());
            buf.flip();
            channel.write(buf);
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    FileChunkerClient(ManagedChannelBuilder<?> channelBuilder) {
        managedChannel = channelBuilder.build();
        blockingStub = ChunkerGrpc.newBlockingStub(managedChannel);
    }

}
