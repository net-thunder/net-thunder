//package org.graalvm.feature;
//
//import com.sun.jna.*;
//import com.sun.jna.ptr.IntByReference;
//import com.sun.jna.ptr.PointerByReference;
//import org.graalvm.nativeimage.hosted.Feature;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
///**
// * Feature for use at build time on GraalVM, which enables basic support for JNA.
// *
// * <p>This "Feature" implementation is discovered on the class path, via the argument file at native-image.properties.
// * At build-time, the feature is registered with the Native Image compiler.
// *
// * <p>If JNA is detected on the class path, the feature is enabled, and the JNA library is initialized and configured
// * for native access support.
// *
// * <p>Certain features like reflection and JNI access are configured by this feature; to enable static optimized
// * support for JNA, see the {@link JavaNativeAccess} feature.
// *
// * @author Sam Gammon (sam@elide.dev)
// * @author Dario Valdespino (dario@elide.dev)
// * @since 5.15.0
// */
//public final class JavaNativeAccess extends AbstractJNAFeature implements Feature {
//
//    static final String NATIVE_LAYOUT = "com.sun.jna.Native";
//
//    static {
//        System.out.println("init JavaNativeAccess");
//    }
//
//    @Override
//    public String getDescription() {
//        return "Enables access to JNA at runtime on JavaNativeAccess";
//    }
//
//    @Override
//    public boolean isInConfiguration(IsInConfigurationAccess access) {
//        return access.findClassByName(NATIVE_LAYOUT) != null;
//    }
//
//    private void registerCommonTypes() {
//        registerJniClass(Callback.class);
//        registerJniClass(CallbackReference.class);
//        registerJniMethods(
//                method(CallbackReference.class, "getCallback", Class.class, Pointer.class));
//        registerJniMethods(
//                method(CallbackReference.class, "getCallback", Class.class, Pointer.class, boolean.class));
//        registerJniMethods(
//                method(CallbackReference.class, "getFunctionPointer", Callback.class));
//        registerJniMethods(
//                method(CallbackReference.class, "getFunctionPointer", Callback.class, boolean.class));
//        registerJniMethods(
//                method(CallbackReference.class, "getNativeString", Object.class, boolean.class));
////        registerJniMethods(
////                method(CallbackReference.class, "initializeThread", Callback.class, CallbackReference.AttachOptions.class));
//
////        registerJniClass(com.sun.jna.CallbackReference.AttachOptions.class);
//
//        registerJniClass(FromNativeConverter.class);
//        registerJniMethods(method(FromNativeConverter.class, "nativeType"));
//
//        registerJniClass(IntegerType.class);
//        registerJniFields(fields(IntegerType.class, "value"));
//
//        registerJniClass(Native.class);
//        registerJniMethods(
//                method(Native.class, "dispose"),
//                method(Native.class, "fromNative", FromNativeConverter.class, Object.class, Method.class),
//                method(Native.class, "fromNative", Class.class, Object.class),
//                method(Native.class, "nativeType", Class.class),
//                method(Native.class, "toNative", ToNativeConverter.class, Object.class),
//                method(Native.class, "open", String.class, int.class),
//                method(Native.class, "close", long.class),
//                method(Native.class, "findSymbol", long.class, String.class));
//
//        registerJniClass(Native.ffi_callback.class);
//        registerJniMethods(method(Native.ffi_callback.class, "invoke", long.class, long.class, long.class));
//
//        registerJniClass(NativeLong.class);
//
//        registerJniClass(NativeMapped.class);
//        registerJniMethods(method(NativeMapped.class, "toNative"));
//
//        registerJniClass(Pointer.class);
//        registerJniFields(fields(Pointer.class, "peer"));
//
//        registerJniClass(PointerType.class);
//        registerJniFields(fields(PointerType.class, "pointer"));
//
//        registerJniClass(Structure.class);
//        registerJniFields(fields(Structure.class, "memory", "typeInfo"));
//        registerJniMethods(
//                method(Structure.class, "autoRead"),
//                method(Structure.class, "autoWrite"),
//                method(Structure.class, "getTypeInfo"),
//                method(Structure.class, "getTypeInfo", Object.class),
//                method(Structure.class, "newInstance", Class.class),
//                method(Structure.class, "newInstance", Class.class, long.class),
//                method(Structure.class, "newInstance", Class.class, Pointer.class));
//
//        registerJniClass(Structure.ByValue.class);
////        registerJniClass(Structure.FFIType.class);
//        registerJniClass(WString.class);
//        registerJniClass(PointerByReference.class);
//    }
//
//    private void registerCommonProxies() {
//        registerProxyInterfaces(Callback.class);
//        registerProxyInterfaces(Library.class);
//    }
//
//    private void registerReflectiveAccess() {
//        reflectiveClass(
//                CallbackProxy.class,
//                CallbackReference.class,
////                Klass.class,
//                NativeLong.class,
//                Structure.class,
//                IntByReference.class,
//                PointerByReference.class);
//    }
//
//    @Override
//    public void beforeAnalysis(BeforeAnalysisAccess access) {
//        registerCommonTypes();
//        registerCommonProxies();
//        registerReflectiveAccess();
//
//        // extending `com.sun.jna.Library` should add interfaces as proxies
//        access.registerSubtypeReachabilityHandler((duringAnalysisAccess, aClass) -> {
//            // must extend `Library`, be an interface, and not already be a proxy
//            assert aClass.isInterface();
//            if (Library.class.isAssignableFrom(aClass) && !Proxy.isProxyClass(aClass)) {
//                registerProxyInterfaces(aClass);
//            }
//        }, Library.class);
//    }
//}