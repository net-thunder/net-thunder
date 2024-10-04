package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.LongAttr;
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

    private String tenantId;
    private P2pClient p2pClient;
    private RelayClient relayClient;
    private KeyPair encryptionKeyPair;
    private long electionTimeout;
    private long pingTimeout;

    public ElectionProtocol(String tenantId,
                            P2pClient p2pClient,
                            RelayClient relayClient,
                            KeyPair encryptionKeyPair,
                            long electionTimeout,
                            long pingTimeout) {
        this.tenantId = tenantId;
        this.p2pClient = p2pClient;
        this.relayClient = relayClient;
        this.encryptionKeyPair = encryptionKeyPair;
        this.electionTimeout = electionTimeout;
        this.pingTimeout = pingTimeout;
    }

    public CompletableFuture<DataTransport> offer(SDWanProtos.NodeInfo nodeInfo) {
        List<AddressUri> uriList = nodeInfo.getAddressUriList()
                .stream()
                .map(u -> AddressUri.parse(u))
                .collect(Collectors.toList());
        List<PingRequest> pingRequestList = uriList.stream().map(uri -> {
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, pingTimeout);
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, pingTimeout);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        BlockingQueue<DataTransport> queue = new LinkedBlockingQueue<>();
        pingRequestList.forEach(e -> {
            e.execute().thenAccept(pingResp -> {
                AddressUri uri = e.getAddressUri();
                log.info("pong: uri={}", uri.toString());
                if (AddressType.RELAY.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new RelayTransport(uri, relayClient, order);
                    queue.add(dataTransport);
                } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new P2pTransport(uri, p2pClient, order);
                    queue.add(dataTransport);
                } else {
                    throw new UnsupportedOperationException();
                }
            });
        });
        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.newBuilder()
                .setTenantId(tenantId)
                .setSrcVIP(getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .addAllAddressUri(getLocalAddressUriList())
                .setPublicKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                .build();
        return sendOffer(p2pOffer, 3 * electionTimeout)
                .thenApply(resp -> {
                    try {
                        List<DataTransport> transportList = queue.stream().collect(Collectors.toList());
                        byte[] publicKey = resp.getPublicKey().toByteArray();
                        SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), publicKey);
                        DataTransport transport = selectDataTransport(p2pOffer.getSrcVIP(), p2pOffer.getDstVIP(), transportList);
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
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, pingTimeout);
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, pingTimeout);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        CompletableFuture<List<DataTransport>> future = AsyncTask.create(3 * electionTimeout);
        CountBarrier<DataTransport> countBarrier = new CountBarrier<>(pingRequestList.size(), new Consumer<List<DataTransport>>() {
            @Override
            public void accept(List<DataTransport> list) {
                future.complete(list);
            }
        });
        CompletableFuture<List<DataTransport>> collectFuture = future.handle((list, ex) -> {
            if (null != ex) {
                return countBarrier.toList();
            } else {
                return list;
            }
        });
        pingRequestList.forEach(req -> {
            req.execute().thenAccept(pingResp -> {
                try {
                    AddressUri uri = req.getAddressUri();
                    log.info("pong: uri={}", uri.toString());
                    if (AddressType.RELAY.equals(uri.getScheme())) {
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new RelayTransport(uri, relayClient, order);
                        countBarrier.add(dataTransport);
                    } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new P2pTransport(uri, p2pClient, order);
                        countBarrier.add(dataTransport);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } finally {
                    countBarrier.countDown();
                }
            });
        });
        return collectFuture.thenApply(transportList -> {
            try {
                if (null == transportList) {
                    transportList = Collections.emptyList();
                }
                byte[] publicKey = p2pOffer.getPublicKey().toByteArray();
                SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), publicKey);
                DataTransport transport = selectDataTransport(p2pOffer.getSrcVIP(), p2pOffer.getDstVIP(), transportList);
                transport.setSecretKey(secretKey);
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setTenantId(tenantId)
                        .setCode(SDWanProtos.MessageCode.Success)
                        .setSrcVIP(p2pOffer.getDstVIP())
                        .setDstVIP(p2pOffer.getSrcVIP())
                        .setPublicKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                        .build();
                sendAnswer(reqId, p2pAnswer);
                return transport;
            } catch (Exception e) {
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setTenantId(tenantId)
                        .setCode(SDWanProtos.MessageCode.SysError)
                        .build();
                sendAnswer(reqId, p2pAnswer);
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    private DataTransport selectDataTransport(String srcVip, String dstVip, List<DataTransport> transportList) {
        if (transportList.isEmpty()) {
            throw new ProcessException("not found transport");
        }
        Collections.sort(transportList, (o1, o2) -> NumberUtils.compare(o2.order(), o1.order()));
        Optional<DataTransport> optional = transportList.stream().filter(e -> e instanceof P2pTransport).findFirst();
        if (!optional.isPresent()) {
            optional = transportList.stream().filter(e -> e instanceof RelayTransport).findFirst();
        }
        DataTransport transport = optional.get();
        log.info("selectDataTransport: {} -> {}, uri={}", srcVip, dstVip, transport.addressUri().toString());
        return transport;
    }

    private PingRequest parseP2pPing(AddressUri uri, long timeout) {
        log.info("ping uri: {}", uri.toString());
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> p2pClient.ping(addr, timeout));
        pingRequest.setAddressUri(uri);
        return pingRequest;
    }

    private PingRequest parseRelayPing(AddressUri uri, long timeout) {
        log.info("ping uri: {}", uri.toString());
        String token = uri.getParams().get("token");
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> relayClient.ping(token, timeout));
        pingRequest.setAddressUri(uri);
        return pingRequest;
    }

    protected abstract CompletableFuture<SDWanProtos.P2pAnswer> sendOffer(SDWanProtos.P2pOffer p2pOffer, long timeout);

    protected abstract void sendAnswer(String reqId, SDWanProtos.P2pAnswer p2pAnswer);

    protected abstract String getLocalVip();

    protected abstract List<String> getLocalAddressUriList();

    @Getter
    @Setter
    private static class PingRequest {

        private Supplier<CompletableFuture<StunPacket>> supplier;
        private AddressUri addressUri;

        public CompletableFuture<StunPacket> execute() {
            return supplier.get();
        }
    }

    private static class P2pTransport implements DataTransport {

        private AddressUri addressUri;
        private P2pClient p2pClient;
        private SecretKey secretKey;
        private long order;
        private InetSocketAddress address;

        public P2pTransport(AddressUri addressUri, P2pClient p2pClient, long order) {
            this.addressUri = addressUri;
            this.p2pClient = p2pClient;
            this.order = -order;
            address = new InetSocketAddress(addressUri.getHost(), addressUri.getPort());
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
        public AddressUri addressUri() {
            return addressUri;
        }

        @Override
        public void sendPingOneWay(String tranId) {
            p2pClient.sendPingOneWay(address, tranId);
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

        private AddressUri addressUri;
        private RelayClient relayClient;
        private SecretKey secretKey;
        private long order;
        private InetSocketAddress address;
        private String token;

        public RelayTransport(AddressUri addressUri, RelayClient relayClient, long order) {
            this.addressUri = addressUri;
            this.relayClient = relayClient;
            this.order = -order;
            address = new InetSocketAddress(addressUri.getHost(), addressUri.getPort());
            token = addressUri.getParams().get("token");
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
        public AddressUri addressUri() {
            return addressUri;
        }

        @Override
        public void sendPingOneWay(String tranId) {
            relayClient.sendPingOneWay(token, tranId);
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                relayClient.transfer(vip, address, token, encodeData);
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
