package cn.worldwalker.game.jinhua.web.game;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.utils.IPUtil;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.game.UserInfo;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;
import cn.worldwalker.game.jinhua.service.game.GameService;
import cn.worldwalker.game.jinhua.service.session.SessionContainer;

@Controller
@RequestMapping("game/")
public class GameController {
	
	private static final Log log = LogFactory.getLog(GameController.class);
	
	@Autowired
	private JedisTemplate jedisTemplate;
	
	@Autowired
	private GameService gameService;
	
	@RequestMapping("login")
	@ResponseBody
	public Result login(String code, String deviceType, HttpServletResponse response, HttpServletRequest request){
		response.addHeader("Access-Control-Allow-Origin", "*");
		Result result = null;
		if ("1".equals(jedisTemplate.get(Constant.jinhuaLoginFuse))) {
			result = gameService.login(code, deviceType, request);
		}else{
			result = gameService.login1(code, deviceType, request);
		}
		return result;
	}
	
	@RequestMapping("getIpByRoomId")
	@ResponseBody
	public Result getIpByRoomId(String token, Long roomId, HttpServletResponse response){
		response.addHeader("Access-Control-Allow-Origin", "*");
		return gameService.getIpByRoomId(token, roomId);
		
	}
	
	@RequestMapping("notice")
	@ResponseBody
	public Result notice(@RequestBody Msg msg){
		Result result = new Result();
		
		if (null == msg || msg.getNoticeType() == null || StringUtils.isBlank(msg.getNoticeContent())) {
			result.setCode(ResultCode.PARAM_ERROR.code);
			result.setDesc(ResultCode.PARAM_ERROR.returnDesc);
			return result;
		}
		GameRequest request = new GameRequest();
		request.setMsg(msg);
		return gameService.notice(null, request);
	}
	
	@RequestMapping(value = "/index")
    public String index(){
        return "test/testIndex";
    }
	
	@RequestMapping(value = "/upload",method = RequestMethod.POST)
	@ResponseBody
    public Result upload(@RequestParam("file") MultipartFile file, String token, Model model,HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (file == null) {
			log.info("请求, 上传语音文件， file:"+ ", token:" + token);
		}else{
			log.info("请求, 上传语音文件， file:"+ file.toString() + ", token:" + token);
		}
		
		response.addHeader("Access-Control-Allow-Origin", "*");
		Result result = new Result();
        if (file == null || StringUtils.isBlank(token)) {
        	result.setCode(ResultCode.PARAM_ERROR.code);
        	result.setDesc(ResultCode.PARAM_ERROR.returnDesc);
        	return result;
		}
        UserInfo userInfo = SessionContainer.getUserInfoFromRedis(token);
        if (userInfo == null) {
        	result.setCode(ResultCode.NEED_LOGIN.code);
        	result.setDesc(ResultCode.NEED_LOGIN.returnDesc);
        	return result;
		}
        Long playerId = userInfo.getPlayerId();
		try {
			File dir=new File(request.getSession().getServletContext().getRealPath("/uploadfiles/" + playerId));
			if(!dir.exists()){
			    dir.mkdirs();
			}
			String fileName = file.getOriginalFilename();
			String path = dir.getAbsolutePath() + "/" + fileName;
			file.transferTo(new File(path));
			result.setData("http://" + IPUtil.getLocalIp() + ":8080/uploadfiles/" + playerId + "/" + fileName);
		} catch (Exception e) {
			result.setCode(1);
			result.setDesc("系统异常");
			e.printStackTrace();
		}
		log.info("返回, 上传语音文件， result:" + JsonUtil.toJson(result));
        return result;
    }
	
	
	@RequestMapping(value = "/upload1",method = RequestMethod.POST)
	@ResponseBody
    public Result upload1(String token, Model model,HttpServletRequest request, HttpServletResponse response) throws IOException {
	
		log.info("upload1请求， token:" + token);
		response.addHeader("Access-Control-Allow-Origin", "*");
		Result result = new Result();
        if (StringUtils.isBlank(token)) {
        	result.setCode(ResultCode.PARAM_ERROR.code);
        	result.setDesc(ResultCode.PARAM_ERROR.returnDesc);
        	return result;
		}
        UserInfo userInfo = SessionContainer.getUserInfoFromRedis(token);
        if (userInfo == null) {
        	result.setCode(ResultCode.NEED_LOGIN.code);
        	result.setDesc(ResultCode.NEED_LOGIN.returnDesc);
        	return result;
		}
        Long playerId = userInfo.getPlayerId();
		try {
			File dir=new File(request.getSession().getServletContext().getRealPath("/uploadfiles/" + playerId));
			if(!dir.exists()){
			    dir.mkdirs();
			}
		} catch (Exception e) {
			result.setCode(1);
			result.setDesc("系统异常");
			e.printStackTrace();
		}
		log.info("upload1返回， result:" + JsonUtil.toJson(result));
        return result;
    }
	
}
