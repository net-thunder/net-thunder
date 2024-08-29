package io.jaspercloud.sdwan.platform.rpc;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HessianCodec<T> {

    private Class<T> clazz;

    public HessianCodec(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <T> HessianCodec<T> codec(Class<T> clazz) {
        return new HessianCodec<>(clazz);
    }

    public byte[] encode(T obj) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Hessian2Output output = new Hessian2Output(stream);
        output.writeObject(obj);
        output.flush();
        byte[] bytes = stream.toByteArray();
        return bytes;
    }

    public T decode(byte[] bytes) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        Hessian2Input input = new Hessian2Input(stream);
        T object = (T) input.readObject(clazz);
        return object;
    }
}
