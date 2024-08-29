package io.jaspercloud.sdwan.platform.rpc;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.lang.reflect.Method;

public class RpcMessageBuilder {

    public static SDWanProtos.RpcMessage encodeRequest(String id, Method method, Object[] args) throws Exception {
        RpcRequest request = new RpcRequest();
        request.setKey(method.toString());
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        SDWanProtos.RpcMessage rpcMessage = SDWanProtos.RpcMessage.newBuilder()
                .setId(id)
                .setData(ByteString.copyFrom(HessianCodec.codec(RpcRequest.class).encode(request)))
                .build();
        return rpcMessage;
    }

    public static RpcRequest decodeRequest(SDWanProtos.RpcMessage msg) throws Exception {
        RpcRequest request = HessianCodec.codec(RpcRequest.class).decode(msg.getData().toByteArray());
        return request;
    }

    public static SDWanProtos.RpcMessage encodeResponse(SDWanProtos.RpcMessage msg, RpcResponse response) throws Exception {
        byte[] encode = HessianCodec.codec(RpcResponse.class).encode(response);
        SDWanProtos.RpcMessage rpcMessage = msg.toBuilder().setData(ByteString.copyFrom(encode)).build();
        return rpcMessage;
    }

    public static RpcResponse decodeRpcResponse(SDWanProtos.RpcMessage rpcResp) throws Exception {
        RpcResponse response = HessianCodec.codec(RpcResponse.class).decode(rpcResp.getData().toByteArray());
        return response;
    }
}
