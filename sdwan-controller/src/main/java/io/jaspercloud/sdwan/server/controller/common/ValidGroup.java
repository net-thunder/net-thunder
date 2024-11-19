package io.jaspercloud.sdwan.server.controller.common;

import cn.hutool.core.lang.RegexPool;

public interface ValidGroup {

    String NAME = "^\\w{4,16}$";
    String CODE = "^\\w{4,10}";
    String UNAME_PWD = "^\\w{6,18}";
    String MAC_ADDRESS = RegexPool.MAC_ADDRESS;

    interface Add {

    }

    interface Update {

    }

    interface Delete {

    }
}
