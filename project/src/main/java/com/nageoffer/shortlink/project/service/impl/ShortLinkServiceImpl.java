package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValidDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolKit.HashUtil;
import com.nageoffer.shortlink.project.toolKit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.nageoffer.shortlink.project.toolKit.LinkUtil.getLinkCacheValidTime;

/**
 * 短链接节后实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;

    @Value("${shortlink.domain.default}")
    private String createShortLinkDefaultDomain;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shorLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = createShortLinkDefaultDomain + "/" + shorLinkSuffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .shortUri(shorLinkSuffix)
                .fullShortUrl(fullShortUrl)
                .originUrl(requestParam.getOriginUrl())
                .clickNum(0)
                .gid(requestParam.getGid())
                .enableStatus(0)
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDateType() == ValidDateTypeEnum.PERMANENT.getType()?
                        null : requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .build();

        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException e) {
            log.warn("ShortLinkServiceImpl.createShortLink Occur DuplicateKeyException, Try Again! requestParam: {}", fullShortUrl, e);
            throw new ServiceException("短链接重复生成");
        }
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                getLinkCacheValidTime(requestParam.getValidDate()),
                TimeUnit.MILLISECONDS
        );
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPge = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPge.convert(each -> {
            ShortLinkPageRespDTO respDTO = BeanUtil.copyProperties(each, ShortLinkPageRespDTO.class);
            respDTO.setDomain("http://" + respDTO.getDomain());
            return respDTO;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid", "count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDoList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDoList, ShortLinkGroupCountQueryRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {

        //如果要修改gid，并且修改后的gid分库后不在同一个库
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接不存在，无法修改");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if(Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValidDateTypeEnum.PERMANENT.getType()) ,
                            ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, updateWrapper);
        } else{
            LambdaUpdateWrapper<ShortLinkDO> deleteWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(deleteWrapper);
            baseMapper.insert(shortLinkDO);
        }
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(port -> !(Objects.equals(port, 80) || Objects.equals(port, 443)))
                .map(String::valueOf)
                .orElse("");
        String fullShortUrl = serverName + ":"+serverPort+"/" + shortUri;
        String originUrlLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(originUrlLink)){
            shortLinkStats(fullShortUrl, null, request, response);
            ((HttpServletResponse) response).sendRedirect(originUrlLink);
            return;
        }

        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if(!contains){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //如果contains为true，有可能是误判，进一步操作
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(gotoIsNullShortLink)){
            //如果缓存中存在该key，说明该短链接对应的原始链接不存在于数据库，直接返回
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            // 再次查询缓存，避免缓存穿击穿,双重检测  缓存击穿：大量并发请求查询一个数据库中不存在的数据，导致大量请求直接打到数据库上，造成数据库压力过大，甚至宕机。
            originUrlLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originUrlLink)) {
                shortLinkStats(fullShortUrl, null, request, response);
                ((HttpServletResponse) response).sendRedirect(originUrlLink);
                return;
            }

            if(StrUtil.isNotBlank(
                    stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl)))){
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            LambdaQueryWrapper<ShortLinkGotoDO> LinkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(LinkGotoQueryWrapper);
            if (shortLinkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30 , TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if(shortLinkDO == null ||
                    (shortLinkDO.getValidDate() != null && (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())))){
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30 , TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;

            }

            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS
            );
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), request, response);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }

    private void shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();

        try {

            Runnable addResponseCookieTask = () -> {
                String uv = UUID.fastUUID().toString();
                Cookie uvCookie = new Cookie("uv", uv);
                uvCookie.setMaxAge(60*60*24*30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
                ((HttpServletResponse) response).addCookie(uvCookie);
                uvFirstFlag.set(true);
                stringRedisTemplate.opsForSet().add("short-link:stats:uv"+fullShortUrl, uv);
            };

            if(ArrayUtil.isNotEmpty(cookies)) {
                Arrays.stream(cookies)
                        .filter(each ->Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each ->{
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv"+fullShortUrl, each);
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0);
                        }, addResponseCookieTask);

            } else {
                addResponseCookieTask.run();
            }

            String remoteAddr = LinkUtil.getActualIp((HttpServletRequest) request);
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip"+fullShortUrl, remoteAddr);
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0;

            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> LinkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(LinkGotoQueryWrapper);
                if(shortLinkGotoDO != null){
                    gid = shortLinkGotoDO.getGid();
                }
            }
            int hourOfDay = LocalTime.now().getHour();
            int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .pv(1)
                    .uv(uvFirstFlag.get() ? 1 : 0)
                    .uip(uipFirstFlag? 1 : 0)
                    .hour(hourOfDay)
                    .weekday(dayOfWeek)
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
        } catch (Exception e) {
            log.error("短链接访问量统计异常，",e);
        }
    }

    private  String generateSuffix(ShortLinkCreateReqDTO requestParam) {

        int customGenerateCount = 0;
        String shortUri;
        // 生成短链后缀，避免冲突
        while (true) {
            if(customGenerateCount >10){
                throw new ServiceException("短链接生成失败，请稍后重试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }
}
