package io.jaspercloud.sdwan.platform.rpc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RpcResponse implements Serializable {

    private Object result;
    private Throwable throwable;

    public Object getResult() throws Throwable {
        if (null != throwable) {
            throw throwable;
        }
        return result;
    }
}
