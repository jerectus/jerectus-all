package jerectus.validation;

import java.lang.annotation.Annotation;

import jerectus.util.BeanProperty;

public interface ValidationContext {
    Annotation constraint();

    Object bean();

    BeanProperty property();

    Object value();
}