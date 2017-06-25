package cn.worldwalker.game.jinhua.web.test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import cn.worldwalker.game.jinhua.common.utils.IPUtil;
import cn.worldwalker.game.jinhua.domain.Test;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.service.session.SessionContainer;
import cn.worldwalker.game.jinhua.service.test.TestService;

@Controller
@RequestMapping("test/")
public class TestController {
	
	@Autowired
	private TestService testService;
	
	@RequestMapping("testIndex")
	public ModelAndView testIndex(){
		ModelAndView mv = new ModelAndView();
		mv.setViewName("test/testIndex");
		testService.myTest(new Test());
		return mv;
	}
	
	@RequestMapping("getSessionMapSize")
	@ResponseBody
	public Result getSessionMapSize(HttpServletRequest request,HttpServletResponse response){
		Result result = new Result();
		Test test = new Test();
		test.setMerchantId(123);
//		List<Test> list = testService.myTest(test);
		result.setData(SessionContainer.getSessionMap().size());
		return result;
	}
	
	@RequestMapping("getLocalIp")
	@ResponseBody
	public Result getLocalIp(HttpServletRequest request,HttpServletResponse response){
		Result result = new Result();
		Test test = new Test();
		test.setMerchantId(123);
//		List<Test> list = testService.myTest(test);
		result.setData(IPUtil.getLocalIp());
		return result;
	}
	
	
	@RequestMapping("sendMsgToPlayerById")
	@ResponseBody
	public Result sendMsgToPlayerById(HttpServletRequest request,HttpServletResponse response, Long playerId, String msg){
		Result result = new Result();
		result.setData(msg);
		SessionContainer.sendTextMsgByPlayerId(playerId, result);
		return result;
	}
	
}
