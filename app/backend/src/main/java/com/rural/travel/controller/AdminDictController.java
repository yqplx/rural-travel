package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.entity.Category;
import com.rural.travel.entity.Tag;
import com.rural.travel.service.DictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理端-分类与标签管理 */
@RestController
@RequestMapping("/api/admin")
public class AdminDictController {

    private final DictService dictService;

    public AdminDictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/categories")
    public Result<PageResult<Category>> categories(@RequestParam(defaultValue = "1") long current,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(dictService.pageCategories(current, size, keyword));
    }

    @GetMapping("/categories/all")
    public Result<List<Category>> allCategories() {
        return Result.ok(dictService.allCategories());
    }

    @PostMapping("/categories")
    public Result<Category> saveCategory(@RequestBody Category category) {
        return Result.ok(dictService.saveCategory(category));
    }

    @PutMapping("/categories/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        return Result.ok(dictService.saveCategory(category));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        dictService.deleteCategory(id);
        return Result.ok();
    }

    @GetMapping("/tags")
    public Result<PageResult<Tag>> tags(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(dictService.pageTags(current, size, keyword));
    }

    @GetMapping("/tags/all")
    public Result<List<Tag>> allTags() {
        return Result.ok(dictService.allTags());
    }

    @PostMapping("/tags")
    public Result<Tag> saveTag(@RequestBody Tag tag) {
        return Result.ok(dictService.saveTag(tag));
    }

    @PutMapping("/tags/{id}")
    public Result<Tag> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        tag.setId(id);
        return Result.ok(dictService.saveTag(tag));
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        dictService.deleteTag(id);
        return Result.ok();
    }
}
