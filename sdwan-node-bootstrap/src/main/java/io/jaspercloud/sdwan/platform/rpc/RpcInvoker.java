package io.jaspercloud.sdwan.platform.rpc;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RpcInvoker {

    private static final int PORT = 45785;

    public static <T> T buildClient(Class<T> clazz) throws Exception {
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{clazz}, new InvocationHandler() {

            private Channel channel;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(clazz, args);
                }
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    if ("toString".equals(methodName)) {
                        return clazz.toString();
                    } else if ("hashCode".equals(methodName)) {
                        return clazz.hashCode();
                    }
                } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
                    return clazz.equals(args[0]);
                }
                String id = UUID.randomUUID().toString();
                SDWanProtos.RpcMessage rpcReq = buildRpcRequest(id, method, args);
                CompletableFuture<SDWanProtos.RpcMessage> task = AsyncTask.waitTask(id, 30000);
                getChannel().writeAndFlush(rpcReq);
                SDWanProtos.RpcMessage rpcResp = task.get();
                RpcResponse response = HessianCodec.codec(RpcResponse.class).decode(rpcResp.getData().toByteArray());
                return response.getResult();
            }

            private Channel getChannel() throws Exception {
                if (null != channel) {
                    if (channel.isActive()) {
                        return channel;
                    } else {
                        channel.close();
                    }
                }
                channel = RpcChannel.clientChannel("localhost", PORT, new SimpleChannelInboundHandler<SDWanProtos.RpcMessage>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.RpcMessage msg) throws Exception {
                        AsyncTask.completeTask(msg.getId(), msg);
                    }
                });
                return channel;
            }

            private SDWanProtos.RpcMessage buildRpcRequest(String id, Method method, Object[] args) throws Exception {
                RpcRequest request = new RpcRequest();
                request.setKey(method.toString());
                request.setInterfaceName(method.getDeclaringClass().getName());
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);
                SDWanProtos.RpcMessage rpcReq = SDWanProtos.RpcMessage.newBuilder()
                        .setId(id)
                        .setData(ByteString.copyFrom(HessianCodec.codec(RpcRequest.class).encode(request)))
                        .build();
                return rpcReq;
            }
        });
        return (T) proxy;
    }

    public static <T> Channel exportServer(Class<T> clazz, T target) throws Exception {
        Map<String, Method> methodMap = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methodMap.put(method.toString(), method);
        }
        Channel channel = RpcChannel.serverChannel(PORT, new SimpleChannelInboundHandler<SDWanProtos.RpcMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.RpcMessage msg) throws Exception {
                RpcResponse response = new RpcResponse();
                try {
                    RpcRequest request = HessianCodec.codec(RpcRequest.class).decode(msg.getData().toByteArray());
                    Method method = methodMap.get(request.getKey());
                    Object result = method.invoke(target, request.getParameters());
                    response.setResult(result);
                } catch (Throwable e) {
                    response.setThrowable(e);
                } finally {
                    byte[] encode = HessianCodec.codec(RpcResponse.class).encode(response);
                    SDWanProtos.RpcMessage rpcMessage = msg.toBuilder().setData(ByteString.copyFrom(encode)).build();
                    ctx.writeAndFlush(rpcMessage);
                }
            }
        });
        return channel;
    }
}
