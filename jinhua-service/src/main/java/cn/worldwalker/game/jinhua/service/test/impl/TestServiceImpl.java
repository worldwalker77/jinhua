package cn.worldwalker.game.jinhua.service.test.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.worldwalker.game.jinhua.dao.user.UserDao;
import cn.worldwalker.game.jinhua.domain.Test;
import cn.worldwalker.game.jinhua.domain.model.UserModel;
import cn.worldwalker.game.jinhua.service.test.TestService;
@Service
public class TestServiceImpl implements TestService{
	
	private static final Log log = LogFactory.getLog(TestServiceImpl.class);
	
	@Autowired
	private UserDao userDao;
	
	@Transactional  //如果需要用到数据库事务
	public List<Test> myTest(Test test) {
		
		List<Test> temp = new ArrayList<Test>();
		Long playerId = null;
		UserModel userModel = userDao.getUserByWxOpenId("orPvp1PzB-9ZSAr0CYz5aXZusx3c");
		if (null == userModel) {
			userModel = new UserModel();
			userModel.setNickName("nickName");
			userModel.setHeadImgUrl("url");
			userModel.setWxOpenId("orPvp1PzB-9ZSAr0CYz5aXZusx3c");
			userDao.insertUser(userModel);
			playerId = userModel.getId();
		}
		return temp;
	}

}
