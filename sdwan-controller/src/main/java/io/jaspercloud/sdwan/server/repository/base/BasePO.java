package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasePO<T extends Model<?>> extends Model<T> {

    @TableId(type = IdType.AUTO)
    private Long id;
}
