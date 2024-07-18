package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.LongAttr;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.stun.StunPacket;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.CountBarrier;
import io.jaspercloud.sdwan.support.Ecdh;
import io.jaspercloud.sdwan.util.AddressType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public abstract class ElectionProtocol {

    private P2pClient p2pClient;
    private RelayClient relayClient;
    private KeyPair encryptionKeyPair;

    public ElectionProtocol(P2pClient p2pClient, RelayClient relayClient, KeyPair encryptionKeyPair) {
        this.p2pClient = p2pClient;
        this.relayClient = relayClient;
        this.encryptionKeyPair = encryptionKeyPair;
    }

    public CompletableFuture<DataTransport> offer(SDWanProtos.NodeInfo nodeInfo) {
        List<AddressUri> uriList = nodeInfo.getAddressUriList()
                .stream()
                .map(u -> AddressUri.parse(u))
                .collect(Collectors.toList());
        List<PingRequest> pingRequestList = uriList.stream().map(uri -> {
            if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri);
            } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        BlockingQueue<DataTransport> queue = new LinkedBlockingQueue<>();
        pingRequestList.stream().forEach(e -> {
            e.execute().thenAccept(pingResp -> {
                AddressUri uri = e.getDstUri();
                if (AddressType.RELAY.equals(uri.getScheme())) {
                    String token = uri.getParams().get("token");
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new RelayTransport(relayClient, token, order);
                    queue.add(dataTransport);
                } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                    InetSocketAddress sender = pingResp.sender();
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new P2pTransport(p2pClient, sender, order);
                    queue.add(dataTransport);
                } else {
                    throw new UnsupportedOperationException();
                }
            });
        });
        SDWanProtos.P2pOffer p2pOfferReq = SDWanProtos.P2pOffer.newBuilder()
                .setSrcVIP(getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .addAllAddressUri(pingRequestList.stream().map(e -> e.getSrcUri().toString()).collect(Collectors.toList()))
                .setPublicKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                .build();
        return sendOffer(p2pOfferReq)
                .thenApply(resp -> {
                    try {
                        List<DataTransport> transportList = queue.stream().collect(Collectors.toList());
                        byte[] publicKey = resp.getPublicKey().toByteArray();
                        SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), publicKey);
                        DataTransport transport = selectDataTransport(transportList);
                        transport.setSecretKey(secretKey);
                        return transport;
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<DataTransport> answer(String reqId, SDWanProtos.P2pOffer p2pOffer) {
        List<AddressUri> uriList = p2pOffer.getAddressUriList().stream()
                .map(u -> AddressUri.parse(u))
                .collect(Collectors.toList());
        List<PingRequest> pingRequestList = uriList.stream().map(uri -> {
            if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri);
            } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        CompletableFuture<List<DataTransport>> future = AsyncTask.create(3000);
        CountBarrier<DataTransport> countBarrier = new CountBarrier<>(pingRequestList.size(), new Consumer<List<DataTransport>>() {
            @Override
            public void accept(List<DataTransport> list) {
                future.complete(list);
            }
        });
        pingRequestList.stream().forEach(e -> {
            e.execute().thenAccept(pingResp -> {
                try {
                    AddressUri uri = e.getDstUri();
                    if (AddressType.RELAY.equals(uri.getScheme())) {
                        String token = uri.getParams().get("token");
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new RelayTransport(relayClient, token, order);
                        countBarrier.add(dataTransport);
                    } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                        InetSocketAddress sender = pingResp.sender();
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new P2pTransport(p2pClient, sender, order);
                        countBarrier.add(dataTransport);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } finally {
                    countBarrier.countDown();
                }
            });
        });
        return future.handle((transportList, ex) -> {
            try {
                if (null == transportList) {
                    transportList = Collections.emptyList();
                }
                byte[] publicKey = p2pOffer.getPublicKey().toByteArray();
                SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), publicKey);
                DataTransport transport = selectDataTransport(transportList);
                transport.setSecretKey(secretKey);
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setCode(SDWanProtos.MessageCode.Success)
                        .setSrcVIP(p2pOffer.getDstVIP())
                        .setDstVIP(p2pOffer.getSrcVIP())
                        .setPublicKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                        .build();
                sendAnswer(reqId, p2pAnswer);
                return transport;
            } catch (Exception e) {
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setCode(SDWanProtos.MessageCode.SysError)
                        .build();
                sendAnswer(reqId, p2pAnswer);
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    private DataTransport selectDataTransport(List<DataTransport> transportList) {
        if (transportList.isEmpty()) {
            throw new ProcessException("not found transport");
        }
        Collections.sort(transportList, (o1, o2) -> NumberUtils.compare(o2.order(), o1.order()));
        Optional<DataTransport> optional = transportList.stream().filter(e -> e instanceof P2pTransport).findFirst();
        if (!optional.isPresent()) {
            optional = transportList.stream().filter(e -> e instanceof RelayTransport).findFirst();
        }
        DataTransport transport = optional.get();
        return transport;
    }

    private PingRequest parseRelayPing(AddressUri uri) {
        String token = uri.getParams().get("token");
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> relayClient.ping(token, 3000));
        pingRequest.setSrcUri(AddressUri.builder()
                .scheme(uri.getScheme())
                .host("0.0.0.0")
                .port(0)
                .params(Collections.singletonMap("token", relayClient.getCurToken()))
                .build());
        pingRequest.setDstUri(uri);
        return pingRequest;
    }

    private PingRequest parseP2pPing(AddressUri uri) {
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());
        NatAddress natAddress = getNatAddress();
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> p2pClient.ping(addr, 3000));
        pingRequest.setSrcUri(AddressUri.builder()
                .scheme(uri.getScheme())
                .host(natAddress.getMappingAddress().getHostString())
                .port(p2pClient.getLocalPort())
                .params(Collections.singletonMap("mappingType", natAddress.getMappingType().name()))
                .build());
        pingRequest.setDstUri(uri);
        return pingRequest;
    }

    protected abstract CompletableFuture<SDWanProtos.P2pAnswer> sendOffer(SDWanProtos.P2pOffer p2pOffer);

    protected abstract void sendAnswer(String reqId, SDWanProtos.P2pAnswer p2pAnswer);

    protected abstract NatAddress getNatAddress();

    protected abstract String getLocalVip();

    @Getter
    @Setter
    private static class PingRequest {

        private Supplier<CompletableFuture<StunPacket>> supplier;
        private AddressUri srcUri;
        private AddressUri dstUri;

        public CompletableFuture<StunPacket> execute() {
            return supplier.get();
        }
    }

    private static class P2pTransport implements DataTransport {

        private P2pClient p2pClient;
        private InetSocketAddress address;
        private SecretKey secretKey;
        private long order;

        public P2pTransport(P2pClient p2pClient, InetSocketAddress address, long order) {
            this.p2pClient = p2pClient;
            this.address = address;
            this.order = -order;
        }

        @Override
        public void setSecretKey(SecretKey secretKey) {
            this.secretKey = secretKey;
        }

        @Override
        public long order() {
            return order;
        }

        @Override
        public void ping(long timeout) throws Exception {
            p2pClient.ping(address, timeout).get();
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                p2pClient.transfer(vip, address, encodeData);
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }

        @Override
        public byte[] decode(byte[] bytes) {
            try {
                byte[] decodeData = Ecdh.decryptAES(bytes, secretKey);
                return decodeData;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }
    }

    private static class RelayTransport implements DataTransport {

        private RelayClient relayClient;
        private String token;
        private SecretKey secretKey;
        private long order;

        public RelayTransport(RelayClient relayClient, String token, long order) {
            this.relayClient = relayClient;
            this.token = token;
            this.order = -order;
        }

        @Override
        public void setSecretKey(SecretKey secretKey) {
            this.secretKey = secretKey;
        }

        @Override
        public long order() {
            return order;
        }

        @Override
        public void ping(long timeout) throws Exception {
            relayClient.ping(token, timeout).get();
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                relayClient.transfer(vip, token, encodeData);
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }

        @Override
        public byte[] decode(byte[] bytes) {
            try {
                byte[] decodeData = Ecdh.decryptAES(bytes, secretKey);
                return decodeData;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }
    }
}
