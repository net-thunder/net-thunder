package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.AttrType;
import io.jaspercloud.sdwan.stun.LongAttr;
import io.jaspercloud.sdwan.stun.StunPacket;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Ecdh;
import io.jaspercloud.sdwan.util.AddressType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public abstract class ElectionProtocol {

    private Config config;
    private P2pClient p2pClient;
    private RelayClient relayClient;

    public ElectionProtocol(Config config,
                            P2pClient p2pClient,
                            RelayClient relayClient) {
        this.config = config;
        this.p2pClient = p2pClient;
        this.relayClient = relayClient;
    }

    public CompletableFuture<DataTransport> processOffer(SDWanProtos.NodeInfo nodeInfo) {
        if (config.getShowElectionLog()) {
            log.info("processOffer: src={}, dst={}", getLocalVip(), nodeInfo.getVip());
        }
        List<AddressUri> uriList = nodeInfo.getAddressUriList()
                .stream()
                .map(u -> AddressUri.parse(u))
                .collect(Collectors.toList());
        List<PingRequest> pingRequestList = uriList.stream().map(uri -> {
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, config.getPingTimeout());
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, config.getPingTimeout());
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        CompletableFuture<DataTransport> future = new CompletableFuture<>();
        pingRequestList.forEach(e -> {
            e.execute().thenAccept(pingResp -> {
                AddressUri uri = e.getAddressUri();
                if (config.getShowElectionLog()) {
                    log.info("pong: uri={}", uri.toString());
                }
                if (AddressType.RELAY.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new RelayTransport(uri, relayClient, order);
                    future.complete(dataTransport);
                } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new P2pTransport(uri, p2pClient, order);
                    future.complete(dataTransport);
                } else {
                    throw new UnsupportedOperationException();
                }
            });
        });
        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.newBuilder()
                .setTenantId(config.getTenantId())
                .setSrcVIP(getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .addAllAddressUri(getLocalAddressUriList())
                .setPublicKey(ByteString.copyFrom(config.getEncryptionKeyPair().getPublic().getEncoded()))
                .build();
        //sendOffer = 3 * electionTimeout
        if (config.getShowElectionLog()) {
            log.info("sendOffer: srcVIP={}, dstVIP={}", p2pOffer.getSrcVIP(), p2pOffer.getDstVIP());
        }
        return sendOffer(p2pOffer, 3 * config.getElectionTimeout())
                .thenApply(resp -> {
                    try {
                        DataTransport transport = future.getNow(null);
                        if (null == transport) {
                            throw new ProcessException("not found transport");
                        }
                        if (config.getShowElectionLog()) {
                            log.info("selectDataTransport: {} -> {}, uri={}", p2pOffer.getSrcVIP(), p2pOffer.getDstVIP(), transport.addressUri().toString());
                        }
                        byte[] publicKey = resp.getPublicKey().toByteArray();
                        SecretKey secretKey = Ecdh.generateAESKey(config.getEncryptionKeyPair().getPrivate(), publicKey);
                        transport.setSecretKey(secretKey);
                        return transport;
                    } catch (ProcessException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<DataTransport> processAnswer(String reqId, SDWanProtos.P2pOffer p2pOffer) {
        if (config.getShowElectionLog()) {
            log.info("processAnswer: src={}, dst={}", p2pOffer.getDstVIP(), p2pOffer.getSrcVIP());
        }
        List<AddressUri> uriList = p2pOffer.getAddressUriList().stream()
                .map(u -> AddressUri.parse(u))
                .collect(Collectors.toList());
        List<PingRequest> pingRequestList = uriList.stream().map(uri -> {
            if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                return parseP2pPing(uri, config.getPingTimeout());
            } else if (AddressType.RELAY.equals(uri.getScheme())) {
                return parseRelayPing(uri, config.getPingTimeout());
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.toList());
        //wait ping resp
        CompletableFuture<DataTransport> future = AsyncTask.create(config.getElectionTimeout());
        pingRequestList.forEach(req -> {
            req.execute().thenAccept(pingResp -> {
                AddressUri uri = req.getAddressUri();
                if (config.getShowElectionLog()) {
                    log.info("pong: uri={}", uri.toString());
                }
                if (AddressType.RELAY.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new RelayTransport(uri, relayClient, order);
                    future.complete(dataTransport);
                } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                    long order = System.currentTimeMillis() - ((LongAttr) pingResp.content().getAttr(AttrType.Time)).getData();
                    DataTransport dataTransport = new P2pTransport(uri, p2pClient, order);
                    future.complete(dataTransport);
                } else {
                    throw new UnsupportedOperationException();
                }
            });
        });
        return future.thenApply(transport -> {
            try {
                if (config.getShowElectionLog()) {
                    log.info("selectDataTransport: {} -> {}, uri={}", p2pOffer.getDstVIP(), p2pOffer.getSrcVIP(), transport.addressUri().toString());
                }
                byte[] publicKey = p2pOffer.getPublicKey().toByteArray();
                SecretKey secretKey = Ecdh.generateAESKey(config.getEncryptionKeyPair().getPrivate(), publicKey);
                transport.setSecretKey(secretKey);
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setTenantId(config.getTenantId())
                        .setCode(SDWanProtos.MessageCode.Success)
                        .setSrcVIP(p2pOffer.getDstVIP())
                        .setDstVIP(p2pOffer.getSrcVIP())
                        .setPublicKey(ByteString.copyFrom(config.getEncryptionKeyPair().getPublic().getEncoded()))
                        .build();
                if (config.getShowElectionLog()) {
                    log.info("sendAnswer: srcVIP={}, dstVIP={}, code={}", p2pAnswer.getSrcVIP(), p2pAnswer.getDstVIP(), p2pAnswer.getCode());
                }
                sendAnswer(reqId, p2pAnswer);
                return transport;
            } catch (Exception e) {
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                        .setTenantId(config.getTenantId())
                        .setCode(SDWanProtos.MessageCode.SysError)
                        .build();
                if (config.getShowElectionLog()) {
                    log.info("sendAnswer: srcVIP={}, dstVIP={}, code={}", p2pAnswer.getSrcVIP(), p2pAnswer.getDstVIP(), p2pAnswer.getCode());
                }
                sendAnswer(reqId, p2pAnswer);
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    private PingRequest parseP2pPing(AddressUri uri, long timeout) {
        if (config.getShowElectionLog()) {
            log.info("ping uri: {}", uri.toString());
        }
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> p2pClient.ping(addr, timeout));
        pingRequest.setAddressUri(uri);
        return pingRequest;
    }

    private PingRequest parseRelayPing(AddressUri uri, long timeout) {
        if (config.getShowElectionLog()) {
            log.info("ping uri: {}", uri.toString());
        }
        InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
        String token = uri.getParams().get("token");
        PingRequest pingRequest = new PingRequest();
        pingRequest.setSupplier(() -> relayClient.ping(socketAddress, token, timeout));
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
                p2pClient.transfer(vip, addressUri.getScheme(), address, encodeData);
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
            relayClient.sendPingOneWay(address, token, tranId);
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                relayClient.transfer(vip, addressUri.getScheme(), address, token, encodeData);
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

    @Getter
    @Setter
    public static class Config {

        private KeyPair encryptionKeyPair;
        private String tenantId;
        private long electionTimeout;
        private long pingTimeout;
        private Boolean showElectionLog = false;
    }
}
