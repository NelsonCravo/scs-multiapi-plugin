package com.sngular.scsplugin.customValidator.model.event.consumer;

import com.sngular.scsplugin.customValidator.model.event.StatusMsgDTO;

public interface ICustomValidatorResponse {

  void customValidatorResponse(final StatusMsgDTO value);
}