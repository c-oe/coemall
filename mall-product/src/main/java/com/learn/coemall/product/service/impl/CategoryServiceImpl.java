package com.learn.coemall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.coemall.product.dao.CategoryDao;
import com.learn.coemall.product.entity.CategoryEntity;
import com.learn.coemall.product.service.CategoryBrandRelationService;
import com.learn.coemall.product.service.CategoryService;
import com.learn.coemall.product.vo.Catelog2Vo;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {

        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子的树形结构
        //1.找到一级分类：parentId为0
        return entities.stream().filter(c -> c.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(getChildren(menu, entities));
                    return menu;
                }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()))
                .collect(Collectors.toList());
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {

        List<Long> parentPath = findParentPath(catelogId, new ArrayList<>());
        Collections.reverse(parentPath);

        return  parentPath.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     */
//    @CacheEvict 失效模式
//    @CachePut 双写模式
//    @CacheEvict(value = "category",allEntries = true)
    @Caching(evict = {
            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatalogJson'")
    })
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCascade(CategoryEntity category) {
        baseMapper.updateById(category);

        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Cacheable(value = {"category"}/*缓存分区(按照业务类型分)*/,key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {

       return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid",0));
    }

/*
  public Map<String, List<Catelog2Vo>> getCatalogJson2() {


        //空结果缓存：解决缓存穿透
        //设置过期时间（加随机值）：解决缓存雪崩
        //加锁：解决缓存击穿


        //加入缓存逻辑
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        //判断缓存中是否有数据
        if (!StringUtils.hasLength(catalogJSON)){
            //没有则查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
            //将查到的数据转为json放入缓存
            if (catalogJsonFromDb == null){
                redisTemplate.opsForValue().set("catalogJSON", String.valueOf(0));
                return null;
            }
            String s = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return catalogJsonFromDb;
        }
        return JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        RLock lock = redissonClient.getLock("CatalogJson-lock");
        lock.lock();

        //加锁成功，执行业务
        Map<String, List<Catelog2Vo>> dataFromDb = null;
        try {
            dataFromDb = getCatalogJsonFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //占分布式锁
        String uuid = UUID.randomUUID().toString();
        //设置过期时间，必须和加锁是同步的，原子的
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock){
            //加锁成功，执行业务
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            try {
                dataFromDb = getCatalogJsonFromDb();
            }finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Collections.singletonList("lock"),uuid);

            }
            return dataFromDb;
        }else {
            return getCatalogJsonFromDbWithRedisLock();
        }
    }
*/
    /**
     * 从数据库中查询并封装数据
     */
    @Cacheable(value = "category",key = "#root.methodName",sync = true)
    public Map<String, List<Catelog2Vo>> getCatalogJson(){

        //查一次数据库
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
        //封装数据
        return level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList,l2.getCatId());
                    if (level3Catalog != null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catalog.stream().map(l3 -> {
                            return new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parentCid) {

        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //收集当前节点id
        paths.add(catelogId);
        CategoryEntity id = getById(catelogId);
        if (id.getParentCid() != 0){
            findParentPath(id.getParentCid(),paths);
        }
        return paths;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        return all.stream().filter(c -> c.getParentCid().equals(root.getCatId())).map(c -> {
            c.setChildren(getChildren(c, all));
            return c;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()))
                .collect(Collectors.toList());
    }

}