package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.util.concurrent.CompletableFuture;

public interface ElectionProtocol {

    CompletableFuture<DataTransport> createOffer(SDWanProtos.NodeInfo nodeInfo);

    CompletableFuture<DataTransport> createAnswer(String reqId, SDWanProtos.P2pOffer p2pOffer);
}
