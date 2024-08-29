package io.jaspercloud.sdwan.platform.rpc;

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
                CompletableFuture<SDWanProtos.RpcMessage> task = AsyncTask.waitTask(id, 30000);
                getChannel().writeAndFlush(RpcMessageBuilder.encodeRequest(id, method, args));
                RpcResponse rpcResp = RpcMessageBuilder.decodeRpcResponse(task.get());
                return rpcResp.getResult();
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
                    RpcRequest request = RpcMessageBuilder.decodeRequest(msg);
                    Method method = methodMap.get(request.getKey());
                    Object result = method.invoke(target, request.getParameters());
                    response.setResult(result);
                } catch (Throwable e) {
                    response.setThrowable(e);
                } finally {
                    ctx.writeAndFlush(RpcMessageBuilder.encodeResponse(msg, response));
                }
            }
        });
        return channel;
    }
}
