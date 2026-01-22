package ${packageModel}.customValidator;

import ${javaEEPackage}.validation.Constraint;
import ${javaEEPackage}.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinPropertiesValidator.class)
@Documented
public @interface MinProperties {
  int minimum();
  String message() default "Object has fewer properties than required.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
