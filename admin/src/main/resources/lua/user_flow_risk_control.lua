-- 设置用户访问频率限制的参数
local username = KEYS[1]
local timeWindow = tonumber(ARGV[1]) -- 时间窗口，单位：秒

-- 构造 Redis 中存储用户访问次数的键名
local accessKey = "short-link:user-flow-risk-control:" .. username

-- 原子递增访问次数，并获取递增后的值
local currentAccessCount = redis.call("INCR", accessKey)

-- 仅当键存在且没有设置过期时间时才设置过期，否则可能会一直给这个键设置过期时间，统计访问次数全加到一起，无法实现时间窗口中的访问限制
local ttl = redis.call("TTL", accessKey)
if ttl == -1 then
  redis.call("EXPIRE", accessKey, timeWindow)
end

-- 返回当前访问次数
return currentAccessCount