package org.graalvm.feature;

import org.graalvm.nativeimage.hosted.Feature;

public class JNIRegistrationFeature implements Feature {

    static {
        System.out.println("init JNIRegistrationFeature");
    }

    @Override
    public String getDescription() {
        return "Enables JNIRegistrationFeature";
    }

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
    }
}
