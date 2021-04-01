package com.kexin.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator
        implements ConstraintValidator<ListValue,Integer> { //<注解,校验值类型>

    // 存储所有可能的值
    private Set<Integer> set=new HashSet<>();

    @Override // 初始化方法，你可以获取注解上的内容并进行处理
    public void initialize(ListValue constraintAnnotation) {
        // 获取后端写好的限制 // 这个value就是ListValue里的value，我们写的注解是@ListValue(value={0,1})
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }
    }

    /**
     * 判断是否校验成功
     * @param value  需要校验的值
     * @param context
     * @return
     */
    @Override // 覆写验证逻辑
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // 看是否在限制的值里
        return  set.contains(value);
    }
}
