package com.demo.grpc.rangechunker;

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
public class SingRangeChunkerClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(SingRangeChunkerClient.class);
    private static final String HOST = "192.168.202.137";
    private static final int PORT = 8888;
    private int chunkSize = 64 * 1024;

    public static void main(String[] args){
        long start = System.currentTimeMillis();
//          String fileName = "10k-1.txt";
//        String fileName = "65k-1.txt";
//        String fileName = "1Mb-1.txt";
//        String fileName = "100Mb-1.txt";
        String fileName = "200Mb-1.txt";
        if(args.length > 1){
            fileName = args[0];
        }
        try {
            SingRangeChunkerClient client = new SingRangeChunkerClient(HOST, PORT);
            client.batchDownload(fileName);
            client.shutdown();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        long end = System.currentTimeMillis();
        LOGGER.warn("[客户端单线程下载]filename:{}下载完成,下载时间:{}",fileName,end-start);
    }

    private ManagedChannel managedChannel;

    private RangeChunkerGrpc.RangeChunkerBlockingStub blockingStub;

    public SingRangeChunkerClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * 客户端单线程下载
     * @param name
     * @throws IOException
     */
    public void download(String name) throws IOException{
        RandomAccessFile file = new RandomAccessFile("/Users/peijiepang/Downloads/pycharm11111.dmg","rw");
        FileChannel channel = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(chunkSize);

        long position = 0;
        RangeRequest rangeRequest = RangeRequest.newBuilder().setFileName(name).setPosition(position).build();
        RangeChunkResponse response = blockingStub.range(rangeRequest);
        while (response.getCode() == 1){
            buf.clear();
            buf.put(response.getChunk().toByteArray());
            buf.flip();
            channel.write(buf);

            rangeRequest = RangeRequest.newBuilder().setFileName(name).setPosition(response.getNextposition()).build();
            response = blockingStub.range(rangeRequest);
        }
        LOGGER.info("下载完成:{}",name);
    }

    /**
     * 客户端单线程下载
     * @param name
     * @throws IOException
     */
    public void batchDownload(String name) throws IOException{
        String fileName = "/temp/download/"+name;
        RandomAccessFile file = new RandomAccessFile(fileName,"rw");
        FileChannel channel = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(chunkSize);

        FileSizeRequest fileSizeRequest = FileSizeRequest.newBuilder().setFileName(name).buildPartial();
        FileSizeResponse fileSizeResponse = blockingStub.getFileSize(fileSizeRequest);
        int loopSize = fileSizeResponse.getSize() / chunkSize + 1;
        int start = 0;
        int end = chunkSize;
        for(int i=0;i<loopSize;i++){
            RangeRegionRequest rangeRegionRequest = RangeRegionRequest.newBuilder().setFileName(name).setStart(start).setEnd(end).build();
            Iterator<RangeRegionResponse> rangeRegionResponseIterator = blockingStub.rangeRegion(rangeRegionRequest);
            while (rangeRegionResponseIterator.hasNext()){
                RangeRegionResponse rangeRegionResponse = rangeRegionResponseIterator.next();
                buf.clear();
                buf.put(rangeRegionResponse.getChunk().toByteArray());
                buf.flip();
                channel.write(buf);
            }
            start = end;
            end = start + chunkSize;
        }
        LOGGER.info("下载完成:{}",name);
    }

    /**
     * 关闭客户端
     */
    public void shutdown() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    SingRangeChunkerClient(ManagedChannelBuilder<?> channelBuilder) {
        managedChannel = channelBuilder.build();
        blockingStub = RangeChunkerGrpc.newBlockingStub(managedChannel);
    }

}
