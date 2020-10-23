package com.demo.grpc.rangechunker;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author: peijiepang
 * @date 2020/10/21
 * @Description:
 */
public class RangeChunkerImplBaseImpl extends RangeChunkerGrpc.RangeChunkerImplBase {

    private int chunkSize = 64 * 1024;

    @Override
    public void range(RangeRequest request, StreamObserver<RangeChunkResponse> responseObserver) {
        // 文件名
        String fileName = "/tmp/download/" + request.getFileName();
        RangeChunkResponse rangeChunkResponse = null;
        FileInputStream fileInputStream = null;
        FileChannel fileChannel = null;
        try{
            fileInputStream = new FileInputStream(fileName);
            fileChannel = fileInputStream.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(chunkSize);
            // 定位偏移量
            fileChannel.position(request.getPosition());
            int readCount = fileChannel.read(byteBuffer);
            if(readCount != -1){
                byte[]array = new byte[readCount];
                byteBuffer.flip();
                byteBuffer.get(array);
                byteBuffer.clear();
                // 文件读取
                rangeChunkResponse = RangeChunkResponse.newBuilder().setNextposition(fileChannel.position()).
                    setCode(1).setChunk(ByteString.copyFrom(array)).build();
            }else{
                // 文件读取完成
                rangeChunkResponse = RangeChunkResponse.newBuilder().setNextposition(fileChannel.position()).
                    setCode(0).build();
            }
        }catch (IOException ex){
            ex.printStackTrace();
            rangeChunkResponse = RangeChunkResponse.newBuilder().setCode(-1).build();
        }finally {
            try {
                if(null != fileInputStream){
                    fileInputStream.close();
                }
                if(null != fileChannel){
                    fileChannel.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        responseObserver.onNext(rangeChunkResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getFileSize(FileSizeRequest request,
        StreamObserver<FileSizeResponse> responseObserver) {
        String fileName = "/tmp/download/" + request.getFileName();
        FileInputStream fileInputStream = null;
        int fileSize = 0;
        try{
            fileInputStream = new FileInputStream(fileName);
            fileSize = fileInputStream.available();
        }catch (IOException ex){
            ex.printStackTrace();
        }
        FileSizeResponse fileSizeResponse = FileSizeResponse.newBuilder().setSize(fileSize).build();
        responseObserver.onNext(fileSizeResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void rangeRegion(RangeRegionRequest request,
        StreamObserver<RangeRegionResponse> responseObserver) {
        String fileName = "/tmp/download/" + request.getFileName();
        ByteBuffer byteBuffer = ByteBuffer.allocate(chunkSize);
        RangeRegionResponse rangeRegionResponse = null;
        FileInputStream fileInputStream = null;
        FileChannel fileChannel = null;
        try{
            fileInputStream = new FileInputStream(fileName);
            fileChannel = fileInputStream.getChannel();
            fileChannel.position(request.getStart());
            long length = request.getEnd() - request.getStart();
            int loopSize = (int) length/chunkSize;
            for(int i=0;i<loopSize;i++){
                int readCount = fileChannel.read(byteBuffer);
                if(-1 != readCount){
                    byte[]array = new byte[readCount];
                    byteBuffer.flip();
                    byteBuffer.get(array);
                    byteBuffer.clear();
                    rangeRegionResponse = RangeRegionResponse.newBuilder().setChunk(ByteString.copyFrom(array)).build();
                    responseObserver.onNext(rangeRegionResponse);
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }finally {
            try{
                fileChannel.close();
                fileInputStream.close();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
        responseObserver.onCompleted();
    }
}
