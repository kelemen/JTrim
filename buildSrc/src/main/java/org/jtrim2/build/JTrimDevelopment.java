package org.jtrim2.build;

import javax.inject.Inject;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class JTrimDevelopment {
    private final Property<String> url;
    private final Property<String> scmUrl;
    private final NamedDomainObjectContainer<JTrimDeveloper> developers;

    @Inject
    public JTrimDevelopment(ObjectFactory objects) {
        this.url = objects.property(String.class);
        this.scmUrl = objects.property(String.class);
        this.developers = objects.domainObjectContainer(JTrimDeveloper.class, name -> new JTrimDeveloper(name, objects));
    }

    public Property<String> getUrl() {
        return url;
    }

    public Property<String> getScmUrl() {
        return scmUrl;
    }

    public NamedDomainObjectContainer<JTrimDeveloper> getDevelopers() {
        return developers;
    }
}
