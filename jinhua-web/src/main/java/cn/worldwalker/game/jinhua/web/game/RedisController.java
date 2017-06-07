package cn.worldwalker.game.jinhua.web.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.domain.result.Result;

@Controller
@RequestMapping("redis/")
public class RedisController {
	
	@Autowired
	private JedisTemplate jedisTemplate;
	
	@RequestMapping("get")
	@ResponseBody
	public Result get(String key){
		Result result = new Result();
		result.setData(jedisTemplate.get(key));
		return result;
	}
	
	@RequestMapping("hgetAll")
	@ResponseBody
	public Result hgetAll(String key){
		Result result = new Result();
		result.setData(jedisTemplate.hgetAll(key));
		return result;
	}
	
	@RequestMapping("del")
	@ResponseBody
	public Result del(String key){
		Result result = new Result();
		jedisTemplate.del(key);
		return result;
	}
}
