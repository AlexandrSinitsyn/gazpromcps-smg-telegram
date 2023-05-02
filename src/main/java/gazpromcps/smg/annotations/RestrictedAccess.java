package gazpromcps.smg.annotations;

import gazpromcps.smg.entity.Role;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictedAccess {
    Role minAllowed();
}
