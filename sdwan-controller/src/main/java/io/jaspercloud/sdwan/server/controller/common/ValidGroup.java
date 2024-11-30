package io.jaspercloud.sdwan.server.controller.common;

import cn.hutool.core.lang.RegexPool;

public interface ValidGroup {

    String NAME = "^[\u4E00-\u9FFF\\w]{2,16}$";
    String CODE = "^\\w{4,10}";
    String UNAME_PWD = "^\\w{6,18}";
    String MAC_ADDRESS = RegexPool.MAC_ADDRESS;
    String IPV4_ADDRESS = RegexPool.IPV4;

    interface Add {

    }

    interface Update {

    }

    interface Delete {

    }
}
