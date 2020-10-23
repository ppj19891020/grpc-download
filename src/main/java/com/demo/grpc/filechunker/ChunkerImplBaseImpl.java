package com.demo.grpc.filechunker;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: peijiepang
 * @date 2020/10/20
 * @Description:
 */
public class ChunkerImplBaseImpl extends ChunkerGrpc.ChunkerImplBase{
    private final static Logger LOGGER = LoggerFactory.getLogger(ChunkerImplBaseImpl.class);
    private int chunkSize = 64 * 1024;

    @Override
    public void chunker(ChunkRequest request, StreamObserver<ChunkResponse> responseObserver) {
        // 文件名
        String filePath = "/tmp/download/" + request.getFileName();
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            FileChannel channel = fileInputStream.getChannel();
            ByteBuffer byteBuf = ByteBuffer.allocate(chunkSize);
            int count = channel.read(byteBuf);
            byte[] array;
            int batchId = 1;//批次id
            while (count != -1) {
                array = new byte[count];// 字节数组长度为已读取长度
                byteBuf.flip();
                byteBuf.get(array);// 从ByteBuffer中得到字节数组
                byteBuf.clear();
                count = channel.read(byteBuf);
                ChunkResponse chunkResponse = ChunkResponse.newBuilder().setChunk(ByteString.copyFrom(array)).setBatchid(batchId).build();
                responseObserver.onNext(chunkResponse);
                batchId++;
            }
            responseObserver.onCompleted();
            LOGGER.info("下载推送完成，filename:{} size:{}",request.getFileName(),fileInputStream.available()/1024/1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
