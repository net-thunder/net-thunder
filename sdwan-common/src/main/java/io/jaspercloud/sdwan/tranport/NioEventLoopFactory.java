package io.jaspercloud.sdwan.tranport;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NioEventLoopFactory {

    public static NioEventLoopGroup createBossGroup() {
        return new NioEventLoopGroup(
                Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("nioBossLoop")
        );
    }

    public static NioEventLoopGroup createWorkerGroup() {
        return new NioEventLoopGroup(
                Runtime.getRuntime().availableProcessors() * 2,
                new DefaultThreadFactory("nioWorkerLoop")
        );
    }

    public static EventLoopGroup createEventLoopGroup() {
        return new DefaultEventLoopGroup(
                Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("nioBossLoop")
        );
    }

}
