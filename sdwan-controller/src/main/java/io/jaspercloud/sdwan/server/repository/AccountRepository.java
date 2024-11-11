package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.enums.UserRole;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.AccountMapper;
import io.jaspercloud.sdwan.server.repository.po.AccountPO;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository extends BaseRepository<Account, AccountPO, AccountMapper> {

    @Override
    protected BeanTransformer.Builder<Account, AccountPO> transformerBuilder() {
        return super.transformerBuilder()
                .addFieldMapping(Account::getRole, AccountPO::getRole, e -> {
                    return e.name();
                }, e -> {
                    return UserRole.valueOf(e);
                });
    }
}
