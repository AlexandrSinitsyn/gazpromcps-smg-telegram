package gazpromcps.smg.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AskOnEnd {
    String question();
    ButtonRow[] buttons();
}
