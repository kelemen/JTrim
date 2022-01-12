package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class JTrimDeveloper implements Named {
    private final String name;
    private final Property<String> displayName;
    private final Property<String> email;

    public JTrimDeveloper(String name, ObjectFactory objects) {
        this.name = Objects.requireNonNull(name, "name");

        this.displayName = objects.property(String.class);
        this.displayName.set(name);

        this.email = objects.property(String.class);
    }

    @Override
    public String getName() {
        return name;
    }

    public Property<String> getDisplayName() {
        return displayName;
    }

    public Property<String> getEmail() {
        return email;
    }
}
