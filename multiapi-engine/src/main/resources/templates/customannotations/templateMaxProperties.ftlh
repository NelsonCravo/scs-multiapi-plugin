package ${packageModel}.customValidator;

import ${javaEEPackage}.validation.Constraint;
import ${javaEEPackage}.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxPropertiesValidator.class)
@Documented
public @interface MaxProperties {
  int maximum();
  String message() default "Object has more properties than allowed.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
