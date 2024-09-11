package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.LongAttr;
import io.jaspercloud.sdwan.stun.StunPacket;
import io.jaspercloud.sdwan.support.*;
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

    private long electionTimeout = 1000;
    private String tenantId;
    private P2pClient p2pClient;
    private RelayClient relayClient;
    private KeyPair encryptionKeyPair;

    public ElectionProtocol(String tenantId, P2pClient p2pClient, RelayClient relayClient, KeyPair encryptionKeyPair) {
        this.tenantId = tenantId;
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
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, electionTimeout);
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, electionTimeout);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        BlockingQueue<DataTransport> queue = new LinkedBlockingQueue<>();
        pingRequestList.forEach(e -> {
            e.execute().thenAccept(pingResp -> {
                AddressUri uri = e.getAddressUri();
                log.info("offer pong: uri={}", uri.toString());
                if (AddressType.RELAY.equals(uri.getScheme())) {
                    String token = uri.getParams().get("token");
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new RelayTransport(uri.getScheme(), relayClient, token, order);
                    queue.add(dataTransport);
                } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                    InetSocketAddress sender = pingResp.sender();
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new P2pTransport(uri.getScheme(), p2pClient, sender, order);
                    queue.add(dataTransport);
                } else {
                    throw new UnsupportedOperationException();
                }
            });
        });
        SDWanProtos.P2pOffer p2pOfferReq = SDWanProtos.P2pOffer.newBuilder()
                .setTenantId(tenantId)
                .setSrcVIP(getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .addAllAddressUri(getLocalAddressUriList())
                .setPublicKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                .build();
        return sendOffer(p2pOfferReq, 3 * electionTimeout)
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
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, electionTimeout);
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, electionTimeout);
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        CompletableFuture<List<DataTransport>> future = AsyncTask.create(electionTimeout);
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
                    log.info("answer pong: uri={}", uri.toString());
                    if (AddressType.RELAY.equals(uri.getScheme())) {
                        String token = uri.getParams().get("token");
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new RelayTransport(uri.getScheme(), relayClient, token, order);
                        countBarrier.add(dataTransport);
                    } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                        InetSocketAddress sender = pingResp.sender();
                        long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                        DataTransport dataTransport = new P2pTransport(uri.getScheme(), p2pClient, sender, order);
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
                DataTransport transport = selectDataTransport(transportList);
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
        log.info("selectDataTransport type: {}", transport.type());
        return transport;
    }

    private PingRequest parseP2pPing(AddressUri uri, long timeout) {
        log.info("pingP2p uri: {}", uri.toString());
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> p2pClient.ping(addr, timeout));
        pingRequest.setAddressUri(uri);
        return pingRequest;
    }

    private PingRequest parseRelayPing(AddressUri uri, long timeout) {
        log.info("pingRelay uri: {}", uri.toString());
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

        private String type;
        private P2pClient p2pClient;
        private InetSocketAddress address;
        private SecretKey secretKey;
        private long order;

        public P2pTransport(String type, P2pClient p2pClient, InetSocketAddress address, long order) {
            this.type = type;
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
        public String type() {
            return type;
        }

        @Override
        public void ping(long timeout) throws Exception {
            p2pClient.ping(address, timeout).get();
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                GlobalTime.log("encryptAES");
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

        private String type;
        private RelayClient relayClient;
        private String token;
        private SecretKey secretKey;
        private long order;

        public RelayTransport(String type, RelayClient relayClient, String token, long order) {
            this.type = type;
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
        public String type() {
            return type;
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
