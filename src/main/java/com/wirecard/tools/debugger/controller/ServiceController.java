package com.wirecard.tools.debugger.controller;

import com.google.gson.Gson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class ServiceController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    private static final Gson gson = new Gson();

    @RequestMapping("/${spring.data.rest.base-path}")
    public String index() {
        return "not allowed";
    }

    @GetMapping(value = "/${spring.data.rest.base-path}/service/{serviceId}/{stageId}")
    public List queryServiceById(@PathVariable String serviceId, @PathVariable String stageId) {
        JSONArray result = null;
        if (serviceId.equals("HelloController")) {
            if (stageId.equals("startMe")) {
                result = gson.fromJson("[{\"line\":1,\"name\":\"sayHello\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":2,\"name\":\"updateVar\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":3,\"name\":\"newInstance\",\"stage\":\"Operation\",\"isDebug\":false}]", JSONArray.class);
            } else if (stageId.equals("searchUserByCriteria")) {
                result = gson.fromJson("[{\"line\":1,\"name\":\"setValueUserId\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":2,\"name\":\"setValueUserName\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":3,\"name\":\"getPCCUser\",\"stage\":\"Operation\",\"isDebug\":false}]", JSONArray.class);
            }
        }
        return result;
    }

    // processFlowGenerator (pfgId) > processFlowId > [services + method] > [stages]
    @GetMapping(value = "/${spring.data.rest.base-path}/processFlowGenerator/{processFlowGeneratorId}")
    public JSONObject queryAllProcessFlow(@PathVariable String processFlowGeneratorId) {
        JSONObject result = new JSONObject();
        if (processFlowGeneratorId.equals("e700abce-7d8f-405d-be6b-7ec2bd5df971")) {
            result = gson.fromJson("{\"BODomesticBankSearchFlow\":{\"bodomesticbankservice - searchDomesticBank\":[{\"line\":1,\"name\":\"callAppendWildcardToSearchCriteriaForCode\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":2,\"name\":\"callAppendWildcardToSearchCriteriaForName\",\"stage\":\"Call Function\",\"isDebug\":false},{\"line\":3,\"name\":\"loggerAfterAppend\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":4,\"name\":\"GetCurrentPage\",\"stage\":\"Operation\",\"isDebug\":false},{\"line\":5,\"name\":\"GetDomesticBankList\",\"stage\":\"Get\",\"isDebug\":false},{\"line\":6,\"name\":\"Log\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":7,\"name\":\"GetDomesticBankAllList\",\"stage\":\"Get\",\"isDebug\":false},{\"line\":8,\"name\":\"getTotalRecord\",\"stage\":\"Operation\",\"isDebug\":false},{\"line\":9,\"name\":\"pagehelperserviceCalculateTotalPage\",\"stage\":\"Call Service\",\"isDebug\":false},{\"line\":10,\"name\":\"pagehelperserviceConvertToDataTable\",\"stage\":\"Call Service\",\"isDebug\":false}]},\"BODomesticBankEditFlow\":{\"bopendingtaskservice - getUserBranchAndDivisionCodeAndType\":[{\"line\":1,\"name\":\"GetBranchAndDivisionCodeAndType\",\"stage\":\"Call Function\",\"isDebug\":false}],\"bodomesticbankservice - getDetailDomesticBank\":[{\"line\":1,\"name\":\"GetDOMESTICBANK\",\"stage\":\"Get\",\"isDebug\":false},{\"line\":2,\"name\":\"GetFromCollection\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":3,\"name\":\"CreateMapTenant\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":4,\"name\":\"CreateMapCountry\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":5,\"name\":\"CreateMapState\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":6,\"name\":\"CreateMapCity\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":7,\"name\":\"bocachedataserviceGetTimezoneByUserId\",\"stage\":\"Call Service\",\"isDebug\":false},{\"line\":8,\"name\":\"ConvertCreatedDate\",\"stage\":\"Operation\",\"isDebug\":false},{\"line\":9,\"name\":\"ConvertUpdatedDate\",\"stage\":\"Operation\",\"isDebug\":false},{\"line\":10,\"name\":\"PutToDomesticBankDetail\",\"stage\":\"Collection\",\"isDebug\":false}],\"bopendingtaskservice - getMenuNameByMenuCode\":[{\"line\":1,\"name\":\"getMenuNameByCode\",\"stage\":\"CallCollection\",\"isDebug\":false}],\"bousermanagementservice - getUserChannelTenantByChannelAndTenantList\":[{\"line\":1,\"name\":\"checkTenantIsNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":2,\"name\":\"LogIfTenantListNull\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":3,\"name\":\"getChannelTenantByChannel\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":4,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":5,\"name\":\"elseIfChannelIsNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":6,\"name\":\"LogIfChannelNull\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":7,\"name\":\"loopChannelTenantList\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":8,\"name\":\"setToLocal\",\"stage\":\"Set Value\",\"isDebug\":false},{\"line\":9,\"name\":\"tenantContains\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":10,\"name\":\"addChannelTenantIdToReturn\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":11,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":12,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":13,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":14,\"name\":\"elseIfBothNotEmpty\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":15,\"name\":\"LogIfBothNotEmpty\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":16,\"name\":\"loopTenantCodeList\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":17,\"name\":\"setTennatMap\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":18,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":19,\"name\":\"loopChanneltenantListSession\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":20,\"name\":\"checkChannel\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":21,\"name\":\"getTenantCode\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":22,\"name\":\"checkIfTenantisNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":23,\"name\":\"setChannelTenantIdToList\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":24,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":25,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":26,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":27,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":28,\"name\":\"else\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":29,\"name\":\"LogIfBothEmpty\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":30,\"name\":\"setToReturnList\",\"stage\":\"Set Value\",\"isDebug\":false},{\"line\":31,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false}]},\"BODomesticBankAddFlow\":{\"bopendingtaskservice - getUserBranchAndDivisionCodeAndType\":[{\"line\":1,\"name\":\"GetBranchAndDivisionCodeAndType\",\"stage\":\"Call Function\",\"isDebug\":false}],\"bopendingtaskservice - getMenuNameByMenuCode\":[{\"line\":1,\"name\":\"getMenuNameByCode\",\"stage\":\"Collection\",\"isDebug\":false}],\"bousermanagementservice - getUserChannelTenantByChannelAndTenantList\":[{\"line\":1,\"name\":\"checkTenantIsNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":2,\"name\":\"LogIfTenantListNull\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":3,\"name\":\"getChannelTenantByChannel\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":4,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":5,\"name\":\"elseIfChannelIsNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":6,\"name\":\"LogIfChannelNull\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":7,\"name\":\"loopChannelTenantList\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":8,\"name\":\"setToLocal\",\"stage\":\"Set Value\",\"isDebug\":false},{\"line\":9,\"name\":\"tenantContains\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":10,\"name\":\"addChannelTenantIdToReturn\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":11,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":12,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":13,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":14,\"name\":\"elseIfBothNotEmpty\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":15,\"name\":\"LogIfBothNotEmpty\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":16,\"name\":\"loopTenantCodeList\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":17,\"name\":\"setTennatMap\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":18,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":19,\"name\":\"loopChanneltenantListSession\",\"stage\":\"Looping\",\"isDebug\":false},{\"line\":20,\"name\":\"checkChannel\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":21,\"name\":\"getTenantCode\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":22,\"name\":\"checkIfTenantisNull\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":23,\"name\":\"setChannelTenantIdToList\",\"stage\":\"Collection\",\"isDebug\":false},{\"line\":24,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":25,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":26,\"name\":\"end looping\",\"stage\":\"End Looping\",\"isDebug\":false},{\"line\":27,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false},{\"line\":28,\"name\":\"else\",\"stage\":\"Condition\",\"isDebug\":false},{\"line\":29,\"name\":\"LogIfBothEmpty\",\"stage\":\"Logger\",\"isDebug\":false},{\"line\":30,\"name\":\"setToReturnList\",\"stage\":\"Set Value\",\"isDebug\":false},{\"line\":31,\"name\":\"end condition\",\"stage\":\"End Condition\",\"isDebug\":false}]}}", JSONObject.class);
        }
        return result;
    }
}
