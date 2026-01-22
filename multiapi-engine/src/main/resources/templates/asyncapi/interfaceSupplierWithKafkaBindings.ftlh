package ${supplierPackage};

import ${method.classNamespace}.${method.className};
<#if method.keyClassNamespace?has_content>
import ${method.keyClassNamespace}.${method.keyClassName};
</#if>
import ${supplierPackage}.MessageWrapper;

public interface I${method.className} {

  MessageWrapper<${method.className}, ${method.keyClassName?cap_first}> ${method.operationId?uncap_first}();
}
