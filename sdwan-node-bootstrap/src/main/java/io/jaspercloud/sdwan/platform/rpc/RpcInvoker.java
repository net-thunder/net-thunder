package io.jaspercloud.sdwan.platform.rpc;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.ShortUUID;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class RpcInvoker {

    public static <T> T buildClient(Class<T> clazz, int port) throws Exception {
        Supplier<Channel> supplier = new Supplier<Channel>() {

            private Channel channel;

            @Override
            public Channel get() {
                synchronized (clazz) {
                    if (null != channel) {
                        if (channel.isActive()) {
                            return channel;
                        } else {
                            channel.close();
                        }
                    }
                    try {
                        channel = RpcChannel.clientChannel("localhost", port, new RpcMessageHandler() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.RpcMessage msg) throws Exception {
                                AsyncTask.completeTask(msg.getId(), msg);
                            }
                        });
                        return channel;
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                }
            }
        };
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{clazz}, new InvocationHandler() {
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
                String id = ShortUUID.gen();
                CompletableFuture<SDWanProtos.RpcMessage> task = AsyncTask.waitTask(id, 5000);
                supplier.get().writeAndFlush(RpcMessageBuilder.encodeRequest(id, method, args));
                RpcResponse rpcResp = RpcMessageBuilder.decodeRpcResponse(task.get());
                return rpcResp.getResult();
            }
        });
        return (T) proxy;
    }

    public static <T> Channel exportServer(Class<T> clazz, T target, int port) throws Exception {
        Map<String, Method> methodMap = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methodMap.put(method.toString(), method);
        }
        Channel channel = RpcChannel.serverChannel(port, new RpcMessageHandler() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.RpcMessage msg) throws Exception {
                RpcResponse response = new RpcResponse();
                try {
                    RpcRequest request = RpcMessageBuilder.decodeRequest(msg);
                    Method method = methodMap.get(request.getKey());
                    if (null == method) {
                        throw new ProcessException("not found method: " + request.getKey());
                    }
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
