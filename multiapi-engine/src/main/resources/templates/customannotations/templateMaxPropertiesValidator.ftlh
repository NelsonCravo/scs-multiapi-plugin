package ${packageModel}.customvalidator;

import java.util.Map;
import java.util.Objects;

import ${javaEEPackage}.validation.ConstraintValidator;
import ${javaEEPackage}.validation.ConstraintValidatorContext;

public class MaxPropertiesValidator implements ConstraintValidator<MaxProperties, Map<String, ?>> {

  private int maximum;

  @Override
  public void initialize(MaxProperties constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    this.maximum = constraintAnnotation.maximum();
  }

  @Override
  public boolean isValid(Map<String, ?> value, ConstraintValidatorContext context) {
    return Objects.isNull(value) || value.size() <= this.maximum;
  }
}
