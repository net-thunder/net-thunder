package org.graalvm.feature;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import java.security.Security;

public class BouncyCastleFeature implements Feature {

    static {
        System.out.println("init BouncyCastleFeature");
    }

    @Override
    public String getDescription() {
        return "Enables BouncyCastleFeature";
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle");
        RuntimeClassInitialization.initializeAtRunTime("org.bouncycastle.jcajce.provider.drbg.DRBG$Default");
        RuntimeClassInitialization.initializeAtRunTime("org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV");
        Security.addProvider(new BouncyCastleProvider());
    }
}
