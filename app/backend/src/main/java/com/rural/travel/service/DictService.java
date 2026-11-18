package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.entity.Category;
import com.rural.travel.entity.Tag;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.CategoryMapper;
import com.rural.travel.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 分类与标签字典 */
@Service
public class DictService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public DictService(CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    public List<Category> listCategories() {
        return categoryMapper.selectList(new QueryWrapper<Category>()
                .eq("status", 1).orderByAsc("sort_order"));
    }

    public List<Tag> listTags() {
        return tagMapper.selectList(new QueryWrapper<Tag>()
                .eq("status", 1).orderByAsc("sort_order"));
    }

    public List<Category> allCategories() {
        return categoryMapper.selectList(new QueryWrapper<Category>().orderByAsc("sort_order"));
    }

    public List<Tag> allTags() {
        return tagMapper.selectList(new QueryWrapper<Tag>().orderByAsc("sort_order"));
    }

    public Category saveCategory(Category c) {
        if (c.getName() == null || c.getName().isBlank()) throw new BizException("请输入分类名称");
        if (c.getSortOrder() == null) c.setSortOrder(0);
        if (c.getStatus() == null) c.setStatus(1);
        if (c.getId() == null) categoryMapper.insert(c); else categoryMapper.updateById(c);
        return c;
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    public Tag saveTag(Tag t) {
        if (t.getName() == null || t.getName().isBlank()) throw new BizException("请输入标签名称");
        if (t.getSortOrder() == null) t.setSortOrder(0);
        if (t.getStatus() == null) t.setStatus(1);
        if (t.getId() == null) tagMapper.insert(t); else tagMapper.updateById(t);
        return t;
    }

    public void deleteTag(Long id) {
        tagMapper.deleteById(id);
    }

    public PageResult<Category> pageCategories(long current, long size, String keyword) {
        Page<Category> p = categoryMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Category>()
                        .like(keyword != null && !keyword.isBlank(), "name", keyword)
                        .orderByAsc("sort_order"));
        return PageResult.of(p);
    }

    public PageResult<Tag> pageTags(long current, long size, String keyword) {
        Page<Tag> p = tagMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Tag>()
                        .like(keyword != null && !keyword.isBlank(), "name", keyword)
                        .orderByAsc("sort_order"));
        return PageResult.of(p);
    }
}
