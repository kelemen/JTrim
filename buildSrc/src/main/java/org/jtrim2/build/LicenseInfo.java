package org.jtrim2.build;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class LicenseInfo {
    private final Property<String> name;
    private final Property<String> url;

    @Inject
    public LicenseInfo(ObjectFactory objects) {
        this.name = objects.property(String.class);
        this.url = objects.property(String.class);
    }

    public Property<String> getName() {
        return name;
    }

    public Property<String> getUrl() {
        return url;
    }
}
