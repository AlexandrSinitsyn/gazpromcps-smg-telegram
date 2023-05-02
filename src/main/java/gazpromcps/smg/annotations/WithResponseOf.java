package gazpromcps.smg.annotations;

import gazpromcps.smg.annotations.WithResponseOf.WithResponseOfWrapper;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// @Repeatable(WithResponseOfWrapper.class)
public @interface WithResponseOf {
    String question();
    ButtonRow[] buttons();

    @Deprecated
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface WithResponseOfWrapper {
        WithResponseOf[] value() default {};
    }
}
