package hudson.model;

import jakarta.annotation.PostConstruct;

/**
 * Marker interface for Descriptors which use xml persistent data, and as such need to load from disk when instantiated.
 * <p>
 * {@link Descriptor#load()} method is annotated as {@link PostConstruct} so it get automatically invoked after
 * constructor and field injection.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @since 2.140
 */
public interface PersistentDescriptor extends Saveable {

    @PostConstruct
    void load();
}
