package com.demo.grpc.file;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author: peijiepang
 * @date 2020/10/19
 * @Description:
 */
public class FileServiceImplBaseImpl extends FileServiceGrpc.FileServiceImplBase{

    @Override
    public void upload(Request request, StreamObserver<Response> responseObserver) {
        byte[] bytes = request.getFile().toByteArray();
        System.out.println(String.format("收到文件%s长度%s", request.getName(), bytes.length));
        File f = new File("/tmp/" + request.getName());
        Response response;
        if (f.exists()) {
            f.delete();
        }
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(bytes);
            response = Response.newBuilder().setCode(1).setMsg("上传成功").build();
        } catch (IOException e) {
            response = Response.newBuilder().setCode(-1).setMsg(String.format("上传失败:%s", e.getMessage())).build();
            e.printStackTrace();
        }
        // 返回数据，完成此次请求
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void download(DownloadRequest request,
        StreamObserver<DownloadResponse> responseObserver) {
        String fileName = request.getName();
        byte[] bytes = null;
        try {
            bytes = getContent("/tmp/"+fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DownloadResponse downloadResponse;
        if(null == bytes){
            downloadResponse = DownloadResponse.newBuilder().setCode(-1).build();
        }else{
            downloadResponse = DownloadResponse.newBuilder().setCode(1).setFile(ByteString.copyFrom(bytes)).build();
        }
        responseObserver.onNext(downloadResponse);
        responseObserver.onCompleted();
    }

    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) {
            return null;
        }
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
            && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file "
                + file.getName());
        }
        fi.close();
        System.out.println("生成文件长度" + buffer.length);
        return buffer;
    }

}
