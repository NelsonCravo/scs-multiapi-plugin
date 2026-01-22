package com.sngular.multifileplugin.lombok.testapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.MaxInteger;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.MinInteger;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.MultipleOf;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.Size;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.Pattern;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.MaxItems;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.MinItems;
import com.sngular.multifileplugin.lombok.testapi.model.customValidator.UniqueItems;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
public class ApiErrorDTO {

  @JsonProperty(value ="intCode")
  private Integer intCode;

  @JsonProperty(value ="code")
  @MinInteger(minimum = "10", exclusive = false)
  @MaxInteger(maximum = "200", exclusive = true)
  @MultipleOf(multiple = "10.55")
  @NonNull
  private Integer code;

  @JsonProperty(value ="message")
  @Size(min =50, max =200)
  @Pattern(regex = "^[a-zA-Z0-9_.-]*$")
  @NonNull
  private String message;

  @JsonProperty(value ="test")
  @MaxItems(maximum = 10)
  @MinItems(minimum = 5)
  @UniqueItems
  @Singular("_test")
  private List<Integer> test;


  @Builder
  @Jacksonized
  private ApiErrorDTO(Integer intCode, @NonNull Integer code, @NonNull String message, List<Integer> test) {
    this.intCode = intCode;
    this.code = code;
    this.message = message;
    this.test = test;

  }

}
