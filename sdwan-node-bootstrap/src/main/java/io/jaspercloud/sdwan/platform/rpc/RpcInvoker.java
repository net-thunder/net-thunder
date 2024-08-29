package io.jaspercloud.sdwan.platform.rpc;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RpcInvoker {

    private static final int PORT = 45785;

    private static Channel channel;

    public static <T> T buildClient(Class<T> clazz) throws Exception {
        getChannel();
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
                String id = UUID.randomUUID().toString();
                CompletableFuture<SDWanProtos.RpcMessage> task = AsyncTask.waitTask(id, 5000);
                getChannel().writeAndFlush(RpcMessageBuilder.encodeRequest(id, method, args));
                RpcResponse rpcResp = RpcMessageBuilder.decodeRpcResponse(task.get());
                return rpcResp.getResult();
            }
        });
        return (T) proxy;
    }

    private static synchronized Channel getChannel() throws Exception {
        if (null != channel) {
            if (channel.isActive()) {
                return channel;
            } else {
                channel.close();
            }
        }
        int tryCount = 15;
        ProcessException ex;
        while (tryCount > 0) {
            try {
                channel = RpcChannel.clientChannel("localhost", PORT, new RpcMessageHandler() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.RpcMessage msg) throws Exception {
                        AsyncTask.completeTask(msg.getId(), msg);
                    }
                });
                return channel;
            } catch (ProcessException e) {
                tryCount--;
                ex = e;
            }
            if (0 == tryCount) {
                throw ex;
            }
        }
        throw new ProcessException();
    }

    public static <T> Channel exportServer(Class<T> clazz, T target) throws Exception {
        Map<String, Method> methodMap = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methodMap.put(method.toString(), method);
        }
        Channel channel = RpcChannel.serverChannel(PORT, new RpcMessageHandler() {
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
