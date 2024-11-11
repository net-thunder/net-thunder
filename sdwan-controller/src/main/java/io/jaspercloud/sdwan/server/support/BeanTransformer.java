package io.jaspercloud.sdwan.server.support;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * bean复制
 *
 * @param <High> 上层bean
 * @param <Low>  下层bean
 */
public class BeanTransformer<High, Low> {

    private Class<High> highClass;
    private Class<Low> lowClass;
    private CopyOptions highOptions;
    private CopyOptions lowOptions;
    private BiConsumer<Low, High> highMapping;
    private BiConsumer<High, Low> lowMapping;

    private BeanTransformer(Class<High> highClass, Class<Low> lowClass,
                            CopyOptions highOptions, CopyOptions lowOptions,
                            BiConsumer<Low, High> highMapping,
                            BiConsumer<High, Low> lowMapping) {
        this.highClass = highClass;
        this.lowClass = lowClass;
        this.highOptions = highOptions;
        this.lowOptions = lowOptions;
        this.highMapping = highMapping;
        this.lowMapping = lowMapping;
    }

    public Low build(High high) {
        Low result = BeanUtil.toBean(high, lowClass, lowOptions);
        if (null != lowMapping) {
            lowMapping.accept(high, result);
        }
        return result;
    }

    public High output(Low low) {
        High result = BeanUtil.toBean(low, highClass, highOptions);
        if (null != highMapping) {
            highMapping.accept(low, result);
        }
        return result;
    }

    public static <High, Low> Builder<High, Low> builder(Class<High> highClass, Class<Low> lowClass) {
        return new Builder<>(highClass, lowClass);
    }

    public static class Builder<High, Low> {

        private Class<High> highClass;
        private Class<Low> lowClass;
        private Map<String, Mapping> highMap = new HashMap<>();
        private Map<String, Mapping> lowMap = new HashMap<>();
        private BiConsumer<Low, High> highMapping;
        private BiConsumer<High, Low> lowMapping;

        private Builder(Class<High> highClass, Class<Low> lowClass) {
            this.highClass = highClass;
            this.lowClass = lowClass;
        }

        public <H, L> Builder<High, Low> addFieldMapping(Func1<High, H> buildField, Func1<Low, L> outputField,
                                                         Function<H, L> buildMapping,
                                                         Function<L, H> outputMapping) {
            String buildFieldName = LambdaUtil.getFieldName(buildField);
            String outputFieldName = LambdaUtil.getFieldName(outputField);
            lowMap.put(outputFieldName, new Mapping(buildFieldName, outputFieldName, buildMapping, outputMapping));
            highMap.put(buildFieldName, new Mapping(outputFieldName, buildFieldName, buildMapping, outputMapping));
            return this;
        }

        public Builder<High, Low> merge(BiConsumer<Low, High> highMapping,
                                        BiConsumer<High, Low> lowMapping) {
            this.highMapping = highMapping;
            this.lowMapping = lowMapping;
            return this;
        }

        private Map<String, String> convertFieldNameMap(Map<String, Mapping> mappings) {
            Map<String, String> map = new HashMap<>();
            mappings.forEach((k, v) -> {
                map.put(v.getHighFieldName(), v.getLowFieldName());
            });
            return map;
        }

        public BeanTransformer<High, Low> build() {
            CopyOptions highOptions = CopyOptions.create()
                    .setFieldMapping(convertFieldNameMap(highMap))
                    .setConverter(null)
                    .setFieldValueEditor((f, o) -> {
                        if (null != o) {
                            Mapping mapping = highMap.get(f);
                            if (null == mapping) {
                                return o;
                            }
                            Object result = mapping.getOutputMapping().apply(o);
                            return result;
                        }
                        return o;
                    });
            CopyOptions lowOptions = CopyOptions.create()
                    .setFieldMapping(convertFieldNameMap(lowMap))
                    .setConverter(null)
                    .setFieldValueEditor((f, o) -> {
                        if (null != o) {
                            Mapping mapping = lowMap.get(f);
                            if (null == mapping) {
                                return o;
                            }
                            Object result = mapping.getBuildMapping().apply(o);
                            return result;
                        }
                        return o;
                    });
            BeanTransformer<High, Low> beanTransformer = new BeanTransformer<>(highClass, lowClass, highOptions, lowOptions, highMapping, lowMapping);
            return beanTransformer;
        }
    }

    @Getter
    @Setter
    public static class Mapping {

        private String highFieldName;
        private String lowFieldName;
        private Function buildMapping;
        private Function outputMapping;

        public Mapping(String highFieldName, String lowFieldName,
                       Function buildMapping, Function outputMapping) {
            this.highFieldName = highFieldName;
            this.lowFieldName = lowFieldName;
            this.buildMapping = buildMapping;
            this.outputMapping = outputMapping;
        }
    }
}
