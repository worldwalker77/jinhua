package cn.worldwalker.game.jinhua.web.game;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.service.game.GameService;

@Controller
@RequestMapping("game/")
public class GameController {
	
	@Autowired
	private GameService gameService;
	
	@RequestMapping("login")
	@ResponseBody
	public Result login(String token, String deviceType, HttpServletResponse response){
		response.addHeader("Access-Control-Allow-Origin", "*");
		return gameService.login(token, deviceType);
	}
	
	@RequestMapping("getIpByRoomId")
	@ResponseBody
	public Result getIpByRoomId(String token, Long roomId, HttpServletResponse response){
		response.addHeader("Access-Control-Allow-Origin", "*");
		return gameService.getIpByRoomId(token, roomId);
		
	}
	
}
