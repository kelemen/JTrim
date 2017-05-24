package org.jtrim2.build;

import groovy.lang.Closure;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Action;

public final class JTrimDevelopers {
    private final List<JTrimDeveloper> developers;

    public JTrimDevelopers() {
        this.developers = new ArrayList<>();
    }

    private JTrimDeveloper addNewAndGet() {
        JTrimDeveloper result = new JTrimDeveloper();
        developers.add(result);
        return result;
    }

    public void developer(Action<? super JTrimDeveloper> developerConfig) {
        developerConfig.execute(addNewAndGet());
    }

    public void developer(Closure<?> developerConfig) {
        GroovyUtils.configClosure(addNewAndGet(), developerConfig);
    }

    public List<JTrimDeveloper> getDevelopers() {
        return developers;
    }
}
