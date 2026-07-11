package com.jingxuan.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类 — 将 MP Page 转换为 PageResult
 */
public class PageUtil {

    private PageUtil() {}

    /**
     * 直接转换 Page 对象
     */
    public static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    /**
     * 转换并映射 VO
     */
    public static <T, R> PageResult<R> toPageResult(Page<T> page, Function<T, R> mapper) {
        List<R> records = page.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new PageResult<>(
                records,
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    /**
     * 一步完成分页查询 + 转换 PageResult
     * <p>消除 Service 层重复的 {@code new Page<>() + selectPage() + PageResult.of()} 模板代码。</p>
     *
     * @param pageNum          页码
     * @param pageSize         每页条数
     * @param mapper           MyBatis-Plus BaseMapper
     * @param wrapperConsumer  用于构建 LambdaQueryWrapper 的 Consumer（添加查询条件）
     * @param <T>              实体类型
     * @return 分页结果
     */
    public static <T> PageResult<T> query(int pageNum, int pageSize,
                                           com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                           Consumer<LambdaQueryWrapper<T>> wrapperConsumer) {
        Page<T> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<T> wrapper = Wrappers.lambdaQuery();
        wrapperConsumer.accept(wrapper);
        Page<T> result = mapper.selectPage(page, wrapper);
        return toPageResult(result);
    }
}
