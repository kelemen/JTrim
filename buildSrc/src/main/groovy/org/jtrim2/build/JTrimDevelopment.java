package org.jtrim2.build;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.gradle.api.Action;

public final class JTrimDevelopment {
    private String url;
    private String scmUrl;
    private final JTrimDevelopers developers;

    public JTrimDevelopment() {
        this.developers = new JTrimDevelopers();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }

    public JTrimDevelopers getDevelopers() {
        return developers;
    }

    public void developers(Action<? super JTrimDevelopers> developerConfig) {
        developerConfig.execute(developers);
    }

    public void developers(@DelegatesTo(JTrimDeveloper.class) Closure<?> developerConfig) {
        GroovyUtils.configClosure(developers, developerConfig);
    }
}
