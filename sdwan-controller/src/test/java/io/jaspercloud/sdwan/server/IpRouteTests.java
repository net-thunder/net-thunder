package io.jaspercloud.sdwan.server;

import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.entity.dto.IpRouteTest;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IpRouteTests {

    @Test
    public void test() {
        String tenantJson = "{\"name\":\"default\",\"description\":\"\",\"code\":\"default\",\"cidr\":\"10.8.0.0/24\",\"stunServerList\":[\"192.222.0.150:3478\"],\"relayServerList\":[\"192.222.0.150:2478\"],\"enable\":true,\"nodeGrant\":false,\"ipIndex\":3,\"accountId\":201,\"id\":101}";
//        String detailJson = "{\"id\":101,\"name\":\"8bicZmHv\",\"mac\":\"fa:2c:49:58:a9:00\",\"os\":\"windows\",\"osVersion\":\"Windows 10\",\"nodeVersion\":\"1.0.4\",\"mesh\":false,\"vip\":\"10.8.0.3\",\"groupList\":[{\"name\":\"default\",\"defaultGroup\":true,\"routeIdList\":[1],\"routeRuleIdList\":[101,401,402],\"vnatIdList\":[],\"id\":101,\"tenantId\":101}],\"routeList\":[{\"name\":\"test\",\"description\":\"\",\"destination\":\"192.168.1.0/16\",\"enable\":true,\"id\":1,\"tenantId\":101}],\"routeRuleList\":[{\"name\":\"reject1\",\"description\":\"\",\"strategy\":\"Reject\",\"direction\":\"Input\",\"ruleList\":[\"10.8.0.3\"],\"level\":1,\"enable\":true,\"id\":402,\"tenantId\":101},{\"name\":\"default\",\"strategy\":\"Allow\",\"direction\":\"All\",\"ruleList\":[\"0.0.0.0/0\"],\"level\":100,\"enable\":true,\"id\":101,\"tenantId\":101}],\"vnatList\":[],\"nodeList\":[{\"name\":\"8bicZmHv\",\"mac\":\"fa:2c:49:58:a9:00\",\"vip\":\"10.8.0.3\",\"os\":\"windows\",\"osVersion\":\"Windows 10\",\"nodeVersion\":\"1.0.4\",\"mesh\":false,\"enable\":true,\"id\":101,\"tenantId\":101},{\"name\":\"mesh\",\"description\":\"\",\"mac\":\"42:ac:bd:00:00:00\",\"vip\":\"10.8.0.2\",\"os\":\"linux\",\"osVersion\":\"Linux\",\"nodeVersion\":\"1.0.4\",\"mesh\":true,\"enable\":true,\"id\":102,\"tenantId\":101}],\"enable\":true,\"tenantId\":101}";
        String detailJson = "{\"id\":101,\"name\":\"8bicZmHv\",\"mac\":\"fa:2c:49:58:a9:00\",\"os\":\"windows\",\"osVersion\":\"Windows 10\",\"nodeVersion\":\"1.0.4\",\"mesh\":false,\"vip\":\"10.8.0.3\",\"groupList\":[{\"name\":\"default\",\"defaultGroup\":true,\"routeIdList\":[1],\"routeRuleIdList\":[101,401,402],\"vnatIdList\":[],\"id\":101,\"tenantId\":101}],\"routeList\":[{\"name\":\"test\",\"description\":\"\",\"destination\":\"192.168.1.0/16\",\"enable\":true,\"id\":1,\"tenantId\":101}],\"routeRuleList\":[{\"name\":\"default\",\"strategy\":\"Allow\",\"direction\":\"All\",\"ruleList\":[\"0.0.0.0/0\"],\"level\":100,\"enable\":true,\"id\":101,\"tenantId\":101}],\"vnatList\":[],\"nodeList\":[{\"name\":\"8bicZmHv\",\"mac\":\"fa:2c:49:58:a9:00\",\"vip\":\"10.8.0.3\",\"os\":\"windows\",\"osVersion\":\"Windows 10\",\"nodeVersion\":\"1.0.4\",\"mesh\":false,\"enable\":true,\"id\":101,\"tenantId\":101},{\"name\":\"mesh\",\"description\":\"\",\"mac\":\"42:ac:bd:00:00:00\",\"vip\":\"10.8.0.2\",\"os\":\"linux\",\"osVersion\":\"Linux\",\"nodeVersion\":\"1.0.4\",\"mesh\":true,\"enable\":true,\"id\":102,\"tenantId\":101}],\"enable\":true,\"tenantId\":101}";
        Tenant tenant = JSONUtil.parseObj(tenantJson).toBean(Tenant.class);
        NodeDetailResponse detail = JSONUtil.parseObj(detailJson).toBean(NodeDetailResponse.class);
        IpRouteTest ipRouteTest = new IpRouteTest();
        ipRouteTest.setSrcIp("10.8.0.3");
        ipRouteTest.setDstIp("192.168.14.7");
        ipRouteTest.test(tenant, detail);
        List<IpRouteTest.Message> logList = ipRouteTest.getLogList();
        System.out.println();
    }
}
