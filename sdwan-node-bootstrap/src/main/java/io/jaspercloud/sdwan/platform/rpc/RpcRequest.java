package io.jaspercloud.sdwan.platform.rpc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RpcRequest implements Serializable {

    private String key;
    private String interfaceName;
    private String methodName;
    private  Class<?>[] parameterTypes;
    private Object[] parameters;
}
