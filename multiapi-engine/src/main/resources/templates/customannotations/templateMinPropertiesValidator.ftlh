package ${packageModel}.customvalidator;

import java.util.Map;
import java.util.Objects;

import ${javaEEPackage}.validation.ConstraintValidator;
import ${javaEEPackage}.validation.ConstraintValidatorContext;

public class MinPropertiesValidator implements ConstraintValidator<MinProperties, Map<String, ?>> {

  private int minimum;

  @Override
  public void initialize(MinProperties constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    this.minimum = constraintAnnotation.minimum();
  }

  @Override
  public boolean isValid(Map<String, ?> value, ConstraintValidatorContext context) {
    return Objects.isNull(value) || value.size() >= this.minimum;
  }
}
