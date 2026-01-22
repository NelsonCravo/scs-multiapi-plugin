package ${subscribePackage};

import ${method.classNamespace}.${method.className};
<#if method.keyClassNamespace?has_content>
import ${method.keyClassNamespace}.${method.keyClassName};
</#if>
import ${subscribePackage}.MessageWrapper;

public interface I${method.className} {

  void ${method.operationId?uncap_first}(final MessageWrapper<${method.className}, ${method.keyClassName?cap_first}> value);
}
